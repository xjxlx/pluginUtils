package com.plugin.utils;

import static com.plugin.utils.SystemUtil.println;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RandomAccessFileUtil {

    public ArrayList<String> readFile(String path, String mode) {
        ArrayList<String> list = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(path, mode)) {
            // RandomAccessFile默认使用的是【iso-8859-1】字符集，使用的时候，需要把他转换成UTF-8
            long currentPointer = 0;
            long length = raf.length();
            while (currentPointer < length) {
                long filePointer = raf.getFilePointer();
                String line = raf.readLine();
                if (line != null) {
                    String reads = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    list.add(reads);
                    // println("read:" + reads + " filePointer:" + currentPointer);
                }
                currentPointer = filePointer;
            }
            raf.close();
        } catch (Exception e) {
            println("read file failed: " + e.getMessage());
        }
        return list;
    }

}
