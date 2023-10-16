package utils

import org.gradle.api.Project
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

object GradleUtil2 {

    private const val SUPPRESS = "@Suppress(\"DSL_SCOPE_VIOLATION\")"
    private const val PLUGINS = "plugins"
    private const val ID = "id"
    private const val IMPLEMENTATION = "implementation"
    private const val VERSION = "version"
    const val libsVersions = "libs.versions.toml"

    private val mRandomAccessFileUtil = RandomAccessFileUtil()
    private var mRootDir: File? = null
    private var mRootGradle: File? = null
    private var mGradleAnnotate = true
    private val mListLibs = ArrayList<String>()
    private val mListPlugins = ArrayList<String>()
    private val mModuleListContent = ArrayList<String>()
    private val mRootListContent = ArrayList<String>()

    /**
     * @param url       服务器上libs文件的地址
     * @param localLibs 写入项目中文件的地址，一般是在项目中.gradle文件下面，这里交给用户去自己定义
     */
    fun writeGradleToLocal(url: String?, localLibs: File?) {
        //读取线上的html文件地址
        var outputStream: FileOutputStream? = null
        try {
            val doc = Jsoup.connect(url!!)
                .get()
            val body = doc.body()

            localLibs?.let {
                outputStream = FileOutputStream(it)
                for (allElement in body.allElements) {
                    val data = allElement.data()
                    if (data.isNotEmpty()) {
                        if (data.startsWith("{") && data.endsWith("}")) {
                            val jsonObject = JSONObject(data)
                            val jsonPayload = jsonObject.getJSONObject("payload")
                            val jsonBlob = jsonPayload.getJSONObject("blob")
                            val rawLines = jsonBlob.getJSONArray("rawLines")
                            for (next in rawLines) {
                                val content = next.toString()
                                outputStream?.write(content.toByteArray())
                                outputStream?.write("\r\n".toByteArray())
                            }
                            println("gradle-file write success!")
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("gradle-file write failed: " + e.message)
        } finally {
            if (outputStream != null) {
                try {
                    outputStream?.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }

    fun initGradle(project: Project) {
        // 在低于8.0的时候，需要再gradle文件内写入注解 @Suppress("DSL_SCOPE_VIOLATION")
        if (project.gradle.gradleVersion.toFloat() < 8.0f) {
            mGradleAnnotate = true
        }

        mRootDir = project.rootDir

        // 读取本地libs.version.toml文件信息
        readLibsVersions(File(project.rootDir, "gradle${File.separator}${libsVersions}"))
    }

    /**
     * 读取本地libs.versions.toml文件信息
     */
    private fun readLibsVersions(libsFile: File) {
        println("[libsVersions - path]:${libsFile.absolutePath}")

        libsFile.let { libs ->
            if (libs.length() <= 0) {
                println("[libsVersions]: local libs.version.toml content empty!")
                return
            } else {
                try {
                    RandomAccessFile(libs, "r").use { rafGradle ->
                        var libsFlag = false
                        var pluginsFlag = false
                        mListLibs.clear()
                        mListPlugins.clear()
                        while (rafGradle.filePointer < rafGradle.length()) {
                            val readLine = rafGradle.readLine()
                            if (!TextUtil.isEmpty(readLine)) {
                                val versionContent = String(readLine.toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
                                if (libsFlag) {
                                    if (versionContent.startsWith("[plugins]")) {
                                        libsFlag = false
                                    }
                                }
                                // write libs
                                if (libsFlag) {
                                    mListLibs.add(versionContent)
                                }
                                if (versionContent.startsWith("[libraries]")) {
                                    libsFlag = true
                                }

                                // write plugins
                                if (pluginsFlag) {
                                    if (versionContent.startsWith("[bundles]")) {
                                        pluginsFlag = false
                                    }
                                }
                                if (pluginsFlag) {
                                    mListPlugins.add(versionContent)
                                }
                                if (versionContent.startsWith("[plugins]")) {
                                    pluginsFlag = true
                                }
                            }
                        }
                        println("[libsVersions - read:success!]")
                    }
                } catch (exception: Exception) {
                    println("[libsVersions - read:failed]:${exception.message}")
                }
            }
        }
    }

    /**
     * 读取gradle文件信息，进行匹配修改
     *
     * @param gradleFile 更改的gradle文件
     */
    fun changeGradleFile(gradleFile: File?) {
        println("[changeGrad:path]:${gradleFile?.absolutePath}")
        gradleFile?.let { gradle ->
            // 1:清空原始集合
            mModuleListContent.clear()
            // 2: 读取gradle内容
            mModuleListContent.addAll(mRandomAccessFileUtil.readFile(gradle.absolutePath, "r"))
            // 3：转换文件
            try {
                RandomAccessFile(gradle, "rw").use { raf ->
                    // 4:添加注解头
                    if (mGradleAnnotate) {
                        if (!mModuleListContent.contains(SUPPRESS)) {
                            mModuleListContent.add(0, SUPPRESS)
                        }
                    }

                    for (i in mModuleListContent.indices) {
                        val content = mModuleListContent[i]
                        val trim = content.trim { it <= ' ' }
                        if (trim.startsWith(ID)) {
                            // 5:替换 plugins
                            val plugins = replacePlugins(content)
                            mModuleListContent[i] = plugins
                        } else if (trim.startsWith(IMPLEMENTATION)) {
                            // 6:替换 implementation
                            println("$IMPLEMENTATION:$content")
                            val implementation = replaceDependencies(content)
                            mModuleListContent[i] = implementation
                        }
                    }

                    // loop write data
                    for (item in mModuleListContent) {
                        raf.write(item.toByteArray())
                        raf.write("\r\n".toByteArray())
                    }
                }
            } catch (exception: Exception) {
                println("[changeGrad:failed]: ${exception.message}")
            }
        }
    }

    fun changeRootGradle() {
        // root gradle
        mRootDir?.listFiles()
            ?.let { rootFiles ->
                mRootGradle = FileUtil.filterStart(rootFiles, "build.gradle")
                if (mRootGradle != null && mRootGradle!!.exists()) {
                    mRootListContent.clear()
                    mRootListContent.addAll(mRandomAccessFileUtil.readFile(mRootGradle!!.path, "r"))
                    for (i in mRootListContent.indices) {
                        val item = mRootListContent[i]
                        val trim = item.trim { it <= ' ' }
                        if (trim.startsWith(ID)) {
                            val plugin = replacePlugins(item)
                            mRootListContent[i] = plugin
                        } else if (trim.startsWith(PLUGINS)) {
                            if (mGradleAnnotate) { // 添加注解头
                                if (!mRootListContent.contains(SUPPRESS)) {
                                    mRootListContent.add(i, SUPPRESS)
                                }
                            }
                        }
                    }

                    // 添加注解的启用
                    if (mGradleAnnotate) {
                        val lastItem = mRootListContent[mRootListContent.size - 1]
                        if (lastItem != "true") {
                            mRootListContent[mRootListContent.size - 1] = "true"
                        }
                    }

                    // loop write
                    try {
                        RandomAccessFile(mRootGradle!!.path, "rw").use { raf ->
                            for (item in mRootListContent) {
                                raf.write(item.toByteArray())
                                raf.write("\r\n".toByteArray())
                            }
                        }
                    } catch (e: Exception) {
                        println("root-gradle 写入失败：" + e.message)
                    }
                }
            }
    }

    /**
     * 替换dependencies具体的值
     *
     * @param data 原始的数据
     */
    private fun replaceDependencies(data: String): String {
        var result = data
        try {
            if (data.contains(":")) {
                var type = ""
                val flag: Boolean
                val group: String
                val name: String
                val realLeft: String
                val realMiddle: String
                val realRight: String

                //    implementation("org.json:json:20230227")// json 依赖库 " :abc
                //    implementation("org.jsoup:jsoup:1.16.1") // html依赖库
                //    implementation("org.jetbrains.kotlin:kotlin-reflect")

                // 1:确定是用什么进行分割的
                val splitImplementation = data.split(IMPLEMENTATION.toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                realLeft = splitImplementation[0] + IMPLEMENTATION
                val implementationContent = splitImplementation[1].trim { it <= ' ' }

                // 获取右侧去除括号的数据
                flag = implementationContent.startsWith("(")

                // 如果是（ 就跳过第一个数据开始截取
                val allRight: String = if (flag) {
                    implementationContent.substring(1)
                } else {
                    implementationContent
                }
                val allRightTrim = allRight.trim { it <= ' ' }
                if (allRightTrim.startsWith("'")) {
                    type = "'"
                } else if (allRightTrim.startsWith("\"")) {
                    type = "\""
                }

                // 开始分割
                val splitType = implementationContent.split(type.toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val middleLeft = splitType[0]
                val tempMiddle = splitType[1]
                val splitVersion = tempMiddle.split(":".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                group = splitVersion[0]
                name = splitVersion[1]

                // 这里中间长度加2的原因是因为tempMiddle是被type分割出来的，分割的时候，两边的type都会被清除掉，
                // 所以这个要加上分割的字符串长度
                realRight = implementationContent.substring(middleLeft.length + tempMiddle.length + type.length * 2)
                var versions = ""
                for (i in mListLibs.indices) {
                    val line = mListLibs[i]
                    if (line.contains(group) && line.contains(name)) {
                        versions = line
                        break
                    }
                }
                if (versions != "") {
                    println("[replace-dependencies]：$versions")
                    // 取出libs.version.name
                    var libsName = versions.split("=".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0].trim { it <= ' ' }
                    if (libsName.contains("-")) {
                        libsName = libsName.replace("-", ".")
                    }
                    realMiddle = if (flag) {
                        middleLeft + "libs." + libsName
                    } else {
                        middleLeft + "libs." + libsName + ")"
                    }
                    result = realLeft + realMiddle + realRight
                    println("2: result:[$result]")
                }
            }
        } catch (exception: Exception) {
            println("[replace-dependencies - failed]" + exception.message)
        }
        return result
    }

    /**
     * 替换module的plugins内容
     *
     * @param reads plugins的内容
     */
    private fun replacePlugins(reads: String): String {
        var result = reads
        try {
            var type = "" // 分隔符，要么是"要么是'
            var flag = false
            val realLeft: String
            val realMiddle: String
            var realRight: String

            // 先确认是用什么进行分割的，比如：' 或者 "
            val splitID = reads.split(ID.toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            var pluginsContent = splitID[1]
            // 去除所有的空格
            if (pluginsContent.contains(" ")) {
                pluginsContent = pluginsContent.replace(" ", "")
            }
            if (pluginsContent.startsWith("(")) {
                flag = true
                pluginsContent = pluginsContent.replace("(", "")
            }
            if (pluginsContent.startsWith("'")) {
                type = "'"
            } else if (pluginsContent.startsWith("\"")) {
                type = "\""
            }

            // 使用指定的类型去分割字符串
            val split = reads.split(type.toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val tempLeft = split[0]
            var tempContent = split[1]
            val allLeft = tempLeft + type + tempContent
            realRight = reads.replace(allLeft, "")
            // remove "
            realRight = realRight.substring(1)

            // check version
            if (realRight.contains(VERSION)) {
                val versionInfo: String = if (flag) {
                    val splitVersion = realRight.split("\\)".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    splitVersion[1]
                } else {
                    realRight
                }
                val replaceRight = versionInfo.replace("'", "\"")
                val splitRightVersion = replaceRight.split("\"".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val version = splitRightVersion[0]
                val versionCode = splitRightVersion[1]
                realRight = realRight.replace(version, "")
                realRight = realRight.replace("\"" + versionCode + "\"", "")
            }

            // 查找plugins属性
            var pluginLineFlag = false
            val pluginsTemp = tempContent.replace(".", "-")
            for (i in mListPlugins.indices) {
                val pluginSplit = mListPlugins[i].split("=".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].trim { it <= ' ' }
                if (pluginsTemp == pluginSplit) {
                    pluginLineFlag = true
                    break
                }
            }
            if (pluginLineFlag) {
                println("[replace-plugins]：$tempContent")
                if (tempContent.contains("-")) {
                    tempContent = tempContent.replace("-", ".")
                }
                realMiddle = if (!flag) {
                    "(libs.plugins.$tempContent)"
                } else {
                    "libs.plugins.$tempContent"
                }
                realLeft = tempLeft.replace(ID, "alias")
                result = realLeft + realMiddle + realRight
            }
        } catch (e: Exception) {
            println("[replace-plugins - error]：${e.message}")
        }
        return result
    }
}