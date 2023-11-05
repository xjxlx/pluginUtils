package utils

import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.JarFile
import kotlin.io.path.isRegularFile

object FileUtil {

    /**
     * @param file 读取的file对象
     * @return 读取出指定file中的内容，然后返回一个集合
     */
    fun readFile(file: File): List<String>? {
        try {
            return Files.readAllLines(Paths.get(file.absolutePath), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * @param list  集合中的数据
     * @param match 以match开头的匹配符
     * @return 返回集合中以match开始内容
     */
    fun filterStart(list: List<String>, match: String?): List<String> {
        val filter = ArrayList<String>()
        for (i in list.indices) {
            val content = list[i]
            if (content.startsWith(match!!)) {
                filter.add(content)
            }
        }
        return filter
    }

    /**
     * @param array 指定的数组
     * @param match 指定的匹配符
     * @return 返回指定数组中，符合以match开始的匹配符的第一个内容
     */
    fun filterStart(array: Array<File>, match: String?): File? {
        for (file in array) {
            if (file.name.startsWith(match!!)) {
                return file
            }
        }
        return null
    }

    /**
     * @param outPutFile 指定的文件
     * @param content    指定的内容
     * 把指定的内容写入到指定的文件当中去
     */
    fun writeFile(outPutFile: File, content: String) {
        try {
            FileOutputStream(outPutFile).use { outputStream -> outputStream.write(content.toByteArray()) }
        } catch (e: Exception) {
            println("写入数据失败：" + e.message)
        }
    }

    /**
     * @param outPutFile 写入的指定文件
     * @param list 数据集合
     */
    fun writeFile(outPutFile: File, list: List<String>) {
        try {
            if (list.isNotEmpty()) {
                FileOutputStream(outPutFile).use {
                    list.forEach { item ->
                        it.write(item.toByteArray())
                    }
                }
                println("writeFile success!")
            }
        } catch (e: Exception) {
            println("writeFile failed：" + e.message)
        }
    }

    fun writeFile(outFile: File, intPutStream: InputStream) {
        try {
            var len: Int
            val byte = ByteArray(1024)
            FileOutputStream(outFile).use { out ->
                while (intPutStream.read(byte)
                        .also { len = it } != -1) {
                    out.write(byte, 0, len)
                }
            }
        } catch (e: Exception) {
            println("writeFile failed:" + e.message)
        }
    }

    /**
     * @param project 使用插件的project
     * @return 获取当前使用插件的model的build.gradle的文件
     */
    fun getModelGradlePath(project: Project): File? {
        return project.buildscript.sourceFile
    }

    /**
     * @param cls 指定的类名
     * @return 返回jar包中类的本地位置
     */
    fun getFilePathForJar(cls: Class<*>): String? {
        return cls.protectionDomain.codeSource.location.file
    }

    /**
     * @param filePath jar包中文件的具体路径
     * @return 返回jar包中指定文件的inputSteam
     */
    fun getInputStreamForJar(jarPath: String, filePath: String): InputStream? {
        var inputStream: InputStream? = null
        val jarFile = JarFile(File(jarPath))
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name
            if (!entry.isDirectory && entryName.equals(filePath)) {
                inputStream = jarFile.getInputStream(entry)
                break
            }
        }
        return inputStream
    }

    /**
     * delete folder in all child files
     */
    fun deleteFolder(folder: File) {
        Files.walk(folder.toPath())
            .filter { filter ->
                filter.isRegularFile()
            }
            .forEach { file ->
                file.toFile()
                    .delete()
            }
    }

    /**
     * 迭代文件夹下的子文件
     */
    fun iteratorsDirectory(path: String, block: (Path, BasicFileAttributes) -> Unit) {
        val paths = Paths.get(path)
        try {
            Files.walkFileTree(paths, object : SimpleFileVisitor<Path>() {
                override fun visitFile(path: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    // 遍历到的文件或文件夹，进行检查
                    if (path != null && attrs != null) {
                        block(path, attrs)
                    }
                    return super.visitFile(path, attrs)
                }

                override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                    // 处理访问失败的情况，可以记录错误或抛出异常
                    println("[iteratorsDirectory]:[visitFileFailed]:${file} :[error]:${exc?.message}")
                    return super.visitFileFailed(file, exc)
                }
            })
        } catch (e: IOException) {
            e.printStackTrace()
            println("[iteratorsDirectory]:[path]:${path} :[error]:${e.message}")
        }
    }
}