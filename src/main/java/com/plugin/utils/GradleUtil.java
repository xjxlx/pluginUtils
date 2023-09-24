package com.plugin.utils;

import static com.plugin.utils.SystemUtil.println;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class GradleUtil {

    private static final String IMPLEMENTATION = "implementation";
    private static final String version_catalog_path = "/Users/XJX/AndroidStudioProjects/plugins/gradle/libs.versions.toml";
    private final ArrayList<String> mListLibs = new ArrayList<>();

    /**
     * 读取云端libs.versions.toml文件信息
     */
    public void readLibsVersions() {
        try {
            RandomAccessFile rafGradle = new RandomAccessFile(version_catalog_path, "r");
            mListLibs.clear();
            boolean startFlag = false;
            while (rafGradle.length() != rafGradle.getFilePointer()) {
                String versionContent = new String(rafGradle.readLine().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
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
            println("读取library成功 :" + mListLibs);
        } catch (Exception exception) {
            println("读取library失败 :" + exception.getMessage());
        }
    }

    /**
     * 读取gradle文件信息，进行匹配修改
     *
     * @param gradle 对应的gradle文件
     */
    public void changeGradleFile(File gradle) {
        try {
            String gradlePath = gradle.getAbsolutePath();
            String modelName = "";
            if (gradlePath.contains("/")) {
                String[] gradlePathSplit = gradlePath.split("/");
                int gradlePathLength = gradlePathSplit.length;
                int index = gradlePathLength - 2;
                if (index > 0) {
                    modelName = gradlePathSplit[index];
                }
            }
            println("当前的Model:" + modelName);

            // RandomAccessFile默认使用的是【iso-8859-1】字符集，使用的时候，需要把他转换成UTF-8
            RandomAccessFile raf = new RandomAccessFile(gradlePath, "rw");
            long tempLength = 0;
            while (tempLength < raf.length()) {
                long filePointer = raf.getFilePointer();
                String readLine = raf.readLine();
                String reads = "";
                if (!TextUtils.isEmpty(readLine)) {
                    reads = new String(readLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    if (reads.contains(IMPLEMENTATION)) {
                        String trim = reads.trim();
                        // check starts
                        if ((trim.startsWith(IMPLEMENTATION)) && (!trim.startsWith("//"))) {
                            println("implementation : " + reads);
                            // replace '
                            if (reads.contains("'")) {
                                reads = reads.replace("'", "\"");
                            }
                            // change data
                            replaceDependencies(raf, reads, filePointer);
                        }
                    }
                }
                println("" + reads);
                tempLength = filePointer;
            }
        } catch (Exception exception) {
            println("gradle 信息写入失败: " + exception.getMessage());
        }
    }

    /**
     * 替换dependencies具体的值
     *
     * @param raf         操作文件读写流的对象
     * @param data        原始的数据
     * @param filePointer 开始替换的角标
     */
    private void replaceDependencies(RandomAccessFile raf, String data, long filePointer) {
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
}
