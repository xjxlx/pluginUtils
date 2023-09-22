package com.plugin.utils;

import static com.plugin.utils.SystemUtil.println;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class GradleUtil {

    public void changeGradleFile(File gradle) {
        try {
            RandomAccessFile raf = new RandomAccessFile(gradle.getAbsolutePath(), "rw");
            //定义批量读写的字节大小8kb~10kb
            byte[] data = new byte[1024 * 10];//10kb
            //定义一个用于存储实际读取的字节量的变量
            int len = 0;
            //当文件读取到文件末尾，返回值为-1
            try {
                while ((len = raf.read(data)) != -1) {
                    String content = new String(data, "utf-8");
                    println("content: " + content);

                    long filePointer = raf.getFilePointer();
                    println("filePointer: " + filePointer);

                    content += "123";


                    // ASCII字符替换
                    if (content.contains("NULL")) {
                        raf.write(content.replace("NULL", "").getBytes());
                    } else {
                        raf.write(content.getBytes());
                    }
                }
            } catch (IOException ioException) {
                println("read file error: " + ioException.getMessage());
            }
        } catch (FileNotFoundException e) {
            println("gradle file not found: " + gradle.getAbsolutePath());
        }
    }

    private void checkDependencies(RandomAccessFile raf, String data) {


    }
}
