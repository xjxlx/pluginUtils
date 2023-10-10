package utils

object TextUtil {

    fun isEmpty(text: String?): Boolean {
        if (text == null) {
            return true
        } else {
            if (text.isEmpty()) {
                return true
            }
        }
        return false
    }
}