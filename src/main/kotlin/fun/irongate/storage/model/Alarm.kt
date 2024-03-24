package `fun`.irongate.storage.model

import `fun`.irongate.storage.GlobalParams
import kotlinx.coroutines.*
import sun.audio.AudioPlayer
import sun.audio.AudioStream
import java.io.File
import java.io.FileInputStream


class Alarm : CoroutineScope {
    companion object {
        private const val ERROR_FILE_NAME = "error.wav"
    }

    override val coroutineContext = Dispatchers.IO

    var status: Status? = null
        private set

    var error: String? = null
        private set

    private var job: Job? = null

    fun init() {
        val dir = File(GlobalParams.soundsPath)
        if (GlobalParams.soundsPath.isEmpty() || !dir.exists() || !dir.isDirectory) {
            error = "Не задан путь к файлу!"
            status = Status.ERROR
            return
        }

        try {
            val inputStream = FileInputStream(dir.absolutePath + "/" + ERROR_FILE_NAME)
            AudioStream(inputStream)
        }
        catch (ex: Exception) {
            error = ex.stackTraceToString()
            status = Status.ERROR
            return
        }

        status = Status.READY
    }

    fun startAlarm() {
        if (job != null)
            return

        status = Status.ALARM

        var duration = 1000L
        job = launch {
            while (true) {
                playSound()
                delay(duration)
                duration += 1000
            }
        }
    }

    private fun playSound() {
        val dir = File(GlobalParams.soundsPath)
        val inputStream = FileInputStream(dir.absolutePath + "/" + ERROR_FILE_NAME)
        val audioStream = AudioStream(inputStream)
        AudioPlayer.player.start(audioStream)
    }

    fun stopAlarm() {
        job?.cancel()
        job = null
        status = Status.READY
    }

    enum class Status { READY, ERROR, ALARM }
}