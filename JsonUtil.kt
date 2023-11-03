package utils

import org.json.JSONArray
import org.json.JSONObject

object JsonUtil {

    fun jsonArrayToJsonObject(array: JSONArray): List<JSONObject> {
        val mutableListOf = mutableListOf<JSONObject>()
        try {
            val first = array.get(0)
                .toString()
            val last = array.get(array.length() - 1)
                .toString()
            array.forEach {
                val content = it.toString()
                if (content.contains(":")) {
                    val json = "$first$it$last"
                    mutableListOf.add(JSONObject(json))
                }
            }
        } catch (e: Exception) {
            println("jsonArrayToJsonObject:error:${e.message}")
        }
        return mutableListOf
    }
}