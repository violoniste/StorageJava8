package `fun`.irongate.storage.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

class DiskChecker(private val disk: File, private val sample: File) : CoroutineScope {
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

    var checkedFilesCount = 0
        private set

    var totalFilesCount = 0
        private set

    var totalProgress = 0f
        private set

    var fileProgress = 0f
        private set

    private var totalFilesSize = 0L
    private var approximateStorageSize: Long = 0

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
            approximateStorageSize = totalSpace - usableSpace
            if (totalSpace < 1_000_000_000 || usableSpace < 1_000_000_000) {
                error = "Неверные параметры диска! totalSpace=$totalSpace usableSpace=$usableSpace"
                status = Status.ERROR
                return@launch
            }

            progressOccupiedSpace = 1 - usableSpace.toFloat() / totalSpace.toFloat()

            checkDir(disk)

            if (status == Status.ERROR)
                return@launch

            status = Status.OK
        }
    }

    private fun checkDir(diskDir: File) {
        diskDir.listFiles()?.forEach { file ->
            if (file.isDirectory)
                checkDir(file)
            else
                checkFile(file)
        }
    }

    private fun checkFile(diskFile: File) {
        val sampleFile = getSampleFile(diskFile)

        if (sampleFile.exists() && sampleFile.length() != diskFile.length()) {
            val inputStream = FileInputStream(diskFile)
            val fileLength = diskFile.length()
            var sum = 0L
            try {
                val buffer = ByteArray(1024)
                fileProgress = 0f
                var readed: Int
                while ((inputStream.read(buffer).also { readed = it }) > 0) {
                    sum += readed
                    fileProgress = sum.toFloat() / fileLength.toFloat()
                }

                fileProgress = 1f
            }
            catch (ex: Exception) {
                error = ex.stackTraceToString()
                status = Status.ERROR
            }
            finally {
                inputStream.close()
            }

            if (status == Status.ERROR)
                return

            if (fileLength != sum) {
                error = "Отличается пересчитанный размер файла: ${diskFile.absolutePath}"
                status = Status.ERROR
                return
            }

            checkedFilesCount++
        }

        totalFilesCount++

        totalFilesSize += diskFile.length()
        totalProgress = totalFilesSize.toFloat() / approximateStorageSize.toFloat()
    }

    private fun getSampleFile(file: File): File {
        val localPath = file.absolutePath.split(disk.absolutePath)[1]
        val destination = sample.absolutePath + localPath

        return File(destination)
    }

    enum class Status { READY, CHECKING, ERROR, OK }
}