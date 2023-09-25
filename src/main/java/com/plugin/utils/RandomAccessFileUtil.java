package com.plugin.utils;

import static com.plugin.utils.SystemUtil.println;

import java.io.RandomAccessFile;

import kotlin.text.Charsets;

public class RandomAccessFileUtil {

    public void change(String path, String mode) {
        try (RandomAccessFile raf = new RandomAccessFile(path, mode)) {
            long point = 0;
            int interval = 0;
            long length = raf.length();
            println("length --->: " + length);

            while (raf.getFilePointer() != raf.length()) {
                String reads = new String(raf.readLine().getBytes(Charsets.ISO_8859_1), Charsets.UTF_8);
                // 插入数据
                println("read :" + reads);
                insert(raf, reads, "id", "魔改", point);
                point = (raf.getFilePointer());
            }
        } catch (Exception e) {
            println("读取文件异常: " + e.getMessage());
        }
    }

    String left = "";

    public void insert(RandomAccessFile raf, String content, String oldChar, String newChar, long index) {
        if (content.contains("\"")) {
            try {
                String[] split = content.split("\"");
                if (left.equals("")) {
                    left = split[0].replace(oldChar, newChar);
                }
                String middle = split[1];
                String right = split[2];

                String result = left + "\"" + middle + "\"" + right;
                println("result :" + result);
                raf.seek(index);
                raf.write(result.getBytes());
                raf.write("\r\n".getBytes(Charsets.UTF_8));

            } catch (Exception e) {
                println("插入数据异常：" + e.getMessage());
            }
        }
    }


}
