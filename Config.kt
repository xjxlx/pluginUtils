package utils

object Config {

    object Plugin {
        const val GROUP = "io.github.xjxlx"

        const val COMMON = "common"
        const val PUBLISH = "publish"
        const val CATALOG = "catalog"

        const val COMMON_CODE = "1.0.0"
        const val PUBLISH_CODE = "1.0.0"
        const val CATALOG_CODE = "1.0.0"
    }

    object Project {
        const val JITPACK = "com.github.jitpack"
        const val GROUP = "io.github.xjxlx"
        const val VERSION = "1.0"

        const val PUBLISH_PLUGIN_ID = "maven-publish"
        const val PUBLISH_TYPE = "release"
    }

    object URL {
        const val VERSION_PATH = "https://github.com/xjxlx/plugins/blob/39a705f313bec743e2c0437ce0f61a64a63c60f2/gradle/libs.versions.toml"
    }
}