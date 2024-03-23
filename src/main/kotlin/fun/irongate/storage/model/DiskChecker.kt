package `fun`.irongate.storage.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DiskChecker(private val disk: File) : CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    var status = Status.READY
        private set

    var totalSpace = 0L
        private set

    var usableSpace = 0L
        private set

    var progressOccupiedSpace: Float = 0f
        private set

    var error: String? = null
        private set

    fun checkDisk() {
        status = Status.CHECKING

        launch {
            if (!disk.exists() || !disk.isDirectory) {
                error = "Диск отсутствует!!!"
                status = Status.ERROR
                return@launch
            }

            totalSpace = disk.totalSpace
            usableSpace = disk.usableSpace
            if (totalSpace < 1_000_000_000 || usableSpace < 1_000_000_000) {
                error = "Неверные параметры диска! totalSpace=$totalSpace usableSpace=$usableSpace"
                status = Status.ERROR
                return@launch
            }

            progressOccupiedSpace = 1 - usableSpace.toFloat() / totalSpace.toFloat()

            status = Status.READY
        }
    }

    enum class Status { READY, CHECKING, ERROR, OK }
}