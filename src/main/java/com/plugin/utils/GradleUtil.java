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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GradleUtil {

    private final FileUtil mFileUtil = new FileUtil();
    private File mRootDir = null;
    private File mSettingsGradle = null;
    private File mLocalLibs = null;
    private boolean mGradleAnnotate = false;
    private final List<ModuleType> mListModel = new ArrayList<>();

    private static final String IMPLEMENTATION = "implementation";
    private static final String PLUGINS = "plugins";

    private final ArrayList<String> mListLibs = new ArrayList<>();

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

                            if (content.startsWith("#") || content.startsWith("[")) {
                                content = "\r\n" + content + "\r\n";
                            }

                            outputStream.write(content.getBytes());
                            if (content.endsWith("\"") || content.endsWith("]") || content.endsWith("}")) {
                                outputStream.write("\r\n".getBytes());
                            }
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
        RandomAccessFile rafGradle = null;
        try {
            if ((mLocalLibs == null) || (!mLocalLibs.exists())) {
                println("本地的libs.version.toml文件不存在！");
                return;
            }
            if (mLocalLibs.length() == 0) {
                println("本地的libs.version.toml内容为空！");
                return;
            }

            rafGradle = new RandomAccessFile(mLocalLibs, "r");
            boolean startFlag = false;
            mListLibs.clear();
            while (rafGradle.getFilePointer() != rafGradle.length()) {
                String readLine = rafGradle.readLine();
                if (!TextUtils.isEmpty(readLine)) {
                    String versionContent = new String(readLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    if (startFlag) {
                        mListLibs.add(versionContent);
                    }
                    if (versionContent.startsWith("[libraries]")) {
                        startFlag = true;
                    }
                    if (startFlag) {
                        if (versionContent.startsWith("[plugins]")) {
                            startFlag = false;
                        }
                    }
                }
            }
            println("读取library成功 :" + mListLibs);
        } catch (Exception exception) {
            println("读取library失败 :" + exception.getMessage());
        } finally {
            if (rafGradle != null) {
                try {
                    rafGradle.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 读取gradle文件信息，进行匹配修改
     *
     * @param gradle 对应的gradle文件
     */
    private void changeGradleFile(File gradle) {
        RandomAccessFile raf = null;
        boolean pluginsFlag = false;

        try {
            String gradlePath = gradle.getAbsolutePath();
            // RandomAccessFile默认使用的是【iso-8859-1】字符集，使用的时候，需要把他转换成UTF-8
            raf = new RandomAccessFile(gradlePath, "rw");

            // 给文件写入注解
            if (mGradleAnnotate) {
                raf.seek(0);
                raf.writeUTF("@Suppress(\"DSL_SCOPE_VIOLATION\")");
            }

            long tempLength = 0;
            while (tempLength < raf.length()) {
                long filePointer = raf.getFilePointer();
                String readLine = raf.readLine();
                String reads = "";
                long insertPositions = 0;
                if (!TextUtils.isEmpty(readLine)) {
                    reads = new String(readLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

                    // replace plugins
                    if (pluginsFlag) {
                        if (reads.contains("}")) {
                            pluginsFlag = false;
                        }
                    }
                    if (pluginsFlag) {
                        long insertPluginsPosition = replaceModulePlugins(raf, reads, filePointer, insertPositions);
                        insertPositions += insertPluginsPosition;
                    }
                    if (reads.startsWith(PLUGINS)) {
                        pluginsFlag = true;
                    }

                    // replace implementation
                    if (reads.contains(IMPLEMENTATION)) {
                        String trim = reads.trim();
                        // check starts
                        if ((trim.startsWith(IMPLEMENTATION)) && (!trim.startsWith("//"))) {
                            // println("implementation : " + reads);
                            // replace '
                            if (reads.contains("'")) {
                                reads = reads.replace("'", "\"");
                            }
                            // change data
                            //  replaceDependencies(raf, reads, filePointer);
                        }
                    }
                }
                // println("" + reads);
                tempLength = filePointer;
            }
        } catch (Exception exception) {
            println("gradle 信息写入失败: " + exception.getMessage());
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 替换dependencies具体的值
     *
     * @param raf         操作文件读写流的对象
     * @param data        原始的数据
     * @param filePointer 开始替换的角标
     */
    private void replaceModuleDependencies(RandomAccessFile raf, String data, long filePointer) {
        if (data.contains(":")) {
            String group = "";
            String name = "";
            String originalLeft = ""; // implementation("com.android.tools.build
            String originalRight = ""; // gradle-api:7.4.0")

            String realLeft = "";
            String realMiddle = "";
            String realRight = "";
            String result = "";

            String[] split = data.split(":");
            int length = (split.length) - 1;
            // 中间部分可能没有，直接就是 group+ name ,没有version
            originalLeft = split[0];

            // realLeft
            String[] splitLeft = originalLeft.split("\"");
            realLeft = splitLeft[0];
            group = splitLeft[1];

            // get original right
            if (length >= 2) {
                // implementation("com.android.tools.build:gradle-api:7.4.0")
                originalRight = split[2];
            } else {
                // implementation "org.jetbrains.kotlin:kotlin-reflect"
                originalRight = split[1];
            }

            String[] splitOriginRight = originalRight.split("\"");

            // get name
            if (length >= 2) {
                name = split[1];
            } else {
                name = splitOriginRight[0];
            }

            // get real right
            realRight = splitOriginRight[1];

            String versions = "";
            for (int i = 0; i < mListLibs.size(); i++) {
                versions = mListLibs.get(i);
                if (versions.contains(group) && versions.contains(name)) {
                    println("1：找到了对应的属性：" + versions);
                    // 取出libs.version.name
                    realMiddle = "libs." + versions.split("=")[0].replace("-", ".").trim();
                    int originLength = data.length();
                    result = realLeft + realMiddle + realRight;
                    int resultLength = result.length();
                    if (resultLength < originLength) {
                        int intervalLength = originLength - resultLength;
                        for (int j = 0; j < intervalLength; j++) {
                            result += " ";
                        }
                    }
                    println("2: result:[" + result + "]");
                    try {
                        raf.seek(filePointer);
                        raf.write(result.getBytes(StandardCharsets.UTF_8));
                        println("3：写入：result: " + result + " 成功！");
                    } catch (IOException e) {
                        println("3：写入数据异常！");
                    }
                    break;
                }
            }
            if (TextUtils.isEmpty(versions)) {
                println("1：找不到对应的属性：" + group + "-" + name);
            }
        }
    }

    /**
     * 替换module的plugins内容
     *
     * @param raf             随机读写流对象
     * @param reads           plugins的内容
     * @param filePointer     写入的位置
     * @param insertPositions
     */
    private long replaceModulePlugins(RandomAccessFile raf, String reads, long filePointer, long insertPositions) {
        String trim = reads.trim();
        long interval = 0;
        if (trim.startsWith("id")) {
            String realLeft = "";
            String realMiddle = "";
            StringBuilder realRight = new StringBuilder();
            StringBuilder resultBuilder = new StringBuilder();

            if (reads.contains("'")) {
                reads = reads.replace("'", "\"");
            }
            //remove id
            if (reads.contains("\"")) {
                String[] splitId = reads.split("\"");
                String left = splitId[0];
                if (left.contains("(")) {
                    realLeft = left.replace("id", "alias");
                } else {
                    realLeft = left.replace("id", "alias(");
                }

                String tempMiddle = splitId[1];
                if (tempMiddle.contains("-")) {
                    tempMiddle = tempMiddle.replace("-", ".");
                }
                if (!tempMiddle.contains(")")) {
                    tempMiddle += ")";
                }
                realMiddle = "libs.plugins." + tempMiddle;
                int length = splitId.length;
                if (length > 3) {
                    String tempRight = splitId[2];
                    if (tempRight.contains("version")) {
                        String version = tempRight.replace("version", "");
                        realRight.append(version);
                        for (int i = 0; i < length; i++) {
                            if (i <= 3) {
                                continue;
                            }
                            realRight.append(splitId[i]);
                        }
                    } else {
                        for (int i = 0; i < length; i++) {
                            if (i <= 2) {
                                continue;
                            }
                            realRight.append(splitId[i]);
                        }
                    }
                }
            }

            resultBuilder.append(realLeft).append(realMiddle).append(realRight);
            // 补全右侧的占位
            int readsLength = reads.length();
            int resultLength = resultBuilder.length();
            println("readsLength: " + readsLength + " resultLength: " + resultLength);
            interval = readsLength - resultLength;
            if (resultLength < readsLength) {
                for (int i = 0; i < interval; i++) {
                    resultBuilder.append(" ");
                }
            } else if (resultLength > readsLength) {
                resultBuilder.append("\r\n");
            }
            String result = resultBuilder.toString();
            try {
                raf.seek(filePointer);
                raf.write(result.getBytes(StandardCharsets.UTF_8));
                println("module:plugins: [" + result + "] write success!");
            } catch (Exception e) {
                println("module:plugins: [" + result + "] write failed!");
            }
            println("plugins：[ " + result + " ]");
        }
        if (interval < 0) {
            interval = Math.abs(interval);
        }
        return interval;
    }
}
