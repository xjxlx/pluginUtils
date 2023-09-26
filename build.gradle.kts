plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
    // implementation("com.android.tools.build:gradle-api:7.4.0")
    implementation(gradleApi()) // gradle sdk

    implementation("org.json:json:20230227")// json 依赖库
    implementation("org.jsoup:jsoup:1.16.1") // html依赖库
}
