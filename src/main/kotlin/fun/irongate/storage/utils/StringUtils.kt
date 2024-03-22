package `fun`.irongate.storage.utils

import java.util.*

object StringUtils {
    fun sizeToString(size: Long): String {
        if (size > 1_000_000_000)
            return "${size / 1_000_000_000}Gb"

        if (size > 1_000_000)
            return "${size / 1_000_000}Mb"

        if (size > 1000)
            return "${size / 1000}Kb"

        return "${size}b"
    }

    fun getCurrentTimeStr(): String {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR).toString()

        var month = (calendar.get(Calendar.MONTH) + 1).toString()
        if (month.length < 2)
            month = "0$month"

        var day = calendar.get(Calendar.DAY_OF_MONTH).toString()
        if (day.length < 2)
            day = "0$day"

        var hour = calendar.get(Calendar.HOUR_OF_DAY).toString()
        if (hour.length < 2)
            hour = "0$hour"

        var min = calendar.get(Calendar.MINUTE).toString()
        if (min.length < 2)
            min = "0$min"

        var sec = calendar.get(Calendar.SECOND).toString()
        if (sec.length < 2)
            sec = "0$sec"

        return "$year.$month.$day $hour:$min:$sec"
    }
}