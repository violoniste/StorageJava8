package `fun`.irongate.storage.model

import `fun`.irongate.storage.GlobalParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


object Copier : CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    var status = Status.IDLE
        private set

    var copiedFilesCount = 0
        private set

    var skippedFilesCount = 0
        private set

    var deletedFilesCount = 0
        private set

    var totalFilesSize = 0L
        private set

    var totalProgress = 0f
        private set

    var fileProgress = 0f
        private set

    var currentFile = ""
        private set

    private var approximateStorageSize: Long = 0

    fun startCopy() {
        launch(Dispatchers.IO) {
            status = Status.COPYING
            copiedFilesCount = 0
            skippedFilesCount = 0
            totalFilesSize = 0L
            totalProgress = 0f
            fileProgress = 0f
            currentFile = ""

            val storageDir = File(GlobalParams.storagePath)
            approximateStorageSize = storageDir.totalSpace - storageDir.usableSpace

            copyDir(storageDir)

            if (status == Status.INTERRUPTED)
                return@launch

            status = Status.DONE

//            deletedFilesCount = 0
//            val mirrorDir = File(GlobalParams.mirrorPath)
//            status = Status.DELETION
//            totalProgress = 1f
//            fileProgress = 0f
//            currentFile = ""
//
//            clearDir(mirrorDir)
//
//            if (status == Status.INTERRUPTED)
//                return@launch
//
//            status = Status.DONE
        }
    }

    private fun copyDir(storageDir: File) {
        if (status == Status.INTERRUPTED)
            return

        storageDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val mirrorDir = getMirrorFile(file)
                if (!mirrorDir.exists())
                    mirrorDir.mkdirs()

                copyDir(file)
            }
            else {
                copyFile(file)

                totalFilesSize += file.length()
                totalProgress = totalFilesSize.toFloat() / approximateStorageSize.toFloat()
            }
        }
    }

    private fun copyFile(storageFile: File) {
        if (status == Status.INTERRUPTED)
            return

        currentFile = storageFile.path
        val mirrorFile = getMirrorFile(storageFile)

        if (mirrorFile.exists() && mirrorFile.length() == storageFile.length() && mirrorFile.isHidden == storageFile.isHidden) {
            skippedFilesCount++
            return
        }

        if (mirrorFile.exists())
            mirrorFile.delete()

        val inputStream = FileInputStream(storageFile)
        val outputStream = FileOutputStream(mirrorFile)
        try {
            val buffer = ByteArray(1024)
            var length: Int
            fileProgress = 0f
            val fileLength = storageFile.length()
            var sum = 0L
            while ((inputStream.read(buffer).also { length = it }) > 0) {
                if (status == Status.INTERRUPTED)
                    return

                outputStream.write(buffer, 0, length)
                sum += length
                fileProgress = sum.toFloat() / fileLength.toFloat()
            }
            fileProgress = 1f
        }
        catch (ex: Exception) {
            println("Copier.copyFile() $ex")
        }
        finally {
            inputStream.close()
            outputStream.close()
        }

        if (storageFile.isHidden) {
            Runtime.getRuntime().exec("attrib +H ${mirrorFile.path}").waitFor()
        }

        copiedFilesCount++
    }

    private fun getMirrorFile(file: File): File {
        val localPath = file.path.split(GlobalParams.storagePath)[1]
        val destination = GlobalParams.mirrorPath + localPath

        return File(destination)
    }

    private fun clearDir(mirrorDir: File) {
        if (status == Status.INTERRUPTED)
            return

        mirrorDir.listFiles()?.forEach { mirrorFile ->
            val storageFile = getStorageFile(mirrorFile)
            if (!storageFile.exists()) {
                mirrorFile.deleteRecursively()
                deletedFilesCount++
            }
            else {
                if (mirrorFile.isDirectory) {
                    clearDir(mirrorFile)
                }
            }
        }
    }

    private fun getStorageFile(file: File): File {
        val localPath = file.path.split(GlobalParams.mirrorPath)[1]
        val destination = GlobalParams.storagePath + localPath

        return File(destination)
    }

    fun stop() {
        status = Status.INTERRUPTED
    }

    enum class Status { IDLE, COPYING, DELETION, INTERRUPTED, DONE }
}