package com.plugin.utils;

public class TextUtils {
    public static boolean isEmpty(String content) {
        if (content == null) {
            return true;
        } else {
            return content.isEmpty();
        }
    }
}
