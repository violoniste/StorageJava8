package `fun`.irongate.storage.controllers

import `fun`.irongate.storage.GlobalParams
import `fun`.irongate.storage.GlobalParams.SCREEN_WIDTH
import `fun`.irongate.storage.model.Copier
import `fun`.irongate.storage.utils.StringUtils
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class StatusScreenController : CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    @FXML
    private lateinit var btnStart: Button

    @FXML
    private lateinit var labelFileProgress: Label

    @FXML
    private lateinit var labelStatus: Label

    @FXML
    private lateinit var labelTotalProgress: Label

    @FXML
    private lateinit var progressBarFileProgress: ProgressBar

    @FXML
    private lateinit var progressBarTotalProgress: ProgressBar

    private var totalSpace: Long = 0
    private var usableSpace: Long = 0

    init { init() }

    private fun init() {
        launch {
            if (!checkDisks())
                return@launch

            println("Готово к запуску.")

            startCopy()

            startClear()
        }

        listenConsole()
    }

    private fun checkDisks(): Boolean {
        val storage = File(GlobalParams.storagePath)
        val mirror = File(GlobalParams.mirrorPath)

        if (!storage.exists() || !storage.isDirectory || !mirror.exists() || !mirror.isDirectory) {
            println("Один из дисков отсутствует!!!")
            return false
        }

        totalSpace = storage.totalSpace
        usableSpace = storage.usableSpace

        val progress = 1 - usableSpace.toFloat() / totalSpace.toFloat()

        val builder = StringBuilder(SCREEN_WIDTH)
        builder.append('[')
        builder.append(getDoubleBarString(100, progress, progress))
        builder.append(']')
        println(builder.toString())

        println("${StringUtils.sizeToString(usableSpace)} из ${StringUtils.sizeToString(totalSpace)} свободно")

        return true
    }

    private suspend fun startCopy() {
        println()
        println("Копирование...")

        Copier.startCopy()

        while (true) {
            delay(16)

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append('[')
            builder.append(getDoubleBarString(100, Copier.totalProgress, Copier.fileProgress))
            builder.append(']')
            builder.append(" ${Copier.copiedFilesCount}/${Copier.copiedFilesCount + Copier.skippedFilesCount}")
            print(builder.toString())

            if (Copier.status != Copier.Status.COPYING)
                break
        }

        println()
        println("Скопировано: ${Copier.copiedFilesCount}")
        println("Пропущено: ${Copier.skippedFilesCount}")
        println("Объемом: ${StringUtils.sizeToString(Copier.totalFilesSize)}")
    }

    private suspend fun startClear() {
        println()
        println("Очистка...")

        Copier.startClear()

        while (true) {
            delay(16)

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append('[')
            builder.append(getDoubleBarString(100, Copier.totalProgress, Copier.totalProgress))
            builder.append(']')
            builder.append(" ${Copier.deletedFilesCount}")
            print(builder.toString())

            if (Copier.status != Copier.Status.CLEARING)
                break
        }

        println()
        println("Удалено: ${Copier.deletedFilesCount}")
    }

    private fun getDoubleBarString(width: Int, progressTop: Float, progressBot: Float): String {
        val builder = StringBuilder(width)
        for (i in 1.. width) {
            val cellProgress = i / width.toFloat()
            builder.append(
                if (cellProgress <= progressTop && cellProgress <= progressBot) "█"
                else if (cellProgress <= progressTop) "▀"
                else if (cellProgress <= progressBot) "▄"
                else " "
            )
        }
        return builder.toString()
    }

    private fun listenConsole() {
        val keyboard = Scanner(System.`in`)
        while (true) {
            val input = keyboard.nextLine()
            when (input) {
                "exit" -> exitProcess(0)
                "start" -> Copier.startCopy()
                else -> println("Unknown command: $input")
            }
        }
    }
}