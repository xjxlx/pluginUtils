package com.plugin.utils;

import static com.plugin.utils.SystemUtil.println;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {


    /**
     * @param file 读取的file对象
     * @return 读取出指定file中的内容，然后返回一个集合
     */
    public List<String> readFile(File file) {
        try {
            return Files.readAllLines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param list  集合中的数据
     * @param match 以match开头的匹配符
     * @return 返回集合中以match开始内容
     */
    public List<String> filterStart(List<String> list, String match) {
        ArrayList<String> filter = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String content = list.get(i);
            if (content.startsWith(match)) {
                filter.add(content);
            }
        }
        return filter;
    }

    /**
     * @param array 指定的数组
     * @param match 指定的匹配符
     * @return 返回指定数组中，符合以match开始的匹配符的第一个内容
     */
    public File filterStart(File[] array, String match) {
        for (File file : array) {
            if (file.getName().startsWith(match)) {
                return file;
            }
        }
        return null;
    }


    /**
     * @param outPutFile 指定的文件
     * @param content    指定的内容
     *                   把指定的内容写入到指定的文件当中去
     */
    public void writeFile(File outPutFile, String content) {
        try (FileOutputStream outputStream = new FileOutputStream(outPutFile)) {
            outputStream.write(content.getBytes());
        } catch (Exception e) {
            println("写入数据失败：" + e.getMessage());
        }
    }

}

