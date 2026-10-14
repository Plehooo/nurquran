package com.nurquran.app.data

import com.google.gson.annotations.SerializedName

/** Models mirror the documented public API fields; unknown fields are ignored by Gson. */
data class QuranEnvelope<T>(val code: Int? = null, val status: String? = null, val data: T? = null)

data class Surah(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String
)

data class SurahContent(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String,
    val ayahs: List<Ayah>
)

data class Ayah(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val juz: Int? = null,
    val page: Int? = null,
    val sajda: Any? = null
)

data class AyahPair(val numberInSurah: Int, val globalNumber: Int, val arabic: String, val translation: String)

data class SurahReading(val metadata: Surah, val ayahs: List<AyahPair>)

data class HadithBook(val name: String, val slug: String, val total: Int)

data class HadithPagination(
    val totalItems: Int = 0,
    val currentPage: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int? = null,
    val pages: List<Int> = emptyList()
)

data class HadithPage(
    val name: String = "",
    val slug: String = "",
    val total: Int = 0,
    val pagination: HadithPagination? = null,
    val items: List<HadithItem> = emptyList()
)

data class HadithItem(
    @SerializedName(value = "number", alternate = ["no", "nomor"]) val number: Int? = null,
    @SerializedName(value = "arab", alternate = ["arabic", "text_ar", "textArabic"]) val arabic: String? = null,
    @SerializedName(value = "id", alternate = ["indo", "indonesia", "translation", "text_id"]) val translation: String? = null,
    @SerializedName(value = "name", alternate = ["book"]) val book: String? = null
)

data class AlAdhanEnvelope(
    val code: Int? = null,
    val status: String? = null,
    val data: PrayerData? = null
)

data class PrayerData(
    val timings: Map<String, String> = emptyMap(),
    val date: AlAdhanDate? = null,
    val meta: PrayerMeta? = null
)

data class AlAdhanDate(val readable: String? = null, val timestamp: String? = null, val hijri: HijriDate? = null)
data class HijriDate(val date: String? = null, val month: HijriMonth? = null, val year: String? = null)
data class HijriMonth(val en: String? = null, val ar: String? = null)
data class PrayerMeta(val timezone: String? = null, val method: PrayerMethod? = null)
data class PrayerMethod(val id: Int? = null, val name: String? = null)

data class PrayerTime(val key: String, val label: String, val time: String)
data class PrayerTimetable(
    val dateKey: String,
    val displayDate: String,
    val hijriDate: String?,
    val cityLabel: String,
    val latitude: Double,
    val longitude: Double,
    val methodId: Int,
    val methodName: String?,
    val times: List<PrayerTime>
)

data class CachedResult<T>(val value: T, val isFromCache: Boolean, val notice: String? = null)
