package `fun`.irongate.storage.model

import `fun`.irongate.storage.GlobalParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes


class Cleaner(private val logger: Logger) : CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    var status = Status.READY
        private set

    var error: String? = null
        private set

    var deletedFilesCount = 0
        private set

    var totalFilesCount = 0
        private set

    var totalProgress = 0f
        private set

    private var totalFilesSize = 0L
    private var approximateMirrorSize: Long = 0

    fun startClear() {
        status = Status.CLEARING
        launch {
            val mirrorDir = File(GlobalParams.mirrorPath)
            approximateMirrorSize = mirrorDir.totalSpace - mirrorDir.usableSpace

            clearDir(mirrorDir)

            if (status == Status.ERROR)
                return@launch

            status = Status.DONE
        }
    }

    private fun clearDir(mirrorDir: File) {
        mirrorDir.listFiles()?.forEach { mirrorFile ->
            val storageFile = getStorageFile(mirrorFile)

            if (!mirrorFile.isDirectory) {
                totalFilesSize += mirrorFile.length()
                totalProgress = totalFilesSize.toFloat() / approximateMirrorSize.toFloat()
            }

            if (!storageFile.exists()) {
                mirrorFile.deleteRecursively()
                logger.logln("Удален: ${mirrorFile.absolutePath}", false)
                deletedFilesCount++
            }
            else if (mirrorFile.isDirectory) {
                clearDir(mirrorFile)
            }

            totalFilesCount++
        }
    }

    private fun getStorageFile(file: File): File {
        val localPath = file.path.split(GlobalParams.mirrorPath)[1]
        val destination = GlobalParams.storagePath + localPath

        return File(destination)
    }

    enum class Status { READY, CLEARING, ERROR, DONE }
}