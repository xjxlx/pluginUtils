package utils

object Config {

    object Plugin {
        const val plugin_group = "io.github.xjxlx"

        const val plugin_common = "common"
        const val plugin_publish = "publish"
        const val plugin_version = "version"

        const val plugin_common_code = "1.0.0"
        const val plugin_publish_code = "1.0.0"
        const val plugin_version_code = "1.0.4"
    }

    object Project {
        const val PROJECT_GROUP = "com.github.jitpack"
        const val PROJECT_VERSION = "1.0"

        const val PUBLISH_PLUGIN_ID = "maven-publish"
        const val PUBLISH_TYPE = "release"
    }

    object URL {
        const val URL_VERSION_PATH =
            "https://github.com/xjxlx/plugins/blob/39a705f313bec743e2c0437ce0f61a64a63c60f2/gradle/libs.versions.toml"
    }
}