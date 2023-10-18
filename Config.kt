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
        const val JITPACK_VERSION = "1.0"

        const val PUBLISH_PLUGIN_ID = "maven-publish"
        const val PUBLISH_TYPE = "release"
    }

    object URL {
        const val VERSION_PATH = "https://github.com/xjxlx/plugins/blob/masater/gradle/libs.versions.toml"
    }
}