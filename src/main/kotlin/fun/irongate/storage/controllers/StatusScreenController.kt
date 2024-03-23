package `fun`.irongate.storage.controllers

import `fun`.irongate.storage.GlobalParams
import `fun`.irongate.storage.GlobalParams.SCREEN_WIDTH
import `fun`.irongate.storage.model.Copier
import `fun`.irongate.storage.model.DiskChecker
import `fun`.irongate.storage.model.Logger
import `fun`.irongate.storage.utils.StringUtils.getBarString
import `fun`.irongate.storage.utils.StringUtils.getDoubleBarString
import `fun`.irongate.storage.utils.StringUtils.sizeToString
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class StatusScreenController : CoroutineScope {
    companion object {
        const val BAR_WIDTH = 102
    }

    override val coroutineContext = Dispatchers.IO
    private val logger = Logger()
    private var clockJob: Job? = null
    private var status: Status? = null

    init { init() }

    private fun init() {
        launch {
            if (!logger.checkLogger()) {
                println("Логгер не готов!")
                status = Status.ERROR
                return@launch
            }

            status = Status.OK

            if (!checkDisks()) {
                status = Status.ERROR
                return@launch
            }

            startClock()
        }

        listenConsole()
    }

    private suspend fun checkDisks(): Boolean {
        if (status != Status.OK) {
            logger.logln("Нельзя запустить проверку дисков!")
            return false
        }
        status = Status.CHECKING

        if (!checkStorage()) {
            status = Status.ERROR
            return false
        }

        if (!checkMirror()) {
            status = Status.ERROR
            return false
        }

        status = Status.OK

        return true
    }

    private suspend fun checkStorage(): Boolean {
        logger.logln("Проверка диска хранилища (${GlobalParams.storagePath})...")

        val storage = File(GlobalParams.storagePath)

        val checker = DiskChecker(storage)
        checker.checkDisk()

        while (true) {
            delay(16)

            if (checker.status != DiskChecker.Status.CHECKING)
                break
        }

        if (checker.status == DiskChecker.Status.ERROR) {
            status = Status.ERROR
            logger.logln("Ошибка при проверке хранилища: ${checker.error}")
            return false
        }

        val builder = StringBuilder()
        builder.append("Завершено:\n")
        builder.append("${getBarString(BAR_WIDTH, checker.progressOccupiedSpace)}\n")
        builder.append("${sizeToString(checker.usableSpace)} из ${sizeToString(checker.totalSpace)} свободно\n")
        logger.logln(builder.toString())

        return true
    }

    private suspend fun checkMirror(): Boolean {
        logger.logln("Проверка диска зеркала (${GlobalParams.mirrorPath})...")

        val mirror = File(GlobalParams.mirrorPath)
        val checker = DiskChecker(mirror)
        checker.checkDisk()

        while (true) {
            delay(16)

            if (checker.status != DiskChecker.Status.CHECKING)
                break
        }

        if (checker.status == DiskChecker.Status.ERROR) {
            status = Status.ERROR
            logger.logln("Ошибка при проверке зеркала: ${checker.error}")
            return false
        }

        val builder = StringBuilder()
        builder.append("Завершено:\n")
        builder.append("${getBarString(BAR_WIDTH, checker.progressOccupiedSpace)}\n")
        builder.append("${sizeToString(checker.usableSpace)} из ${sizeToString(checker.totalSpace)} свободно\n")
        logger.logln(builder.toString())

        return true
    }

    private fun startClock() {
        if (clockJob != null) {
            logger.logln("Таймер уже активирован!")
            return
        }

        logger.logln("Таймер активирован.")
        clockJob = launch {
            while (true) {
                delay(60 * 1000)

                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                if (hour == 4 && minute == 0 && Copier.status == Copier.Status.READY) {
                    if (!checkDisks()) {
                        logger.logln("Таймер выключен из-за ошибки")
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
        logger.logln("Таймер остановлен!")
    }

    private fun listenConsole() {
        val keyboard = Scanner(System.`in`)
        while (true) {
            when (val input = keyboard.nextLine()) {    // todo проверка статуса
                "exit" -> exitProcess(0)
                "check" -> launch { checkDisks() }
                "start" -> start()
                "clear" -> clear()
                "clock" -> startClock()
                "stop" -> stopClock()
                else -> logger.logln("Unknown command: $input")
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
            logger.logln("Ошибка копирования!")
            return
        }

        if (status != Status.OK) {
            logger.logln("Проверка дисков не пройдена!")
            return
        }

        logger.logln()
        logger.logln("Копирование...")

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
            logger.logln("Ошибка при копировании!")
            stopClock()
            return
        }

        logger.logln("Завершено:")
        logger.logln("Скопировано: ${Copier.copiedFilesCount}")
        logger.logln("Пропущено: ${Copier.skippedFilesCount}")
        logger.logln("Объемом: ${sizeToString(Copier.totalFilesSize)}")
    }

    private suspend fun startClear() {
        if (Copier.status == Copier.Status.ERROR) {
            logger.logln("Ошибка копирования!")
            return
        }

        if (status != Status.OK) {
            logger.logln("Проверка дисков не пройдена!")
            return
        }

        logger.logln()
        logger.logln("Очистка...")

        Copier.startClear()

        while (true) {
            delay(16)

            val totalProgress = if (Copier.status == Copier.Status.CLEARING) Copier.totalProgress else 100f

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append(getBarString(BAR_WIDTH, totalProgress))
            builder.append(" ${Copier.deletedFilesCount}/${Copier.totalFilesCount}")
            print(builder.toString())

            if (Copier.status != Copier.Status.CLEARING)
                break
        }

        clearString()
        logger.logln("Завершено:")
        logger.logln("Удалено: ${Copier.deletedFilesCount}")
    }

    private fun clearString() {
        print("\r${String(CharArray(SCREEN_WIDTH)).replace('\u0000', ' ')}\r")
    }

    enum class Status { OK, CHECKING, ERROR }
}