package `fun`.irongate.storage.utils

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
}