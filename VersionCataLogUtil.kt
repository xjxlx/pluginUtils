package utils

import com.android.publish.PublishPlugin.Companion.mJsonList
import org.gradle.api.Project
import java.io.FileOutputStream

class VersionCataLogUtil {

    private val mListContent = arrayListOf<String>()

    companion object {
        private const val TAG_PLUGIN_MANAGEMENT = "pluginManagement"
        private const val TAG_DEPENDENCY_RESOLUTION_MANAGEMENT = "dependencyResolutionManagement"
        private const val TAG_REPOSITORIES_MODE = "repositoriesMode"
        private const val TAG_REPOSITORIES = "repositories"
        private const val TAG_MAVEN_CATALOG = "versionCatalogs"
        private const val TAG_MAVEN_PUBLIC = "https://maven.aliyun.com/repository/public"
        private const val TAG_MAVEN_RELEASE = "https://packages.aliyun.com/maven/repository/2131155-release-wH01IT/"
        private val MAVEN_PUBLIC: String by lazy {
            return@lazy try {
                mJsonList?.find { find -> find.has("MAVEN_PUBLIC") }?.getString("MAVEN_PUBLIC")
                ""
            } catch (e: Exception) {
                ""
            }
        }
        private val MAVEN_RELEASE: String by lazy {
            return@lazy try {
                mJsonList?.find { it.has("MAVEN_RELEASE") }?.getString("MAVEN_RELEASE")
                ""
            } catch (e: java.lang.Exception) {
                ""
            }
        }
        private val MAVEN_CATALOG: String by lazy {
            return@lazy try {
                mJsonList?.find { it.has("MAVEN_CATALOG") }?.getString("MAVEN_CATALOG")
                ""
            } catch (e: java.lang.Exception) {
                ""
            }
        }
    }

    fun write(project: Project) {
        val settingsFile = project.rootDir.listFiles()
            ?.find { (it.isFile) && (it.name.contains("settings")) && (it.name.startsWith("settings")) }
        settingsFile?.let {
            FileUtil.readFile(settingsFile)
                ?.let { settingsList ->
                    var dependencyResolutionManagementStartFlag = false
                    var repositoriesStartFlag = false

                    var mavenPublicTagFlag = false
                    var mavenPublicReleaseTagFlag = false
                    var catalogFlag = false

                    var count = 0
                    var tempIndex = 0
                    var addCount = 0
                    var addCountFlag = false

                    settingsList.forEachIndexed { index, item ->
                        val trim = item.trim()

                        // 避免错误检测
                        if (trim.startsWith(TAG_PLUGIN_MANAGEMENT)) {
                            dependencyResolutionManagementStartFlag = false
                            repositoriesStartFlag = false
                        }
                        // 检测到了dependencyResolutionManagement标签
                        if (trim.startsWith(TAG_DEPENDENCY_RESOLUTION_MANAGEMENT)) {
                            dependencyResolutionManagementStartFlag = true
                        }
                        // 检测到了repositories标签
                        if (dependencyResolutionManagementStartFlag) {
                            if (trim.startsWith(TAG_REPOSITORIES) && !trim.startsWith(TAG_REPOSITORIES_MODE)) {
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
                                    tempIndex = index
                                    addCountFlag = true
                                }
                            }

                            // 检测中央公共仓库
                            if (!mavenPublicTagFlag) {
                                mavenPublicTagFlag = (trim.contains(TAG_MAVEN_PUBLIC)) && (!trim.startsWith("//"))
                            }
                            // 检测用户信息-release
                            if (!mavenPublicReleaseTagFlag) {
                                mavenPublicReleaseTagFlag = (trim.contains(TAG_MAVEN_RELEASE)) && (!trim.startsWith("//"))
                            }

                            // 检测catalog
                            if (!catalogFlag) {
                                catalogFlag = (trim.contains(TAG_MAVEN_CATALOG)) && (!trim.startsWith("//"))
                            }
                        }
                        mListContent.add(item)
                        // println("item:$item count:$count")
                    }

                    // println("[mavenPublic]:$mavenPublicTagFlag")
                    // println("[mavenPublicRelease]:$mavenPublicReleaseTagFlag")
                    // println("[catalog]:$catalogFlag")

                    // 添加阿里云：用户信息 - release
                    if (!mavenPublicReleaseTagFlag) {
                        mListContent.add(tempIndex, MAVEN_RELEASE)
                        addCount += 1
                    }
                    // 添加中央控制仓库
                    if (!mavenPublicTagFlag) {
                        mListContent.add(tempIndex, MAVEN_PUBLIC)
                        addCount += 1
                    }

                    // 3:添加catalog
                    if (!catalogFlag) {
                        mListContent.add(tempIndex + addCount + 1, MAVEN_CATALOG)
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