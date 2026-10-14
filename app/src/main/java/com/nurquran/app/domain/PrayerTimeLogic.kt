package com.nurquran.app.domain

import com.nurquran.app.data.PrayerTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object PrayerTimeLogic {
    private val inputFormat = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT)
    private val outputFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun parseTime(raw: String): LocalTime? {
        val match = Regex("(?<!\\d)(\\d{1,2}:\\d{2})(?!\\d)").find(raw) ?: return null
        return runCatching { LocalTime.parse(match.value, inputFormat) }.getOrNull()
    }

    fun normalizedTime(raw: String): String? = parseTime(raw)?.format(outputFormat)

    fun nextPrayer(times: List<PrayerTime>, now: LocalDateTime = LocalDateTime.now()): PrayerTime? {
        val prayerKeys = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        val valid = times.filter { it.key in prayerKeys }
            .mapNotNull { p -> parseTime(p.time)?.let { Triple(p, it, LocalDateTime.of(now.toLocalDate(), it)) } }
            .sortedBy { it.third }
        return valid.firstOrNull { it.third.isAfter(now) }?.first
    }

    fun upcomingEvents(times: List<PrayerTime>, now: LocalDateTime): List<Pair<PrayerTime, LocalDateTime>> =
        times.mapNotNull { p ->
            parseTime(p.time)?.let { t -> LocalDateTime.of(now.toLocalDate(), t).let { dt -> if (dt.isAfter(now)) p to dt else null } }
        }.sortedBy { it.second }

    fun isValidPrayerTimes(times: List<PrayerTime>): Boolean {
        val required = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        return times.filter { it.key in required }.map { it.key }.toSet().containsAll(required) &&
            times.filter { it.key in required }.all { parseTime(it.time) != null }
    }

    fun datePath(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
}
