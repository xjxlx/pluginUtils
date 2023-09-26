package com.plugin.utils;

import static com.plugin.utils.SystemUtil.println;

import com.plugin.utils.bean.ModuleType;

import org.gradle.api.Project;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GradleUtil {

    private final FileUtil mFileUtil = new FileUtil();
    private final RandomAccessFileUtil mRandomAccessFileUtil = new RandomAccessFileUtil();
    private File mRootDir = null;
    private File mSettingsGradle = null;
    private File mLocalLibs = null;
    private boolean mGradleAnnotate = false;
    private final List<ModuleType> mListModel = new ArrayList<>();

    private static final String SUPPRESS = "@Suppress(\"DSL_SCOPE_VIOLATION\")";
    private static final String PLUGINS = "plugins";
    private static final String IMPLEMENTATION = "implementation";
    private static final String ID = "id";
    private static final String VERSION = "version";

    private final ArrayList<String> mListLibs = new ArrayList<>();
    private final ArrayList<String> mListPlugins = new ArrayList<>();
    private final ArrayList<String> mListContent = new ArrayList<>();

    public void initGradle(File rootDir) {
        println("gradle init !");
        this.mRootDir = rootDir;
    }

    public void initGradle(Project project) {
        println("gradle init !");
        this.mRootDir = project.getRootDir();
        float gradleVersion = Float.parseFloat(project.getGradle().getGradleVersion());
        if (gradleVersion < 8.0f) {
            // 在低于8.0的时候，需要再gradle文件内写入注解 @Suppress("DSL_SCOPE_VIOLATION")
            mGradleAnnotate = true;
        }
    }

    /**
     * @param url       服务器上libs文件的地址
     * @param localLibs 写入项目中文件的地址，一般是在项目中.gradle文件下面，这里交给用户去自己定义
     */
    public void writeGradleToLocal(String url, File localLibs) {
        this.mLocalLibs = localLibs;
        //读取线上的html文件地址
        FileOutputStream outputStream = null;
        try {
            Document doc = Jsoup.connect(url).get();
            Element body = doc.body();
            outputStream = new FileOutputStream(localLibs);
            for (Element allElement : body.getAllElements()) {
                String data = allElement.data();
                if (!data.isEmpty()) {
                    if (data.startsWith("{") && data.endsWith("}")) {
                        JSONObject jsonObject = new JSONObject(data);
                        JSONObject jsonPayload = jsonObject.getJSONObject("payload");

                        JSONObject jsonBlob = jsonPayload.getJSONObject("blob");
                        JSONArray rawLines = jsonBlob.getJSONArray("rawLines");
                        for (Object next : rawLines) {
                            String content = String.valueOf(next);
                            outputStream.write(content.getBytes());
                            outputStream.write("\r\n".getBytes());
                        }
                        println("gradle-file write success!");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            println("gradle-file write failed: " + e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void changeModules() {
        // 设置默认的本地文件libs地址
        if (mLocalLibs == null) {
            mLocalLibs = new File(mRootDir, "gradle/libs.versions.toml");
        }

        // 1：读取本地libs.version.toml文件信息
        readLibsVersions();

        // 2：获取project的目录信息，保活settings、module、library...
        File[] rootFiles = mRootDir.listFiles();
        if (rootFiles != null && rootFiles.length > 0) {
            mSettingsGradle = mFileUtil.filterStart(rootFiles, "settings.gradle");
            println("settings: " + mSettingsGradle.getAbsolutePath());

            // 读取settings文件的内容
            List<String> settingContent = mFileUtil.readFile(mSettingsGradle);
            // 过滤引入include的信息，就是model的名字
            List<String> listInclude = mFileUtil.filterStart(settingContent, "include");
            mListModel.clear();

            // 获取module的名字，然后批量进行gradle文件修改
            for (String name : filterModel(listInclude)) {
                File moduleFile = mFileUtil.filterStart(rootFiles, name);
                // module文件存在
                if (moduleFile != null && moduleFile.exists()) {
                    File[] moduleFiles = moduleFile.listFiles();
                    if (moduleFiles != null && moduleFiles.length > 0) {
                        // 获取module下的build.gradle文件
                        File buildGradle = mFileUtil.filterStart(moduleFiles, "build.gradle");
                        if (buildGradle != null && buildGradle.exists()) {
                            println("当前的module:" + name);
                            ModuleType moduleType = new ModuleType(buildGradle, buildGradle.getName().endsWith(".kts"));
                            mListModel.add(moduleType);
                            changeGradleFile(moduleType.getModel());
                        }
                    }
                }
            }
        }
        //  changeGradleFile(new File("/Users/XJX/AndroidStudioProjects/plugins/pluginUtil/src/main/java/com/plugin/utils/Test2.txt"));
    }

    /**
     * @param includeList settings中include的内容
     * @return 返回module名字的集合
     */
    private List<String> filterModel(List<String> includeList) {
        ArrayList<String> modelName = new ArrayList<>();
        if (includeList != null) {
            for (int i = 0; i < includeList.size(); i++) {
                String include = includeList.get(i);

                // split
                String[] split = include.split(":");
                for (String s : split) {
                    String splitContent = s;
                    if (splitContent.contains("(")) {
                        continue;
                    }
                    if (splitContent.contains(")")) {
                        splitContent = splitContent.replace("\")", "");
                    }
                    modelName.add(splitContent.trim());
                }
            }
        }
        return modelName;
    }


    /**
     * 读取云端libs.versions.toml文件信息
     */
    private void readLibsVersions() {
        if ((mLocalLibs == null) || (!mLocalLibs.exists())) {
            println("本地的libs.version.toml文件不存在！");
            return;
        }
        if (mLocalLibs.length() == 0) {
            println("本地的libs.version.toml内容为空！");
            return;
        }

        try (RandomAccessFile rafGradle = new RandomAccessFile(mLocalLibs, "r")) {
            boolean libsFlag = false;
            boolean pluginsFlag = false;
            mListLibs.clear();
            mListPlugins.clear();

            while (rafGradle.getFilePointer() < rafGradle.length()) {
                String readLine = rafGradle.readLine();
                if (!TextUtils.isEmpty(readLine)) {
                    String versionContent = new String(readLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

                    if (libsFlag) {
                        if (versionContent.startsWith("[plugins]")) {
                            libsFlag = false;
                        }
                    }
                    // write libs
                    if (libsFlag) {
                        mListLibs.add(versionContent);
                    }
                    if (versionContent.startsWith("[libraries]")) {
                        libsFlag = true;
                    }

                    // write plugins
                    if (pluginsFlag) {
                        if (versionContent.startsWith("[bundles]")) {
                            pluginsFlag = false;
                        }
                    }
                    if (pluginsFlag) {
                        mListPlugins.add(versionContent);
                    }
                    if (versionContent.startsWith("[plugins]")) {
                        pluginsFlag = true;
                    }
                }
            }
            println("读取library成功 :" + mListLibs);
            println("读取plugins成功 :" + mListPlugins);
        } catch (Exception exception) {
            println("读取library失败 :" + exception.getMessage());
        }
    }

    /**
     * 读取gradle文件信息，进行匹配修改
     *
     * @param gradle 对应的gradle文件
     */
    private void changeGradleFile(File gradle) {
        // 读取所有的字符串存入集合
        mListContent.clear();
        String gradlePath = gradle.getAbsolutePath();
        // 1：读取gradle中的文件
        mListContent.addAll(mRandomAccessFileUtil.readFile(gradlePath, "r"));

        try (RandomAccessFile raf = new RandomAccessFile(gradlePath, "rw")) {
            // 2:添加注解头
            if (!mListContent.contains(SUPPRESS)) {
                mListContent.add(0, SUPPRESS);
            }

            for (int i = 0; i < mListContent.size(); i++) {
                String content = mListContent.get(i);
                String trim = content.trim();
                if (trim.startsWith(ID)) {
                    // 3:替换 plugins
                    println("id: " + content);
                    String plugins = replaceModulePlugins(content);
                    mListContent.set(i, plugins);

                } else if (trim.startsWith(IMPLEMENTATION)) {
                    // 4:替换 implementation
                    println(IMPLEMENTATION + ":" + content);
                    String implementation = replaceModuleDependencies(content);
                    mListContent.set(i, implementation);
                }
            }

            // loop write data
            for (String item : mListContent) {
                raf.write(item.getBytes());
                raf.write("\r\n".getBytes());
            }
        } catch (Exception exception) {
            println("gradle 信息写入失败: " + exception.getMessage());
        }
    }

    /**
     * 替换dependencies具体的值
     *
     * @param data 原始的数据
     */
    private String replaceModuleDependencies(String data) {
        String result = data;
        try {
            if (data.contains(":")) {
                String type = "";
                boolean flag = false;
                String group = "";
                String name = "";

                String realLeft = "";
                String realMiddle = "";
                String realRight = "";

                //    implementation("org.json:json:20230227")// json 依赖库 " :abc
                //    implementation("org.jsoup:jsoup:1.16.1") // html依赖库
                //    implementation("org.jetbrains.kotlin:kotlin-reflect")

                // 1:确定是用什么进行分割的
                String[] splitImplementation = data.split(IMPLEMENTATION);
                realLeft = splitImplementation[0] + IMPLEMENTATION;
                String implementationContent = splitImplementation[1].trim();

                // 获取右侧去除括号的数据
                String allRight = "";
                if (implementationContent.startsWith("(")) {
                    allRight = implementationContent.substring(1);
                    flag = true;
                } else {
                    allRight = implementationContent;
                }
                String allRightTrim = allRight.trim();
                if (allRightTrim.startsWith("'")) {
                    type = "'";
                } else if (allRightTrim.startsWith("\"")) {
                    type = "\"";
                }

                // 开始分割
                String[] splitType = implementationContent.split(type);
                String tempMiddle = splitType[1];
                String[] splitVersion = tempMiddle.split(":");
                group = splitVersion[0];
                name = splitVersion[1];

                // 这里中间长度加2的原因是因为tempMiddle是被type分割出来的，分割的时候，两边的type都会被清除掉，
                // 所以这个要加上分割的字符串长度
                realRight = implementationContent.substring(tempMiddle.length() + type.length() * 2);

                String versions = "";
                for (int i = 0; i < mListLibs.size(); i++) {
                    String line = mListLibs.get(i);
                    if (line.contains(group) && line.contains(name)) {
                        versions = line;
                        break;
                    }
                }

                if (!versions.equals("")) {
                    println("1：找到了对应的implementation属性：" + versions);
                    // 取出libs.version.name
                    String libsName = versions.split("=")[0].trim();
                    if (libsName.contains("-")) {
                        libsName = libsName.replace("-", ".");
                    }
                    if (flag) {
                        realMiddle = "libs." + libsName;
                    } else {
                        realMiddle = "(libs." + libsName + ")";
                    }
                    result = realLeft + realMiddle + realRight;
                    println("2: result:[" + result + "]");
                } else {
                    println("1：找不到对应的implementation属性：" + group + "-" + name);
                }
            }
        } catch (Exception exception) {
            println("写入implementation属性失败：" + exception.getMessage());
        }
        return result;
    }

    /**
     * 替换module的plugins内容
     *
     * @param reads plugins的内容
     */
    private String replaceModulePlugins(String reads) {
        String result = reads;
        try {
            String type = "";// 分隔符，要么是"要么是'
            boolean flag = false;
            String realLeft = "";
            String realMiddle = "";
            String realRight = "";

            // 先确认是用什么进行分割的，比如：' 或者 "
            String[] splitID = reads.split(ID);
            String pluginsContent = splitID[1];
            // 去除所有的空格
            if (pluginsContent.contains(" ")) {
                pluginsContent = pluginsContent.replace(" ", "");
            }
            if (pluginsContent.startsWith("(")) {
                flag = true;
                pluginsContent = pluginsContent.replace("(", "");
            }
            if (pluginsContent.startsWith("'")) {
                type = "'";
            } else if (pluginsContent.startsWith("\"")) {
                type = "\"";
            }

            // 使用指定的类型去分割字符串
            String[] split = reads.split(type);
            String tempLeft = split[0];
            String tempContent = split[1];
            String allLeft = tempLeft + type + tempContent;
            realRight = reads.replace(allLeft, "");

            // check version
            if (realRight.contains(VERSION)) {
                String versionInfo = "";
                if (flag) {
                    String[] splitVersion = realRight.split("\\)");
                    versionInfo = splitVersion[1];
                } else {
                    versionInfo = realRight;
                }
                String replaceRight = versionInfo.replace("'", "\"");
                String[] splitRightVersion = replaceRight.split("\"");
                String version = splitRightVersion[0];
                String versionCode = splitRightVersion[1];

                realRight = realRight.replace(version, "");
                realRight = realRight.replace("\"" + versionCode + "\"", "");
            }

            // 查找plugins属性
            boolean pluginLineFlag = false;
            String pluginsTemp = tempContent.replace(".", "-");
            for (int i = 0; i < mListPlugins.size(); i++) {
                String pluginSplit = mListPlugins.get(i).split("=")[0].trim();
                if (pluginsTemp.equals(pluginSplit)) {
                    pluginLineFlag = true;
                    break;
                }
            }

            if (pluginLineFlag) {
                println("找到plugin属性：" + tempContent);
                if (tempContent.contains("-")) {
                    tempContent = tempContent.replace("-", ".");
                }

                if (!flag) {
                    realMiddle = "(libs.plugins." + tempContent + ")";
                } else {
                    realMiddle = "libs.plugins." + tempContent;
                }

                realLeft = tempLeft.replace(ID, "alias");
                result = realLeft + realMiddle + realRight;
            } else {
                println("找不到plugin属性：" + tempContent);
            }
        } catch (Exception e) {
            println("module:plugins: [" + "" + "] write failed!");
        }
        return result;
    }
}
