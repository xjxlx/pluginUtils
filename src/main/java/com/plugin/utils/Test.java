package com.plugin.utils;

import java.io.File;

public class Test {
    private static final GradleUtil gradleUtil = new GradleUtil();
    private static String urlPath = "https://github.com/xjxlx/plugins/blob/39a705f313bec743e2c0437ce0f61a64a63c60f2/gradle/libs.versions.toml";


    public static void main(String[] args) {
        //        /Users/XJX/AndroidStudioProjects/plugins/app/build.gradle.kts
//        String root = "/Users/XJX/AndroidStudioProjects/plugins/pluginUtil/src/main/java/com/plugin/utils/test.txt";
        String root = "/Users/XJX/AndroidStudioProjects/plugins";

        File rootFile = new File(root);
        gradleUtil.initGradle(rootFile);
        gradleUtil.writeGradleToLocal(urlPath, new File(rootFile, "gradle" + File.separator + "libs2.versions.toml"));
        // gradleUtil.changeModules();

        gradleUtil.changeRootGradle();
    }
}
