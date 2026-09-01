package net.veskuh.lyhty.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel(val priority: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6);

    companion object {
        fun fromName(name: String): LogLevel {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: INFO
        }
    }
}

object LyhtyLogger {

    private const val LOG_FILE_NAME = "lyhty_app.log"
    private const val MAX_LOG_SIZE_BYTES = 512 * 1024L // 512 KB max log file size

    private var currentLogLevel: LogLevel = LogLevel.DEBUG
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context, initialLevel: LogLevel = LogLevel.DEBUG) {
        currentLogLevel = initialLevel
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        logFile = File(logDir, LOG_FILE_NAME)
    }

    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
        info("LyhtyLogger", "Log level updated to ${level.name}")
    }

    fun getLogLevel(): LogLevel = currentLogLevel

    fun v(tag: String, message: String) = log(LogLevel.VERBOSE, tag, message)
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    fun verbose(tag: String, message: String) = v(tag, message)
    fun debug(tag: String, message: String) = d(tag, message)
    fun info(tag: String, message: String) = i(tag, message)
    fun warn(tag: String, message: String, throwable: Throwable? = null) = w(tag, message, throwable)
    fun error(tag: String, message: String, throwable: Throwable? = null) = e(tag, message, throwable)

    @Synchronized
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (level.priority < currentLogLevel.priority) return

        val timestamp = dateFormat.format(Date())
        val stackTrace = throwable?.let {
            try {
                Log.getStackTraceString(it)
            } catch (_: Throwable) {
                it.stackTraceToString()
            }
        }
        val logText = "[$timestamp] [${level.name}] [$tag] $message${if (stackTrace != null) "\n$stackTrace" else ""}"

        // Log to Android Logcat
        try {
            when (level) {
                LogLevel.VERBOSE -> Log.v(tag, message, throwable)
                LogLevel.DEBUG -> Log.d(tag, message, throwable)
                LogLevel.INFO -> Log.i(tag, message, throwable)
                LogLevel.WARN -> Log.w(tag, message, throwable)
                LogLevel.ERROR -> Log.e(tag, message, throwable)
            }
        } catch (_: Exception) {
            // Un-mocked Logcat fallback for unit tests
        }

        // Persist to File
        writeLogToFile(logText)
    }

    private fun writeLogToFile(logText: String) {
        val file = logFile ?: return
        try {
            if (file.exists() && file.length() > MAX_LOG_SIZE_BYTES) {
                file.writeText("[${dateFormat.format(Date())}] --- Log File Truncated ---\n")
            }
            FileWriter(file, true).use { writer ->
                writer.appendLine(logText)
            }
        } catch (_: Exception) {
            // Silently ignore file write failures
        }
    }

    fun getLogFile(): File? = logFile

    @Synchronized
    fun readLogContent(): String {
        return logFile?.takeIf { it.exists() }?.readText() ?: "No log file entries found."
    }

    @Synchronized
    fun readRecentLogs(maxLines: Int = 100): String {
        val file = logFile ?: return "No log file available."
        if (!file.exists()) return "No diagnostic log entries recorded yet."
        return try {
            val lines = file.readLines()
            if (lines.isEmpty()) {
                "No diagnostic log entries recorded yet."
            } else {
                lines.takeLast(maxLines).joinToString("\n")
            }
        } catch (_: Exception) {
            "Unable to read diagnostic log file."
        }
    }

    @Synchronized
    fun clearLogs() {
        logFile?.let {
            if (it.exists()) it.writeText("")
        }
    }
}
