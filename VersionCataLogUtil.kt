package utils

import common.ConfigCatalog
import org.gradle.api.Project
import java.io.FileOutputStream

class VersionCataLogUtil {

    private var mSettingList = mutableListOf<String>()

    private val dependencyResolutionManagement = "dependencyResolutionManagement"
    private val repositoriesMode = "repositoriesMode"
    private val repositories = "repositories"

    private val mavenPublicTag = "https://maven.aliyun.com/repository/public"
    private val mavenPublicReleaseTag =
        "https://packages.aliyun.com/maven/repository/2131155-release-wH01IT/"
    private val mavenPublicSnapshotTag =
        "https://packages.aliyun.com/maven/repository/2131155-snapshot-mh62BC/"
    private val mavenCatalog = "versionCatalogs"

    fun write(project: Project) {
        val settingsFile = project.rootDir.listFiles()
            ?.find { it.isFile && it.name.contains("settings") }

        var dependencyResolutionManagementFlag = false
        var repositoriesFlag = false

        settingsFile?.let {
            FileUtil.readFile(settingsFile)
                ?.let { settingsList ->
                    val mavenPublicTagFlag = settingsList.any { it.contains(mavenPublicTag) }
                    val mavenPublicReleaseTagFlag =
                        settingsList.any { it.contains(mavenPublicReleaseTag) }
                    val mavenPublicSnapshotTagFlag =
                        settingsList.any { it.contains(mavenPublicSnapshotTag) }
                    var catalogFlag =
                        settingsList.any { it.contains(mavenCatalog) }

                    println("[mavenPublic]:$mavenPublicTagFlag")
                    println("[mavenPublicRelease]:$mavenPublicReleaseTagFlag")
                    println("[mavenPublicSnapshot]:$mavenPublicSnapshotTagFlag")
                    println("[catalog]:$catalogFlag")

                    settingsList.forEach { item ->
                        val trim = item.trim()
                        // 在添加原始数据之前添加仓库地址
                        println("item:$item")
                        if (repositoriesFlag) {
                            if (trim == "}") {
                                // 1:添加中央控制仓库
                                if (!mavenPublicTagFlag) {
                                    mSettingList.add(ConfigCatalog.MAVEN_PUBLIC)
                                }

                                // 2：添加阿里云：用户信息 - release
                                if (!mavenPublicReleaseTagFlag) {
                                    mSettingList.add(ConfigCatalog.MAVEN_RELEASE)
                                }

                                // 2：添加阿里云：用户信息 - snapshot
                                if (!mavenPublicSnapshotTagFlag) {
                                    mSettingList.add(ConfigCatalog.MAVEN_SNAPSHOT)
                                }
                            }
                        }
                        mSettingList.add(item)

                        // 3:添加catalog
                        if (repositoriesFlag && !catalogFlag) {
                            mSettingList.add(ConfigCatalog.MAVEN_CATALOG)
                            catalogFlag = true
                        }

                        // 检测到了dependencyResolutionManagement标签
                        if (trim.startsWith(dependencyResolutionManagement)) {
                            dependencyResolutionManagementFlag = true
                        }

                        // 检测到了repositories标签
                        if (dependencyResolutionManagementFlag) {
                            if (trim.startsWith(repositories) && !trim.startsWith(repositoriesMode)) {
                                repositoriesFlag = true
                            }
                        }
                    }
                }

//            mSettingList.forEach {
//                println("item --->: $it")
//            }

            // write data
            FileOutputStream(settingsFile).use {
                mSettingList.forEach { item ->
                    it.write(item.toByteArray())
                    it.write("\r\n".toByteArray())
                }
            }
        }
    }
}