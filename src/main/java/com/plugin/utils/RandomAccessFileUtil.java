package com.plugin.utils;

import static com.plugin.utils.SystemUtil.println;

import java.io.RandomAccessFile;

import kotlin.text.Charsets;

public class RandomAccessFileUtil {

    public void change(String path, String mode) {
        try (RandomAccessFile raf = new RandomAccessFile(path, mode)) {
            long point = 0;
            while (raf.getFilePointer() != raf.length()) {

                String reads = new String(raf.readLine().getBytes(Charsets.ISO_8859_1), Charsets.UTF_8);
                println("reads: " + reads);

                // 插入数据
                insert(raf, reads, point);
                point = raf.getFilePointer();
            }
        } catch (Exception e) {
            println("读取文件异常: " + e.getMessage());
        }
    }

    public void insert(RandomAccessFile raf, String content, long index) {
        if (content.contains("\"")) {
            try {
                raf.seek(index);
                String change = "魔改诗句";
                String result = change + content;
//                raf.write(result.getBytes(Charsets.UTF_8));
                raf.writeUTF(result);
            } catch (Exception e) {
                println("插入数据异常：" + e.getMessage());
            }
        }
    }


}
