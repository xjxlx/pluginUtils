package utils

import org.gradle.api.Project
import org.gradle.internal.impldep.org.apache.http.util.TextUtils
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

object GradleUtil2 {

    private const val SUPPRESS = "@Suppress(\"DSL_SCOPE_VIOLATION\")"
    private const val PLUGINS = "plugins"
    private const val ID = "id"
    private const val IMPLEMENTATION = "implementation"
    private const val VERSION = "version"

    private val mRandomAccessFileUtil = RandomAccessFileUtil()
    private var mRootDir: File? = null
    private var mRootGradle: File? = null
    private var mLocalLibs: File? = null
    private var mGradleAnnotate = true
    private val mListLibs = ArrayList<String>()
    private val mListPlugins = ArrayList<String>()
    private val mModuleListContent = ArrayList<String>()
    private val mRootListContent = ArrayList<String>()

    /**
     * 获取本地jar包中的文件信息
     */
    fun getJarInputStream(clazz: Class<*>): InputStream? {
        // 1：获取jar包中路径
        FileUtil.getFilePathForJar(clazz)
            ?.let { filePathForJar ->
                // 2：读取jar包中指定的文件内容
                return FileUtil.getInputStreamForJar(filePathForJar, "libs.versions.toml")
            }
        return null
    }

    fun initGradle(project: Project) {
        // 在低于8.0的时候，需要再gradle文件内写入注解 @Suppress("DSL_SCOPE_VIOLATION")
        if (project.gradle.gradleVersion.toFloat() < 8.0f) {
            mGradleAnnotate = true
        }

        println("gradle init !")
        mRootDir = project.rootDir

        // 读取本地libs.version.toml文件信息
        readLibsVersions()
    }

    /**
     * 读取本地libs.versions.toml文件信息
     */
    private fun readLibsVersions() {
        if (mLocalLibs == null) {
            mLocalLibs = File(mRootDir, "gradle/libs.versions.toml")
        }

        if (!mLocalLibs!!.exists()) {
            println("本地的libs.version.toml文件不存在！")
            return
        }

        if (mLocalLibs!!.length() == 0L) {
            println("本地的libs.version.toml内容为空！")
            return
        }

        try {
            RandomAccessFile(mLocalLibs, "r").use { rafGradle ->
                var libsFlag = false
                var pluginsFlag = false
                mListLibs.clear()
                mListPlugins.clear()
                while (rafGradle.filePointer < rafGradle.length()) {
                    val readLine = rafGradle.readLine()
                    if (!TextUtils.isEmpty(readLine)) {
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
                println("读取library成功 :$mListLibs")
                println("读取plugins成功 :$mListPlugins")
            }
        } catch (exception: Exception) {
            println("读取library失败 :" + exception.message)
        }
    }

    /**
     * @param url       服务器上libs文件的地址
     * @param localLibs 写入项目中文件的地址，一般是在项目中.gradle文件下面，这里交给用户去自己定义
     */
    fun writeGradleToLocal(url: String?, localLibs: File?) {
        mLocalLibs = localLibs
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

    fun changeModules() {
        // 2：获取project的目录信息，保活settings、module、library...
        mRootDir?.listFiles()
            ?.let { rootFiles ->
                FileUtil.filterStart(rootFiles, "settings.gradle")
                    ?.let { settingsGradle ->
                        // 读取settings文件的内容
                        FileUtil.readFile(settingsGradle)
                            ?.let { settingContent ->
                                // 过滤引入include的信息，就是model的名字
                                FileUtil.filterStart(settingContent, "include")
                                    .forEach { name ->
                                        // 获取module的名字，然后批量进行gradle文件修改
                                        FileUtil.filterStart(rootFiles, name)
                                            ?.let { moduleFile ->
                                                // module文件存在
                                                moduleFile.listFiles()
                                                    ?.let { moduleFiles ->
                                                        // 获取module下的build.gradle文件
                                                        FileUtil.filterStart(moduleFiles, "build.gradle")
                                                            ?.let { buildGradle ->
                                                                println("当前的module:$name")
                                                                changeModuleGradleFile(buildGradle)
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
        //  changeGradleFile(new File("/Users/XJX/AndroidStudioProjects/plugins/pluginUtil/src/main/java/com/plugin/utils/TestData.txt"));
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
     * @param includeList settings中include的内容
     * @return 返回module名字的集合
     */
    private fun filterModel(includeList: List<String>?): List<String> {
        val modelName = ArrayList<String>()
        if (includeList != null) {
            for (i in includeList.indices) {
                val include = includeList[i]

                // split
                val split = include.split(":".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                for (s in split) {
                    var splitContent = s
                    if (splitContent.contains("(")) {
                        continue
                    }
                    if (splitContent.contains(")")) {
                        splitContent = splitContent.replace("\")", "")
                    }
                    modelName.add(splitContent.trim { it <= ' ' })
                }
            }
        }
        return modelName
    }

    /**
     * 读取gradle文件信息，进行匹配修改
     *
     * @param gradle 对应的gradle文件
     */
    private fun changeModuleGradleFile(gradle: File) {
        // 读取所有的字符串存入集合
        mModuleListContent.clear()
        val gradlePath = gradle.absolutePath
        // 1：读取gradle中的文件
        mModuleListContent.addAll(mRandomAccessFileUtil.readFile(gradlePath, "r"))
        try {
            RandomAccessFile(gradlePath, "rw").use { raf ->
                // 2:添加注解头
                if (mGradleAnnotate) {
                    if (!mModuleListContent.contains(SUPPRESS)) {
                        mModuleListContent.add(0, SUPPRESS)
                    }
                }
                for (i in mModuleListContent.indices) {
                    val content = mModuleListContent[i]
                    val trim = content.trim { it <= ' ' }
                    if (trim.startsWith(ID)) {
                        // 3:替换 plugins
                        println("id: $content")
                        val plugins = replacePlugins(content)
                        mModuleListContent[i] = plugins
                    } else if (trim.startsWith(IMPLEMENTATION)) {
                        // 4:替换 implementation
                        println("$IMPLEMENTATION:$content")
                        val implementation = replaceModuleDependencies(content)
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
            println("gradle 信息写入失败: " + exception.message)
        }
    }

    /**
     * 替换dependencies具体的值
     *
     * @param data 原始的数据
     */
    private fun replaceModuleDependencies(data: String): String {
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
                    println("1：找到了对应的implementation属性：$versions")
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
                } else {
                    println("1：找不到对应的implementation属性：$group-$name")
                }
            }
        } catch (exception: Exception) {
            println("写入implementation属性失败：" + exception.message)
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
                println("找到plugin属性：$tempContent")
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
            } else {
                println("找不到plugin属性：$tempContent")
            }
        } catch (e: Exception) {
            println("module:plugins: [" + "" + "] write failed!")
        }
        return result
    }
}