package `fun`.irongate.`fun`.irongate.storage

import `fun`.irongate.storage.GlobalParams
import `fun`.irongate.storage.controllers.StatusScreenController
import java.util.*
import kotlin.math.log10


fun main(vararg args: String) {
    val params = argToMap(args)
    params["storage"]?.let { GlobalParams.storagePath = it }
    params["mirror"]?.let { GlobalParams.mirrorPath = it }
    params["log"]?.let { GlobalParams.logPath = it }
    params["sounds"]?.let { GlobalParams.soundsPath = it }

    StatusScreenController()
}

private fun argToMap(args: Array<out String>): Map<String, String> {
    val map = HashMap<String, String>()
    for (a in args) {
        val split = a.split("--").getOrNull(1)?.split("=") ?: continue
        val key = split.getOrNull(0) ?: continue
        val value = split.getOrNull(1) ?: continue
        map[key] = value
    }
    return map
}