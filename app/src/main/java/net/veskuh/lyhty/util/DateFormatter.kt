package net.veskuh.lyhty.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateFormatter {

    private val sameYearFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val differentYearFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    /**
     * Formats an ISO-8601 or epoch timestamp string into a human-readable relative time string.
     * Timestamps <= 7 days old show relative time (e.g. "3 hours ago", "5 days ago").
     * Timestamps > 7 days old show clean dates (e.g. "Aug 8" or "Aug 18, 2025").
     * Uses the local device timezone.
     */
    fun formatRelativeTime(
        rawTimestamp: String,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        if (rawTimestamp.isBlank()) return ""

        val publishedInstant = parseToInstant(rawTimestamp) ?: return rawTimestamp

        val publishedZoned = publishedInstant.atZone(zoneId)
        val nowZoned = now.atZone(zoneId)

        val secondsAgo = ChronoUnit.SECONDS.between(publishedZoned, nowZoned)
        if (secondsAgo < 0) {
            return "Just now"
        }

        val minutesAgo = ChronoUnit.MINUTES.between(publishedZoned, nowZoned)
        val hoursAgo = ChronoUnit.HOURS.between(publishedZoned, nowZoned)
        val daysAgo = ChronoUnit.DAYS.between(publishedZoned, nowZoned)

        return when {
            minutesAgo < 1 -> "Just now"
            minutesAgo < 60 -> if (minutesAgo == 1L) "1 minute ago" else "$minutesAgo minutes ago"
            hoursAgo < 24 -> if (hoursAgo == 1L) "1 hour ago" else "$hoursAgo hours ago"
            daysAgo < 7 -> if (daysAgo == 1L) "1 day ago" else "$daysAgo days ago"
            else -> {
                if (publishedZoned.year == nowZoned.year) {
                    publishedZoned.format(sameYearFormatter)
                } else {
                    publishedZoned.format(differentYearFormatter)
                }
            }
        }
    }

    private fun parseToInstant(raw: String): Instant? {
        val trimmed = raw.trim()

        // 1. Try parsing numeric Epoch Seconds / Epoch Millis
        trimmed.toLongOrNull()?.let { num ->
            return if (num > 10_000_000_000L) {
                Instant.ofEpochMilli(num)
            } else {
                Instant.ofEpochSecond(num)
            }
        }

        // 2. Try parsing ISO Instant (e.g., 2026-08-18T18:45:00Z)
        try {
            return Instant.parse(trimmed)
        } catch (_: Throwable) {}

        // 3. Try parsing ZonedDateTime / OffsetDateTime
        try {
            return ZonedDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME).toInstant()
        } catch (_: Throwable) {}

        return null
    }
}
