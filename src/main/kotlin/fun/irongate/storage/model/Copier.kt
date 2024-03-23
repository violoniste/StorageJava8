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


class Copier : CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    var status = Status.READY
        private set

    var error: String? = null
        private set

    var copiedFilesCount = 0
        private set

    var totalFilesCount = 0
        private set

    var copiedFilesSize = 0L
        private set

    var totalProgress = 0f
        private set

    var fileProgress = 0f
        private set

    private var totalFilesSize = 0L

    private var approximateStorageSize: Long = 0

    fun startCopy() {
        status = Status.COPYING
        launch(Dispatchers.IO) {
            val storageDir = File(GlobalParams.storagePath)
            approximateStorageSize = storageDir.totalSpace - storageDir.usableSpace

            copyDir(storageDir)

            if (status == Status.ERROR)
                return@launch

            status = Status.DONE
        }
    }

    private fun copyDir(storageDir: File) {
        if (status == Status.ERROR)
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
            }
        }
    }

    private fun copyFile(storageFile: File) {
        if (status == Status.ERROR)
            return

        val mirrorFile = getMirrorFile(storageFile)

        if (!mirrorFile.exists() || mirrorFile.length() != storageFile.length()) {
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
                    outputStream.write(buffer, 0, length)
                    sum += length
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
                outputStream.close()
            }

            if (status == Status.ERROR)
                return

            val attrs: BasicFileAttributes = Files.readAttributes(Paths.get(storageFile.toURI()), BasicFileAttributes::class.java)
            val tgtView = Files.getFileAttributeView(Paths.get(mirrorFile.toURI()), BasicFileAttributeView::class.java)
            tgtView.setTimes(attrs.lastModifiedTime(), attrs.lastAccessTime(), attrs.creationTime())

            copiedFilesCount++
            copiedFilesSize += storageFile.length()
        }

        totalFilesCount++

        totalFilesSize += storageFile.length()
        totalProgress = totalFilesSize.toFloat() / approximateStorageSize.toFloat()

        checkAttributes(storageFile, mirrorFile)
    }

    private fun getMirrorFile(file: File): File {
        val localPath = file.path.split(GlobalParams.storagePath)[1]
        val destination = GlobalParams.mirrorPath + localPath

        return File(destination)
    }

    private fun checkAttributes(storageFile: File, mirrorFile: File) {
        if (storageFile.isHidden && !mirrorFile.isHidden)
            Runtime.getRuntime().exec("attrib +H ${mirrorFile.path}").waitFor()

        if (!storageFile.isHidden && mirrorFile.isHidden)
            Runtime.getRuntime().exec("attrib -H ${mirrorFile.path}").waitFor()
    }

    enum class Status { READY, COPYING, ERROR, DONE }
}