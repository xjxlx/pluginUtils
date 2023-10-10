plugins {
    id("org.jetbrains.kotlin.jvm") // 用kotlin语言来开发
    id("maven-publish")
}

val publishGroup = "io.github.xjxlx"
val publishVersion = "1.0.0.0"
val publishArtifactId = "pluginUtils"

dependencies {
    api("com.android.tools.build:gradle-api:7.4.2")
    api(gradleApi()) // gradle sdk

    api("org.json:json:20230227")// json 依赖库
    api("org.jsoup:jsoup:1.16.1") // html依赖库
}

// 配置阿里云私有信息
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("plugin") {// 注册一个名字为 release 的发布内容
                from(components["java"])
                groupId = publishGroup
                artifactId = publishArtifactId
                version = publishVersion
            }
        }
        repositories {
            maven {
                setUrl("https://packages.aliyun.com/maven/repository/2131155-release-wH01IT/")
                credentials {
                    username = "6123a7974e5db15d52e7a9d8"
                    password = "HsDc[dqcDfda"
                }
            }
            maven {
                setUrl("https://packages.aliyun.com/maven/repository/2131155-snapshot-mh62BC/")
                credentials {
                    username = "6123a7974e5db15d52e7a9d8"
                    password = "HsDc[dqcDfda"
                }
            }
        }
    }
}


