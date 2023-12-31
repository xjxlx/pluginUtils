package utils

import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object HtmlUtil {

    fun getHtmlForGithub(url: String): List<String> {
        val htmlList = arrayListOf<String>()
        val doc: Document = Jsoup.connect(url)
            .get()
        val body: Element = doc.body()
        for (allElement in body.allElements) {
            val data: String = allElement.data()
            if (data.isNotEmpty()) {
                if (data.startsWith("{") && data.endsWith("}")) {
                    val jsonObject: org.json.JSONObject = org.json.JSONObject(data)
                    val jsonPayload: org.json.JSONObject = jsonObject.getJSONObject("payload")
                    val jsonBlob: org.json.JSONObject = jsonPayload.getJSONObject("blob")
                    val rawLines: JSONArray = jsonBlob.getJSONArray("rawLines")
                    for (next in rawLines) {
                        val content = next.toString()
                        htmlList.add(content)
                        htmlList.add("\r\n")
                    }
                    println("html-path:$url")
                    break
                }
            }
        }
        return htmlList
    }

    fun getHtmlForGithubJsonArray(url: String): JSONArray? {
        val doc: Document = Jsoup.connect(url)
            .get()
        val body: Element = doc.body()
        for (allElement in body.allElements) {
            val data: String = allElement.data()
            if (data.isNotEmpty()) {
                if (data.startsWith("{") && data.endsWith("}")) {
                    val jsonObject: org.json.JSONObject = org.json.JSONObject(data)
                    val jsonPayload: org.json.JSONObject = jsonObject.getJSONObject("payload")
                    val jsonBlob: org.json.JSONObject = jsonPayload.getJSONObject("blob")
                    return jsonBlob.getJSONArray("rawLines")
                }
            }
        }
        return null
    }
}