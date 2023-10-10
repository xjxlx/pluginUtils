plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm") // 用kotlin语言来开发
    id("com.gradle.plugin-publish") version "1.0.0-rc-1" // 这个是发布到插件门户网站的插件
}

dependencies {
    implementation("com.android.tools.build:gradle-api:7.4.2")
    implementation(gradleApi()) // gradle sdk

    implementation("org.json:json:20230227")// json 依赖库
    implementation("org.jsoup:jsoup:1.16.1") // html依赖库
}

