package net.veskuh.lyhty

import android.app.Application
import net.veskuh.lyhty.data.repository.MinifluxConfigRepository
import net.veskuh.lyhty.util.LyhtyLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LyhtyApplication : Application() {

    @Inject
    lateinit var configRepository: MinifluxConfigRepository

    override fun onCreate() {
        super.onCreate()
        val initialLogLevel = configRepository.getLogLevelSync()
        LyhtyLogger.init(this, initialLogLevel)
        LyhtyLogger.info("LyhtyApplication", "Lyhty Application started with LogLevel=${initialLogLevel.name}")

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            LyhtyLogger.error("LyhtyApplication", "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
