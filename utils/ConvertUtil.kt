package com.android.helper.utils

import org.gradle.api.provider.Provider

object ConvertUtil {
    fun convertVersion(version: Provider<String>): Int {
        return version.get()
            .toInt()
    }
}