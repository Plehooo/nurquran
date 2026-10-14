package com.nurquran.app.domain

import com.nurquran.app.data.PrayerTime
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrayerTimeLogicTest {
    @Test fun acceptsAlAdhanTimeWithTimezoneSuffix() {
        assertEquals(LocalTime.of(4, 15), PrayerTimeLogic.parseTime("04:15 (WIB)"))
        assertEquals("04:07", PrayerTimeLogic.normalizedTime("4:07 (WIB)"))
    }

    @Test fun invalidTimesAreRejected() {
        assertNull(PrayerTimeLogic.parseTime("25:70"))
        assertNull(PrayerTimeLogic.parseTime("not a time"))
    }

    @Test fun nextPrayerChoosesFirstFutureTime() {
        val times = listOf(
            PrayerTime("Sunrise", "Terbit", "05:45"),
            PrayerTime("Dhuhr", "Zuhur", "12:00"),
            PrayerTime("Fajr", "Subuh", "04:30"),
            PrayerTime("Asr", "Asar", "15:15")
        )
        assertEquals("Asr", PrayerTimeLogic.nextPrayer(times, LocalDateTime.of(2026, 1, 1, 13, 0))?.key)
        assertEquals("Dhuhr", PrayerTimeLogic.nextPrayer(times, LocalDateTime.of(2026, 1, 1, 5, 0))?.key)
    }

    @Test fun validatesTheFiveRequiredPrayers() {
        val valid = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").mapIndexed { i, key ->
            PrayerTime(key, key, listOf("04:30", "12:00", "15:15", "18:00", "19:10")[i])
        }
        assertTrue(PrayerTimeLogic.isValidPrayerTimes(valid))
        assertFalse(PrayerTimeLogic.isValidPrayerTimes(valid.dropLast(1)))
    }

    @Test fun datePathUsesAlAdhanFormat() {
        assertEquals("14-10-2026", PrayerTimeLogic.datePath(LocalDate.of(2026, 10, 14)))
    }
}
