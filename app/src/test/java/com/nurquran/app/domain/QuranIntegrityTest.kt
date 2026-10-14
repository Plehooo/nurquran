package com.nurquran.app.domain

import com.nurquran.app.data.Ayah
import com.nurquran.app.data.Surah
import org.junit.Assert.*
import org.junit.Test

class QuranIntegrityTest {
    private fun syntheticCatalog(): List<Surah> = QuranIntegrity.expectedAyahCounts.mapIndexed { index, count ->
        val n = index + 1
        Surah(
            number = n,
            name = "fixture-arabic-$n",
            englishName = "Fixture Surah $n",
            englishNameTranslation = "Synthetic validation fixture",
            numberOfAyahs = count,
            revelationType = "Fixture"
        )
    }

    @Test fun validatesExpectedCatalogShapeWithoutEmbeddingReligiousText() {
        val catalog = syntheticCatalog()
        assertEquals(6236, catalog.sumOf { it.numberOfAyahs })
        assertTrue(QuranIntegrity.validateCatalog(catalog))
    }

    @Test fun rejectsCatalogWithMissingSurah() {
        assertFalse(QuranIntegrity.validateCatalog(syntheticCatalog().dropLast(1)))
        assertFalse(QuranIntegrity.validateCatalog(syntheticCatalog().mapIndexed { index, s ->
            if (index == 1) s.copy(numberOfAyahs = s.numberOfAyahs - 1) else s
        }))
    }

    @Test fun alignsArabicAndTranslationBySurahAyahIndex() {
        val arabic = listOf(
            Ayah(1, "AR_FIXTURE_1", 1), Ayah(2, "AR_FIXTURE_2", 2)
        )
        val translation = listOf(
            Ayah(1, "ID_FIXTURE_1", 1), Ayah(2, "ID_FIXTURE_2", 2)
        )
        assertTrue(QuranIntegrity.alignEditions(arabic, translation))
        assertFalse(QuranIntegrity.alignEditions(arabic, translation.reversed()))
        assertTrue(QuranIntegrity.validateEdition(1, arabic, 2))
        assertFalse(QuranIntegrity.validateEdition(0, arabic, 2))
        assertFalse(QuranIntegrity.validateEdition(1, arabic, 3))
    }
}
