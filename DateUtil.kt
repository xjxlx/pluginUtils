package utils

import java.util.Calendar

object DateUtil {

    /**
     * 获取当前的时间
     * @return YYYY-MM-DD-dd-hh-mm
     */
    fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return "$year-${month + 1}-$day-$hour-$minute"
    }
}