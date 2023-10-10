package com.android.helper.utils

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

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
}