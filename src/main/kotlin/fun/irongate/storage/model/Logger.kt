package `fun`.irongate.storage.model

import `fun`.irongate.storage.GlobalParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object Logger : CoroutineScope {
    override val coroutineContext = Dispatchers.IO
    private val channel = Channel<String>(Channel.UNLIMITED)
    private val mutex = Mutex()

    init {
        launch {
            while (true) {
                val str = channel.receive()
                Files.write(
                    Paths.get(getLogFile().toURI()),
                    "$str${System.lineSeparator()}".toByteArray(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
                )
            }
        }
    }

    fun log() {
        log("")
    }

    fun log(str: String) {
        launch { channel.send(str) }
    }

    fun checkLogger(): Boolean {
        if (GlobalParams.logPath.isEmpty())
            return false

        val dir = File(GlobalParams.logPath)
        return dir.exists() && dir.isDirectory
    }

    private fun getLogFile(): File {
        val dir = File(GlobalParams.logPath)
        val logFile = File(dir.absolutePath + "/log.txt")
        return logFile
    }
}