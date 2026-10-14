package com.nurquran.app.domain

import com.nurquran.app.data.Ayah
import com.nurquran.app.data.Surah

object QuranIntegrity {
    /** Canonical 114-surah ayah counts used as a structural check (not a verse-text checksum). */
    val expectedAyahCounts: List<Int> = listOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )

    /** Structural validation only; verse content itself comes from the selected API editions. */
    fun validateCatalog(surahs: List<Surah>): Boolean =
        surahs.size == 114 && surahs.map { it.number } == (1..114).toList() &&
            surahs.all { it.name.isNotBlank() && it.englishName.isNotBlank() } &&
            surahs.map { it.numberOfAyahs } == expectedAyahCounts &&
            surahs.sumOf { it.numberOfAyahs } == 6236

    fun validateEdition(number: Int, contentAyahs: List<Ayah>, expectedCount: Int): Boolean =
        number in 1..114 && expectedCount > 0 && contentAyahs.isNotEmpty() && contentAyahs.size == expectedCount &&
            contentAyahs.map { it.numberInSurah } == (1..expectedCount).toList() &&
            contentAyahs.all { it.number > 0 && it.text.isNotBlank() }

    fun alignEditions(arabic: List<Ayah>, translation: List<Ayah>): Boolean =
        arabic.isNotEmpty() && arabic.size == translation.size && arabic.zip(translation).all { (a, t) ->
            a.numberInSurah == t.numberInSurah && a.numberInSurah > 0 && a.text.isNotBlank() && t.text.isNotBlank()
        }
}
