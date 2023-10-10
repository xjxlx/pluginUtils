package utils

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project

object VersionUtil {

    val version: String
        get() {
            val latestGitTag = latestGitTag()
            return if (TextUtil.isEmpty(latestGitTag)) {
                "master-SNAPSHOT"
            } else {
                latestGitTag
            }
        }

    /**
     * 获取git仓库中最新的tag作为版本号
     */
    private fun latestGitTag(): String {
        val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0").start()
        return process.inputStream.bufferedReader()
            .use { bufferedReader ->
                bufferedReader.readText()
                    .trim()
            }
    }

    /**
     * 获取model的name
     */
    fun getModelNameForNamespace(project: Project): String {
        var result = "model"
        project.extensions.getByType(LibraryExtension::class.java).namespace?.let {
            if (it.contains(".")) {
                result = it.split(".")
                    .reversed()
                    .first()
            }
        }
        return result
    }
}