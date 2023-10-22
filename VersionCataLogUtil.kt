package utils

import common.ConfigCatalog
import org.gradle.api.Project
import java.io.FileOutputStream

class VersionCataLogUtil {

    private val mListContent = arrayListOf<String>()
    private val pluginManagement = "pluginManagement"
    private val dependencyResolutionManagement = "dependencyResolutionManagement"
    private val repositoriesMode = "repositoriesMode"
    private val repositories = "repositories"

    private val mavenPublicTag = "https://maven.aliyun.com/repository/public"
    private val mavenPublicReleaseTag = "https://packages.aliyun.com/maven/repository/2131155-release-wH01IT/"
    private val mavenPublicSnapshotTag = "https://packages.aliyun.com/maven/repository/2131155-snapshot-mh62BC/"
    private val mavenCatalog = "versionCatalogs"

    fun write(project: Project) {
        val settingsFile = project.rootDir.listFiles()
            ?.find { it.isFile && it.name.contains("settings") }
        settingsFile?.let {
            FileUtil.readFile(settingsFile)
                ?.let { settingsList ->
                    var dependencyResolutionManagementStartFlag = false
                    var repositoriesStartFlag = false

                    var mavenPublicTagFlag = false
                    var mavenPublicReleaseTagFlag = false
                    var mavenPublicSnapshotTagFlag = false
                    var catalogFlag = false

                    var count = 0
                    var tempIndex = 0
                    var addCount = 0
                    var addCountFlag = false

                    settingsList.forEachIndexed { index, item ->
                        val trim = item.trim()

                        // 避免错误检测
                        if (trim.startsWith(pluginManagement)) {
                            dependencyResolutionManagementStartFlag = false
                            repositoriesStartFlag = false
                        }

                        // 检测到了dependencyResolutionManagement标签
                        if (trim.startsWith(dependencyResolutionManagement)) {
                            dependencyResolutionManagementStartFlag = true
                        }
                        // 检测到了repositories标签
                        if (dependencyResolutionManagementStartFlag) {
                            if (trim.startsWith(repositories) && !trim.startsWith(repositoriesMode)) {
                                repositoriesStartFlag = true
                            }
                        }

                        if (repositoriesStartFlag) {
                            if (!addCountFlag) {
                                // 开始计算
                                if (trim.contains("{") && trim.contains("}")) {
                                    count = 0
                                } else if (trim.contains("{")) {
                                    count -= 1
                                } else if (trim.contains("}")) {
                                    count += 1
                                }
                                if (count >= 1) {
                                    println("count:$count index:$index")
                                    tempIndex = index
                                    addCountFlag = true
                                }
                            }

                            // 检测中央公共仓库
                            if (!mavenPublicTagFlag) {
                                mavenPublicTagFlag = trim.contains(mavenPublicTag)
                            }
                            // 检测用户信息-release
                            if (!mavenPublicReleaseTagFlag) {
                                mavenPublicReleaseTagFlag = trim.contains(mavenPublicReleaseTag)
                            }
                            // 检测用户信息-Snapshot
                            if (!mavenPublicSnapshotTagFlag) {
                                mavenPublicSnapshotTagFlag = trim.contains(mavenPublicSnapshotTag)
                            }
                            // 检测catalog
                            if (!catalogFlag) {
                                catalogFlag = trim.contains(mavenCatalog)
                            }
                        }
                        mListContent.add(item)
                        // println("item:$item count:$count")
                    }

                    println("[mavenPublic]:$mavenPublicTagFlag")
                    println("[mavenPublicRelease]:$mavenPublicReleaseTagFlag")
                    println("[mavenPublicSnapshot]:$mavenPublicSnapshotTagFlag")
                    println("[catalog]:$catalogFlag")

                    // 添加阿里云：用户信息 - snapshot
                    if (!mavenPublicSnapshotTagFlag) {
                        mListContent.add(tempIndex, ConfigCatalog.MAVEN_SNAPSHOT)
                        addCount += 1
                    }
                    // 添加阿里云：用户信息 - release
                    if (!mavenPublicReleaseTagFlag) {
                        mListContent.add(tempIndex, ConfigCatalog.MAVEN_RELEASE)
                        addCount += 1
                    }
                    // 添加中央控制仓库
                    if (!mavenPublicTagFlag) {
                        mListContent.add(tempIndex, ConfigCatalog.MAVEN_PUBLIC)
                        addCount += 1
                    }

                    // 3:添加catalog
                    if (!catalogFlag) {
                        mListContent.add(tempIndex + addCount + 1, ConfigCatalog.MAVEN_CATALOG)
                    }

                    // print content
                    mListContent.forEach {
                        println(it)
                    }

                    // write data
                    if (settingsList.size != mListContent.size) {
                        FileOutputStream(settingsFile).use {
                            mListContent.forEach { item ->
                                it.write(item.toByteArray())
                                it.write("\r\n".toByteArray())
                            }
                        }
                    }
                }
        }
    }
}