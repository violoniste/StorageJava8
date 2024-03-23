package `fun`.irongate.storage.controllers

import `fun`.irongate.storage.GlobalParams
import `fun`.irongate.storage.GlobalParams.SCREEN_WIDTH
import `fun`.irongate.storage.model.Copier
import `fun`.irongate.storage.model.DiskChecker
import `fun`.irongate.storage.model.Logger
import `fun`.irongate.storage.utils.StringUtils
import `fun`.irongate.storage.utils.StringUtils.getBarString
import `fun`.irongate.storage.utils.StringUtils.getCurrentTimeStr
import `fun`.irongate.storage.utils.StringUtils.getDoubleBarString
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class StatusScreenController : CoroutineScope {
    companion object {
        const val BAR_WIDTH = 102
    }

    override val coroutineContext = Dispatchers.IO
    private var clockJob: Job? = null

    init { init() }

    private fun init() {
        launch {
            if (!Logger.checkLogger()) {
                println("${getCurrentTimeStr()} Логгер не готов!")
                return@launch
            }

            if (!checkDisks())
                return@launch

            println("${getCurrentTimeStr()} Готово к запуску.")

            startClock()
        }

        listenConsole()
    }

    private suspend fun checkDisks(): Boolean {
        if (DiskChecker.status == DiskChecker.Status.CHECKING) {
            println("Проверка в процессе!")
            return false
        }

        println()
        println("${getCurrentTimeStr()} Проверка диска хранилища...")

        val storage = File(GlobalParams.storagePath)
        DiskChecker.checkDisk(storage)

        while (true) {
            delay(16)

            println() // todo прогресс проверки

            if (DiskChecker.status != DiskChecker.Status.CHECKING)
                break
        }

        clearString()
        if (DiskChecker.status == DiskChecker.Status.ERROR) {
            println("${getCurrentTimeStr()} Ошибка при проверке хранилища: ${DiskChecker.error}")
            return false
        }

        println("${getCurrentTimeStr()} Завершено:")
        val builder = StringBuilder(SCREEN_WIDTH)
        builder.append(getBarString(BAR_WIDTH, DiskChecker.progressOccupiedSpace))
        println(builder.toString())
        println("${StringUtils.sizeToString(DiskChecker.usableSpace)} из ${StringUtils.sizeToString(DiskChecker.totalSpace)} свободно")

        println()
        println("${getCurrentTimeStr()} Проверка диска зеркала...")

        val mirror = File(GlobalParams.mirrorPath)
        DiskChecker.checkDisk(mirror)

        while (true) {
            delay(16)

            println() // todo прогресс проверки

            if (DiskChecker.status != DiskChecker.Status.CHECKING)
                break
        }

        clearString()
        if (DiskChecker.status == DiskChecker.Status.ERROR) {
            println("${getCurrentTimeStr()} Ошибка при проверке зеркала: ${DiskChecker.error}")
            return false
        }

        println("${getCurrentTimeStr()} Завершено:")

        return true
    }

    private fun startClock() {
        if (clockJob != null) {
            println("${getCurrentTimeStr()} Таймер уже активирован!")
            return
        }

        println("${getCurrentTimeStr()} Таймер активирован.")
        clockJob = launch {
            while (true) {
                delay(60 * 1000)

                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                if (hour == 4 && minute == 0 && Copier.status == Copier.Status.READY) {
                    if (!checkDisks()) {
                        println("${getCurrentTimeStr()} Таймер выключен из-за ошибки")
                        break
                    }

                    start()
                }
            }
        }
    }

    private fun stopClock() {
        clockJob?.cancel()
        clockJob = null
        println("${getCurrentTimeStr()} Таймер остановлен!")
    }

    private fun listenConsole() {
        val keyboard = Scanner(System.`in`)
        while (true) {
            when (val input = keyboard.nextLine()) {
                "exit" -> exitProcess(0)
                "check" -> launch { checkDisks() }
                "start" -> start()
                "clear" -> clear()
                "clock" -> startClock()
                "stop" -> stopClock()
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
        if (Copier.status == Copier.Status.ERROR) {
            println("Ошибка копирования!")
            return
        }

        if (DiskChecker.status != DiskChecker.Status.READY) {
            println("Проверка дисков не пройдена!")
            return
        }

        println()
        println("${getCurrentTimeStr()} Копирование...")

        Copier.startCopy()

        while (true) {
            delay(16)

            val totalProgress = if (Copier.status == Copier.Status.COPYING) Copier.totalProgress else 100f
            val fileProgress = if (Copier.status == Copier.Status.COPYING) Copier.fileProgress else 100f

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append(getDoubleBarString(BAR_WIDTH, totalProgress, fileProgress))
            builder.append(" ${Copier.copiedFilesCount}/${Copier.copiedFilesCount + Copier.skippedFilesCount}")
            print(builder.toString())

            if (Copier.status != Copier.Status.COPYING)
                break
        }

        clearString()

        if (Copier.status == Copier.Status.ERROR) {
            println("${getCurrentTimeStr()} Ошибка при копировании!")
            stopClock()
            return
        }

        println("${getCurrentTimeStr()} Завершено:")
        println("Скопировано: ${Copier.copiedFilesCount}")
        println("Пропущено: ${Copier.skippedFilesCount}")
        println("Объемом: ${StringUtils.sizeToString(Copier.totalFilesSize)}")
    }

    private suspend fun startClear() {
        if (Copier.status == Copier.Status.ERROR) {
            println("Ошибка копирования!")
            return
        }

        if (DiskChecker.status != DiskChecker.Status.READY) {
            println("Проверка дисков не пройдена!")
            return
        }

        println()
        println("${getCurrentTimeStr()} Очистка...")

        Copier.startClear()

        while (true) {
            delay(16)

            val totalProgress = if (Copier.status == Copier.Status.CLEARING) Copier.totalProgress else 100f

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append(getBarString(BAR_WIDTH, totalProgress))
            builder.append(" ${Copier.deletedFilesCount}")
            print(builder.toString())

            if (Copier.status != Copier.Status.CLEARING)
                break
        }

        clearString()
        println("${getCurrentTimeStr()} Завершено:")
        println("Удалено: ${Copier.deletedFilesCount}")
    }

    private fun clearString() {
        print("\r${String(CharArray(SCREEN_WIDTH)).replace('\u0000', ' ')}\r")
    }

    private fun println() {
        kotlin.io.println()
        Logger.log()
    }

    private fun println(str: String) {
        kotlin.io.println(str)
        Logger.log(str)
    }
}