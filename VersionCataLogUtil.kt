package utils

import common.Catalog
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import java.io.FileOutputStream

class VersionCataLogUtil {

    private var mSettingList = mutableListOf<String>()

    private val dependencyResolutionManagement = "dependencyResolutionManagement"
    private var dependencyResolutionManagementFlag = false

    private val repositoriesMode = "repositoriesMode"
    private val repositories = "repositories"
    private var repositoriesFlag = false
    private var catalogFlag = false

    fun write(project: Project) {
        val settingsFile = project.rootDir.listFiles()
            ?.find { it.isFile && it.name.contains("settings") }

        settingsFile?.let {
            FileUtil.readFile(settingsFile)
                ?.let { settingsList ->

                    settingsList.forEach { item ->
                        val trim = item.trim()
                        // println("item:$item")
                        if (repositoriesFlag) {
                            if (trim == "}") {
                                repositoriesFlag = false

                                // 1:添加中央控制仓库
                                mSettingList.add(Catalog.MAVEN_PUBLIC)

                                // 2：添加阿里云：用户信息 - release
                                mSettingList.add(Catalog.MAVEN_RELEASE)

                                // 2：添加阿里云：用户信息 - snapshot
                                mSettingList.add(Catalog.MAVEN_SNAPSHOT)
                                catalogFlag = true
                            }
                        }
                        mSettingList.add(item)

                        // 3:添加catalog
                        if (catalogFlag) {
                            mSettingList.add(Catalog.MAVEN_CATALOG)
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