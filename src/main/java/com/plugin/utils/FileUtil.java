package com.plugin.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileUtil {

    private final String CHARSET_NAME = "UTF-8";

    public List<String> readFile(File file) {
        List<String> content = new ArrayList<>();
        try {
            return Files.readAllLines(Paths.get(file.getAbsolutePath()), Charset.forName(CHARSET_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

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

    public File filterStart(File[] array, String match) {
        ArrayList<String> filter = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            File file = array[i];
            if (file.getName().startsWith(match)) {
                return file;
            }
        }
        return null;
    }


    public List<String> filterStartAndEnd(List<String> listContent, String left, String right) {
        boolean findFlag = false;
        boolean returnFlag = false;
        ArrayList<String> dependenciesList = new ArrayList<>();
        for (int i = 0; i < listContent.size(); i++) {
            String content = listContent.get(i);

            if (content.contains("//")) {
                continue;
            }

            if (content.startsWith(left)) {
                findFlag = true;
                continue;
            }
            if (findFlag) {
                if (content.contains(right)) {
                    findFlag = false;
                    returnFlag = true;
                    continue;
                }
            }


            if (findFlag) {
                if (!content.isEmpty()) {
                    dependenciesList.add(content);
                }
            }

            if (returnFlag) {
                return dependenciesList;
            }
        }
        return dependenciesList;
    }

    public String writeGradleFile(String url, File outputFile) {
        String result = "";
        try {
            //读取线上的html文件地址
            try {
                Document doc = Jsoup.connect(url).get();
                Element body = doc.body();
                FileOutputStream outputStream = null;

                outputStream = new FileOutputStream(outputFile);

                for (Element allElement : body.getAllElements()) {
                    String data = allElement.data();
                    if (!data.isEmpty()) {
                        if (data.startsWith("{") && data.endsWith("}")) {
                            JSONObject jsonObject = new JSONObject(data);
                            JSONObject jsonPayload = jsonObject.getJSONObject("payload");

                            JSONObject jsonBlob = jsonPayload.getJSONObject("blob");
                            JSONArray rawLines = jsonBlob.getJSONArray("rawLines");
                            Iterator<Object> iterator = rawLines.iterator();
                            while (iterator.hasNext()) {
                                Object next = iterator.next();
                                String content = String.valueOf(next);

                                if (content.startsWith("#") || content.startsWith("[")) {
                                    content = "\r\n" + content + "\r\n";
                                }

                                outputStream.write(content.getBytes());
                                if (content.endsWith("\"") || content.endsWith("]") || content.endsWith("}")) {
                                    outputStream.write("\r\n".getBytes());
                                }
                                result += content;
                            }
                            SystemUtil.println("gradle-file write success! ");
                            return result;
                        }
                    }
                }
            } catch (Exception e) {
                SystemUtil.println("body :error: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void writeFile(String content, File outPutFile) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outPutFile);
            outputStream.write(content.getBytes());
        } catch (Exception e) {

        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
            }
        }
    }

}

