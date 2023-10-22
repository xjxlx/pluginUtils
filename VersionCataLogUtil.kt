package utils

import common.ConfigCatalog
import org.gradle.api.Project
import java.io.FileOutputStream

class VersionCataLogUtil {

    private var mSettingList = mutableListOf<String>()

    private val dependencyResolutionManagement = "dependencyResolutionManagement"
    private var dependencyResolutionManagementFlag = false

    private val repositoriesMode = "repositoriesMode"
    private val repositories = "repositories"
    private var repositoriesFlag = false
    private var catalogFlag = false

    private val mavenPublicTag = "https://maven.aliyun.com/repository/public"
    private val mavenPublicReleaseTag =
        "https://packages.aliyun.com/maven/repository/2131155-release-wH01IT/"
    private val mavenPublicSnapshotTag =
        "https://packages.aliyun.com/maven/repository/2131155-snapshot-mh62BC/"

    fun write(project: Project) {
        val settingsFile = project.rootDir.listFiles()
            ?.find { it.isFile && it.name.contains("settings") }

        settingsFile?.let {
            FileUtil.readFile(settingsFile)
                ?.let { settingsList ->
                    val mavenPublicTagFlag = settingsList.contains(mavenPublicTag)
                    val mavenPublicReleaseTagFlag = settingsList.contains(mavenPublicReleaseTag)
                    val mavenPublicSnapshotTagFlag = settingsList.contains(mavenPublicSnapshotTag)

                    println("mavenPublicTagFlag:$mavenPublicTagFlag")
                    println("mavenPublicReleaseTagFlag:$mavenPublicReleaseTagFlag")
                    println("mavenPublicSnapshotTagFlag:$mavenPublicSnapshotTagFlag")

                    settingsList.forEach { item ->
                        val trim = item.trim()
                        println("item:$item")
                        if (repositoriesFlag) {
                            if (trim == "}") {
                                repositoriesFlag = false

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
                                catalogFlag = true
                            }
                        }
                        mSettingList.add(item)

                        // 3:添加catalog
                        if (catalogFlag) {
                            mSettingList.add(ConfigCatalog.MAVEN_CATALOG)
                            catalogFlag = false
                        }

                        if (trim.startsWith(dependencyResolutionManagement)) {
                            dependencyResolutionManagementFlag = true
                        }

                        if (dependencyResolutionManagementFlag) {
                            if (trim.startsWith(repositories) && !trim.startsWith(repositoriesMode)) {
                                dependencyResolutionManagementFlag = false
                                repositoriesFlag = true
                            }
                        }
                    }
                }

            // mSettingList.forEach {
            //     println("item - add: $it")
            // }

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