package utils

import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

class RandomAccessFileUtil {
    fun readFile(path: String?, mode: String?): ArrayList<String> {
        val list = ArrayList<String>()
        try {
            RandomAccessFile(path, mode).use { raf ->
                // RandomAccessFile默认使用的是【iso-8859-1】字符集，使用的时候，需要把他转换成UTF-8
                var currentPointer: Long = 0
                val length = raf.length()
                while (currentPointer < length) {
                    val filePointer = raf.filePointer
                    val line = raf.readLine()
                    if (line != null) {
                        val reads = String(line.toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
                        list.add(reads)
                        // println("read:" + reads + " filePointer:" + currentPointer);
                    }
                    currentPointer = filePointer
                }
                raf.close()
            }
        } catch (e: Exception) {
            println("read file failed: " + e.message)
        }
        return list
    }
}