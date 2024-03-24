package `fun`.irongate.storage.controllers

import `fun`.irongate.storage.GlobalParams
import `fun`.irongate.storage.GlobalParams.SCREEN_WIDTH
import `fun`.irongate.storage.model.Cleaner
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
            checkLogger()
            checkDisks()
            startClock()
        }

        listenConsole()
    }

    private fun checkLogger() {
        if (!logger.checkLogger()) {
            status = Status.ERROR
            println("Логгер не готов!")
            return
        }

        status = Status.OK
        return
    }

    private suspend fun checkDisks() {
        if (status != Status.OK) {
            logger.logln("Нельзя запустить проверку дисков! Статус:$status")
            return
        }
        status = Status.CHECKING

        checkStorage()

        if (status == Status.ERROR)
            return

        checkMirror()

        if (status == Status.ERROR)
            return

        status = Status.OK
        return
    }

    private suspend fun checkStorage() {
        logger.logln("Проверка диска хранилища (${GlobalParams.storagePath})...")

        val storage = File(GlobalParams.storagePath)
        val mirror = File(GlobalParams.mirrorPath)
        val checker = DiskChecker(storage, mirror)
        checker.checkDisk()

        while (true) {
            delay(16)

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append(getDoubleBarString(BAR_WIDTH, checker.totalProgress, checker.fileProgress))
            builder.append(" ${checker.checkedFilesCount}/${checker.totalFilesCount}")
            print(builder.toString())

            if (checker.status != DiskChecker.Status.CHECKING)
                break
        }

        clearString()

        if (checker.status == DiskChecker.Status.ERROR) {
            status = Status.ERROR
            logger.logln("Ошибка при проверке хранилища: ${checker.error}")
            return
        }

        val builder = StringBuilder()
        builder.append("Завершено:\n")
        builder.append("Проверено: ${checker.totalFilesCount}\n")
        builder.append("Отличаются: ${checker.checkedFilesCount}\n")
        builder.append("${getBarString(BAR_WIDTH, checker.progressOccupiedSpace)}\n")
        builder.append("${sizeToString(checker.usableSpace)} из ${sizeToString(checker.totalSpace)} свободно\n")
        logger.logln(builder.toString())

        return
    }

    private suspend fun checkMirror() {
        logger.logln("Проверка диска зеркала (${GlobalParams.mirrorPath})...")

        val storage = File(GlobalParams.storagePath)
        val mirror = File(GlobalParams.mirrorPath)
        val checker = DiskChecker(mirror, storage)
        checker.checkDisk()

        while (true) {
            delay(16)

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append(getDoubleBarString(BAR_WIDTH, checker.totalProgress, checker.fileProgress))
            builder.append(" ${checker.checkedFilesCount}/${checker.totalFilesCount}")
            print(builder.toString())

            if (checker.status != DiskChecker.Status.CHECKING)
                break
        }

        clearString()

        if (checker.status == DiskChecker.Status.ERROR) {
            status = Status.ERROR
            logger.logln("Ошибка при проверке зеркала: ${checker.error}")
            return
        }

        val builder = StringBuilder()
        builder.append("Завершено:\n")
        builder.append("Проверено: ${checker.totalFilesCount}\n")
        builder.append("Отличаются: ${checker.checkedFilesCount}\n")
        builder.append("${getBarString(BAR_WIDTH, checker.progressOccupiedSpace)}\n")
        builder.append("${sizeToString(checker.usableSpace)} из ${sizeToString(checker.totalSpace)} свободно\n")
        logger.logln(builder.toString())

        return
    }

    private suspend fun copy() {
        if (status != Status.OK) {
            logger.logln("Нельзя запустить копирование! Статус:$status")
            return
        }
        status = Status.COPYING
        logger.logln("Копирование...")

        val copier = Copier()
        copier.startCopy()

        while (true) {
            delay(16)

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append(getDoubleBarString(BAR_WIDTH, copier.totalProgress, copier.fileProgress))
            builder.append(" ${copier.copiedFilesCount}/${copier.totalFilesCount}")
            print(builder.toString())

            if (copier.status != Copier.Status.COPYING)
                break
        }

        clearString()

        if (copier.status == Copier.Status.ERROR) {
            status = Status.ERROR
            logger.logln("Ошибка при копировании: ${copier.error}")
            return
        }

        val builder = StringBuilder()
        builder.append("Завершено:\n")
        builder.append("Скопировано: ${copier.copiedFilesCount}\n")
        builder.append("Объемом: ${sizeToString(copier.copiedFilesSize)}\n")
        builder.append("Пропущено: ${copier.totalFilesCount}\n")
        logger.logln(builder.toString())

        status = Status.OK
    }

    private suspend fun clear() {
        if (status != Status.OK) {
            logger.logln("Нельзя запустить очистку! Статус:$status")
            return
        }
        status = Status.CLEARING
        logger.logln("Очистка...")

        val cleaner = Cleaner(logger)
        cleaner.startClear()

        while (true) {
            delay(16)

            val builder = StringBuilder(SCREEN_WIDTH)
            builder.append('\r')
            builder.append(getBarString(BAR_WIDTH, cleaner.totalProgress))
            builder.append(" ${cleaner.deletedFilesCount}/${cleaner.totalFilesCount}")
            print(builder.toString())

            if (cleaner.status != Cleaner.Status.CLEARING)
                break
        }

        clearString()

        if (cleaner.status == Cleaner.Status.ERROR) {
            status = Status.ERROR
            logger.logln("Ошибка при очистке! ${cleaner.error}")
            return
        }

        val builder = StringBuilder()
        builder.append("Завершено:\n")
        builder.append("Удалено: ${cleaner.deletedFilesCount}\n")
        logger.logln(builder.toString())

        status = Status.OK
    }

    private fun startClock() {
        if (status == Status.ERROR) {
            logger.logln("Нельзя запустить таймер из-за ошибки!")
            return
        }

        if (clockJob != null) {
            logger.logln("Таймер уже активирован!")
            return
        }

        logger.logln("Таймер активирован.")
        clockJob = launch {
            while (true) {
                delay(60 * 1000)

                if (status == Status.ERROR) {
                    logger.logln("Таймер выключен из-за ошибки!")
                    break
                }

                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                if (hour == 4 && minute == 0 && status == Status.OK) {
                    if (status != Status.OK) {
                        logger.logln("Нельзя запустить по таймеру! Статус:$status")

                    }

                    checkDisks()
                    copy()
                    clear()
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
            when (val input = keyboard.nextLine()) {
                "exit" -> exitProcess(0)
                "check" -> launch { checkDisks() }
                "copy" -> launch { copy() }
                "clear" -> launch { clear() }
                "clock" -> startClock()
                "stop" -> stopClock()
                else -> logger.logln("Неизвестная команда: $input")
            }
        }
    }

    private fun clearString() {
        print("\r${String(CharArray(SCREEN_WIDTH)).replace('\u0000', ' ')}\r")
    }

    enum class Status { OK, CHECKING, COPYING, CLEARING, ERROR }
}