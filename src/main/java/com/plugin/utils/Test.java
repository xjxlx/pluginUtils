package com.plugin.utils;

import java.io.File;

public class Test {
    private static final RandomAccessFileUtil util = new RandomAccessFileUtil();


    public static void main(String[] args) {
        //        /Users/XJX/AndroidStudioProjects/plugins/app/build.gradle.kts
        String root = "/Users/XJX/AndroidStudioProjects/plugins/pluginUtil/src/main/java/com/plugin/utils/test.txt";
        util.change(root, "rw");
    }
}
