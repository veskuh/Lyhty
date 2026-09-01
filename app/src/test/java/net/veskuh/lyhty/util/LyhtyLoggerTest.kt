package net.veskuh.lyhty.util

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyhtyLoggerTest {

    @Before
    fun setUp() {
        LyhtyLogger.init(ApplicationProvider.getApplicationContext(), LogLevel.VERBOSE)
        LyhtyLogger.clearLogs()
    }

    @Test
    fun `LyhtyLogger writes verbose and info logs to persistent log file`() {
        LyhtyLogger.setLogLevel(LogLevel.VERBOSE)
        LyhtyLogger.verbose("TestTag", "Verbose diagnostic message")
        LyhtyLogger.info("TestTag", "Info message")

        val content = LyhtyLogger.readLogContent()
        assertTrue(content.contains("Verbose diagnostic message"))
        assertTrue(content.contains("Info message"))
    }

    @Test
    fun `LyhtyLogger filters out logs below current LogLevel threshold`() {
        LyhtyLogger.setLogLevel(LogLevel.WARN)
        LyhtyLogger.debug("TestTag", "This debug should be filtered out")
        LyhtyLogger.warn("TestTag", "This warn should be recorded")

        val content = LyhtyLogger.readLogContent()
        assertTrue(!content.contains("This debug should be filtered out"))
        assertTrue(content.contains("This warn should be recorded"))
    }

    @Test
    fun `LogLevel fromName resolves string levels correctly`() {
        assertEquals(LogLevel.VERBOSE, LogLevel.fromName("VERBOSE"))
        assertEquals(LogLevel.ERROR, LogLevel.fromName("error"))
        assertEquals(LogLevel.INFO, LogLevel.fromName("nonexistent"))
    }

    @Test
    fun `readRecentLogs returns last N lines correctly`() {
        LyhtyLogger.setLogLevel(LogLevel.VERBOSE)
        for (i in 1..10) {
            LyhtyLogger.info("TestTag", "Line $i")
        }

        val recent5 = LyhtyLogger.readRecentLogs(5)
        val lines = recent5.lines()
        assertEquals(5, lines.size)
        assertTrue(lines.last().contains("Line 10"))
        assertTrue(lines.first().contains("Line 6"))
    }

    @Test
    fun `clearLogs empties the log file and returns fallback message`() {
        LyhtyLogger.info("TestTag", "Message to clear")
        assertTrue(LyhtyLogger.readRecentLogs(10).contains("Message to clear"))

        LyhtyLogger.clearLogs()
        assertEquals("No diagnostic log entries recorded yet.", LyhtyLogger.readRecentLogs(10))
    }
}
