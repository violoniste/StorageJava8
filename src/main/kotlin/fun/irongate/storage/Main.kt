package `fun`.irongate.`fun`.irongate.storage

import `fun`.irongate.storage.GlobalParams
import `fun`.irongate.storage.controllers.StatusScreenController
import java.util.*
import kotlin.math.log10


fun main(vararg args: String) {
    val params = argToMap(args)
    GlobalParams.storagePath = params["storage"] ?: return
    GlobalParams.mirrorPath = params["mirror"] ?: return

    StatusScreenController()


//    for (i in 0..100) {
//        Thread.sleep(50)
//        printProgress(i)
//    }
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

private fun printProgress(percent: Int) {
    val string = StringBuilder(140)
    string
        .append('\r')
        .append(
            java.lang.String.join(
                "", Collections.nCopies(
                    if (percent == 0) 2 else 2 - log10(percent.toDouble())
                        .toInt(), " "
                )
            )
        )
        .append(String.format(" %d%% [", percent))
        .append(java.lang.String.join("", Collections.nCopies(percent, "=")))
        .append('>')
        .append(java.lang.String.join("", Collections.nCopies(100 - percent, " ")))
        .append(']')

    print(string)
}