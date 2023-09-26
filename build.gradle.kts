plugins {
    id("java-gradle-plugin")
    id("java-library")
    `version-catalog`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.0.0-rc-1" // 这个是发布到插件门户网站的插件
    id("io.github.xjxlx.common") version "1.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
//    implementation("com.android.tools.build:gradle-api:7.4.0") // abds : sss
    implementation(gradleApi()) // gradle sdk

    implementation("org.json:json:20230227")// json 依赖库 " :abc
    implementation("org.jsoup:jsoup:1.16.1") // html依赖库
//    implementation("org.jetbrains.kotlin:kotlin-reflect") // :sd ";ll:
}
