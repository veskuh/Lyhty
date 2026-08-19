package net.veskuh.lyhty.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DateFormatterTest {

    private val baseNow = Instant.parse("2026-08-18T21:00:00Z")
    private val utcZone = ZoneId.of("UTC")

    @Test
    fun formatsJustNowForRecentTimestamps() {
        val raw = "2026-08-18T20:59:45Z"
        val result = DateFormatter.formatRelativeTime(raw, now = baseNow, zoneId = utcZone)
        assertEquals("Just now", result)
    }

    @Test
    fun formatsMinutesAgo() {
        val raw = "2026-08-18T20:45:00Z"
        val result = DateFormatter.formatRelativeTime(raw, now = baseNow, zoneId = utcZone)
        assertEquals("15 minutes ago", result)
    }

    @Test
    fun formatsHoursAgo() {
        val raw = "2026-08-18T18:00:00Z"
        val result = DateFormatter.formatRelativeTime(raw, now = baseNow, zoneId = utcZone)
        assertEquals("3 hours ago", result)
    }

    @Test
    fun formatsDaysAgoWithinOneWeek() {
        val raw = "2026-08-16T21:00:00Z"
        val result = DateFormatter.formatRelativeTime(raw, now = baseNow, zoneId = utcZone)
        assertEquals("2 days ago", result)
    }

    @Test
    fun formatsCalendarDateForSameYearOlderThanOneWeek() {
        val raw = "2026-08-04T21:00:00Z"
        val result = DateFormatter.formatRelativeTime(raw, now = baseNow, zoneId = utcZone)
        assertEquals("Aug 4", result)
    }

    @Test
    fun formatsCalendarDateWithYearForDifferentYear() {
        val raw = "2024-08-18T21:00:00Z"
        val result = DateFormatter.formatRelativeTime(raw, now = baseNow, zoneId = utcZone)
        assertEquals("Aug 18, 2024", result)
    }

    @Test
    fun returnsOriginalStringForInvalidTimestamp() {
        val raw = "invalid-date-string"
        val result = DateFormatter.formatRelativeTime(raw, now = baseNow, zoneId = utcZone)
        assertEquals("invalid-date-string", result)
    }

    @Test
    fun handlesEpochSecondsString() {
        val epochSeconds = baseNow.minusSeconds(3600).epochSecond.toString()
        val result = DateFormatter.formatRelativeTime(epochSeconds, now = baseNow, zoneId = utcZone)
        assertEquals("1 hour ago", result)
    }
}
