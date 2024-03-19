package `fun`.irongate.storage.controllers

import `fun`.irongate.storage.GlobalParams
import `fun`.irongate.storage.GlobalParams.SCREEN_WIDTH
import `fun`.irongate.storage.model.Copier
import `fun`.irongate.storage.utils.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class StatusScreenController : CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    private var totalSpace: Long = 0
    private var usableSpace: Long = 0

    init { init() }

    private fun init() {
        if (!checkDisks()) {
            val scanner = Scanner(System.`in`)
            scanner.nextLine()
            return
        }

        println("${getCurrentTimeStr()} Готово к запуску.")

        startClock()

        listenConsole()
    }

    private fun checkDisks(): Boolean {
        val storage = File(GlobalParams.storagePath)
        val mirror = File(GlobalParams.mirrorPath)

        if (!storage.exists() || !storage.isDirectory || !mirror.exists() || !mirror.isDirectory) {
            println("${getCurrentTimeStr()} Один из дисков отсутствует!!!")
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

    private fun startClock() {
        launch {
            while (true) {
                delay(60 * 1000)

                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                if (hour == 4 && minute == 0 && Copier.status == Copier.Status.READY) {
                    start()
                }
            }
        }
    }

    private fun listenConsole() {
        val keyboard = Scanner(System.`in`)
        while (true) {
            when (val input = keyboard.nextLine()) {
                "exit" -> exitProcess(0)
                "start" -> start()
                "clear" -> clear()
                else -> println("Unknown command: $input")
            }
        }
    }

    private fun start() {
        launch {
            startCopy()
            startClear()
        }
    }

    private fun clear() {
        launch {
            startClear()
        }
    }

    private suspend fun startCopy() {
        println()
        println("${getCurrentTimeStr()} Копирование...")

        Copier.startCopy()

        while (true) {
            delay(16)

            val totalProgress = if (Copier.status == Copier.Status.COPYING) Copier.totalProgress else 100f
            val fileProgress = if (Copier.status == Copier.Status.COPYING) Copier.fileProgress else 100f

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append('[')
            builder.append(getDoubleBarString(100, totalProgress, fileProgress))
            builder.append(']')
            builder.append(" ${Copier.copiedFilesCount}/${Copier.copiedFilesCount + Copier.skippedFilesCount}")
            print(builder.toString())

            if (Copier.status != Copier.Status.COPYING)
                break
        }

        clearString()
        println("${getCurrentTimeStr()} Завершено:")
        println("Скопировано: ${Copier.copiedFilesCount}")
        println("Пропущено: ${Copier.skippedFilesCount}")
        println("Объемом: ${StringUtils.sizeToString(Copier.totalFilesSize)}")
    }

    private suspend fun startClear() {
        println()
        println("${getCurrentTimeStr()} Очистка...")

        Copier.startClear()

        while (true) {
            delay(16)

            val totalProgress = if (Copier.status == Copier.Status.CLEARING) Copier.totalProgress else 100f

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append('[')
            builder.append(getDoubleBarString(100, totalProgress, totalProgress))
            builder.append(']')
            builder.append(" ${Copier.deletedFilesCount}")
            print(builder.toString())

            if (Copier.status != Copier.Status.CLEARING)
                break
        }

        clearString()
        println("${getCurrentTimeStr()} Завершено:")
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

    private fun getCurrentTimeStr(): String {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR).toString()

        var month = (calendar.get(Calendar.MONTH) + 1).toString()
        if (month.length < 2)
            month = "0$month"

        var day = calendar.get(Calendar.DAY_OF_MONTH).toString()
        if (day.length < 2)
            day = "0$day"

        var hour = calendar.get(Calendar.HOUR_OF_DAY).toString()
        if (hour.length < 2)
            hour = "0$hour"

        var min = calendar.get(Calendar.MINUTE).toString()
        if (min.length < 2)
            min = "0$min"

        var sec = calendar.get(Calendar.SECOND).toString()
        if (sec.length < 2)
            sec = "0$sec"

        return "$year.$month.$day $hour:$min:$sec"
    }

    private fun clearString() {
        print("\r${String(CharArray(SCREEN_WIDTH)).replace('\u0000', ' ')}\r")
    }
}