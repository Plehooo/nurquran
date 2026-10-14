package com.ditzy.nurquran.data

import android.content.Context
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PREFS = "nurquran_preferences"

private fun JSONObject.stringOrEmpty(key: String): String = optString(key, "").trim()

/** Data and API layer. Responses are cached locally so previously opened chapters remain readable offline. */
data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameLatin: String,
    val meaning: String,
    val ayahCount: Int,
    val revelation: String
)

data class Ayah(
    val numberInSurah: Int,
    val arabic: String,
    val translation: String,
    val globalNumber: Int = 0
)

data class AppLocation(val name: String, val latitude: Double, val longitude: Double) {
    companion object {
        val DEFAULT = AppLocation("Jakarta", -6.2088, 106.8456)
    }
}

data class PrayerTime(val name: String, val time: String)
data class PrayerAlarmEvent(val name: String, val atMillis: Long)
data class PrayerPack(val today: List<PrayerTime>, val events: List<PrayerAlarmEvent>, val dateLabel: String)
data class HadithItem(val number: Int, val arabic: String, val translation: String)
data class DailyAyah(val reference: String, val arabic: String, val translation: String)

class IslamicRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getLocation(): AppLocation = AppLocation(
        prefs.getString("location_name", AppLocation.DEFAULT.name) ?: AppLocation.DEFAULT.name,
        prefs.getString("location_lat", AppLocation.DEFAULT.latitude.toString())!!.toDoubleOrNull() ?: AppLocation.DEFAULT.latitude,
        prefs.getString("location_lon", AppLocation.DEFAULT.longitude.toString())!!.toDoubleOrNull() ?: AppLocation.DEFAULT.longitude
    )

    fun saveLocation(location: AppLocation) {
        prefs.edit()
            .putString("location_name", location.name.trim().ifBlank { "Lokasi saya" })
            .putString("location_lat", location.latitude.toString())
            .putString("location_lon", location.longitude.toString())
            .apply()
    }

    fun isAdhanEnabled(): Boolean = prefs.getBoolean("adhan_enabled", true)

    fun setAdhanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("adhan_enabled", enabled).apply()
    }

    fun getLastReadSurah(): Int = prefs.getInt("last_read_surah", 1)
    fun getLastReadAyah(): Int = prefs.getInt("last_read_ayah", 1)
    fun saveReadingProgress(surah: Int, ayah: Int) {
        prefs.edit().putInt("last_read_surah", surah).putInt("last_read_ayah", ayah).apply()
    }

    fun cachedSurahs(): List<Surah> = runCatching {
        val raw = prefs.getString("surah_catalog", null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(Surah(o.optInt("number"), o.stringOrEmpty("name"), o.stringOrEmpty("latin"),
                    o.stringOrEmpty("meaning"), o.optInt("count"), o.stringOrEmpty("revelation")))
            }
        }
    }.getOrDefault(emptyList())

    suspend fun getSurahs(): List<Surah> = withContext(Dispatchers.IO) {
        runCatching {
            val data = requestJson("https://api.alquran.cloud/v1/surah").getJSONArray("data")
            val result = buildList {
                for (i in 0 until data.length()) {
                    val o = data.getJSONObject(i)
                    add(Surah(
                        number = o.optInt("number", i + 1),
                        nameArabic = o.stringOrEmpty("name"),
                        nameLatin = o.stringOrEmpty("englishName"),
                        meaning = o.stringOrEmpty("englishNameTranslation"),
                        ayahCount = o.optInt("numberOfAyahs"),
                        revelation = o.stringOrEmpty("revelationType")
                    ))
                }
            }
            val cache = JSONArray()
            result.forEach { s -> cache.put(JSONObject().put("number", s.number).put("name", s.nameArabic)
                .put("latin", s.nameLatin).put("meaning", s.meaning).put("count", s.ayahCount).put("revelation", s.revelation)) }
            prefs.edit().putString("surah_catalog", cache.toString()).apply()
            result
        }.getOrElse { cachedSurahs() }
    }

    fun cachedAyahs(surahNumber: Int): List<Ayah> = runCatching {
        val raw = prefs.getString("surah_detail_$surahNumber", null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(Ayah(o.optInt("number"), o.stringOrEmpty("arabic"), o.stringOrEmpty("translation"), o.optInt("global")))
            }
        }
    }.getOrDefault(emptyList())

    suspend fun getAyahs(surahNumber: Int): List<Ayah> = withContext(Dispatchers.IO) {
        runCatching {
            val data = requestJson("https://api.alquran.cloud/v1/surah/$surahNumber/editions/quran-uthmani,id.indonesian")
                .getJSONArray("data")
            require(data.length() >= 2) { "Terjemahan Qur’an belum tersedia dari layanan." }
            val arabicVerses = data.getJSONObject(0).getJSONArray("ayahs")
            val translations = data.getJSONObject(1).getJSONArray("ayahs")
            val result = buildList {
                val count = minOf(arabicVerses.length(), translations.length())
                for (i in 0 until count) {
                    val ar = arabicVerses.getJSONObject(i)
                    val id = translations.getJSONObject(i)
                    add(Ayah(
                        numberInSurah = ar.optInt("numberInSurah", i + 1),
                        arabic = ar.stringOrEmpty("text"),
                        translation = cleanText(id.stringOrEmpty("text")),
                        globalNumber = ar.optInt("number", 0)
                    ))
                }
            }
            val cache = JSONArray()
            result.forEach { a -> cache.put(JSONObject().put("number", a.numberInSurah).put("global", a.globalNumber).put("arabic", a.arabic).put("translation", a.translation)) }
            prefs.edit().putString("surah_detail_$surahNumber", cache.toString()).apply()
            result
        }.getOrElse { cachedAyahs(surahNumber) }
    }

    suspend fun getDailyAyah(): DailyAyah? = withContext(Dispatchers.IO) {
        runCatching {
            val items = requestJson("https://api.alquran.cloud/v1/ayah/random/editions/quran-uthmani,id.indonesian")
                .getJSONArray("data")
            val arabic = items.getJSONObject(0)
            val translation = items.getJSONObject(1)
            val surah = arabic.optJSONObject("surah")
            DailyAyah(
                reference = "${surah?.optInt("number", 1) ?: 1}:${arabic.optInt("numberInSurah", 1)}",
                arabic = arabic.stringOrEmpty("text"),
                translation = cleanText(translation.stringOrEmpty("text"))
            ).also { prefs.edit().putString("daily_ayah", JSONObject().put("ref", it.reference).put("ar", it.arabic).put("id", it.translation).toString()).apply() }
        }.getOrElse {
            runCatching {
                val o = JSONObject(prefs.getString("daily_ayah", "{}") ?: "{}")
                DailyAyah(o.stringOrEmpty("ref"), o.stringOrEmpty("ar"), o.stringOrEmpty("id"))
                    .takeIf { it.arabic.isNotBlank() }
            }.getOrNull()
        }
    }

    suspend fun getHadiths(book: String, start: Int, end: Int): List<HadithItem> = withContext(Dispatchers.IO) {
        val cacheKey = "hadith_${book}_$start"
        runCatching {
            val root = requestJson("https://api.hadith.gading.dev/books/$book?range=$start-$end")
            val data = root.optJSONObject("data") ?: root
            val entries = data.optJSONArray("hadiths") ?: data.optJSONArray("hadith") ?: JSONArray()
            val result = buildList {
                for (i in 0 until entries.length()) {
                    val o = entries.optJSONObject(i) ?: continue
                    val arabic = o.optString("arab", o.optString("arabic", ""))
                    val translation = cleanText(o.optString("id", o.optString("translation", "")))
                    if (arabic.isNotBlank() || translation.isNotBlank()) {
                        add(HadithItem(o.optInt("number", start + i), arabic, translation))
                    }
                }
            }
            if (result.isNotEmpty()) {
                val array = JSONArray()
                result.forEach { array.put(JSONObject().put("number", it.number).put("ar", it.arabic).put("id", it.translation)) }
                prefs.edit().putString(cacheKey, array.toString()).apply()
            }
            result
        }.getOrElse {
            runCatching {
                val array = JSONArray(prefs.getString(cacheKey, "[]") ?: "[]")
                buildList {
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        add(HadithItem(o.optInt("number"), o.stringOrEmpty("ar"), o.stringOrEmpty("id")))
                    }
                }
            }.getOrDefault(emptyList())
        }
    }

    fun cachedTodayPrayerTimes(location: AppLocation): List<PrayerTime> {
        val key = prayerCacheKey(location, todayKey())
        return parsePrayerTimes(prefs.getString(key, null))
    }

    fun cachedPrayerEvents(): List<PrayerAlarmEvent> = runCatching {
        val array = JSONArray(prefs.getString("prayer_events_cache", "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(PrayerAlarmEvent(o.stringOrEmpty("name"), o.optLong("at")))
            }
        }
    }.getOrDefault(emptyList())

    suspend fun getPrayerPack(location: AppLocation): PrayerPack = withContext(Dispatchers.IO) {
        val todayCal = Calendar.getInstance()
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val todayKey = dateKey(todayCal.time)
        val allEvents = mutableListOf<PrayerAlarmEvent>()
        var todayTimes = emptyList<PrayerTime>()
        var success = false

        for ((index, cal) in listOf(todayCal, tomorrowCal).withIndex()) {
            val date = cal.time
            val cacheKey = prayerCacheKey(location, dateKey(date))
            val timings = runCatching {
                val datePath = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(date)
                val lat = String.format(Locale.US, "%.5f", location.latitude)
                val lon = String.format(Locale.US, "%.5f", location.longitude)
                val url = "https://api.aladhan.com/v1/timings/$datePath?latitude=$lat&longitude=$lon&method=20"
                requestJson(url).getJSONObject("data").getJSONObject("timings")
            }.getOrNull()

            val times = if (timings != null) {
                val parsed = parseApiTimes(timings)
                if (parsed.isNotEmpty()) {
                    val array = JSONArray()
                    parsed.forEach { array.put(JSONObject().put("name", it.name).put("time", it.time)) }
                    prefs.edit().putString(cacheKey, array.toString()).apply()
                    success = true
                }
                parsed
            } else parsePrayerTimes(prefs.getString(cacheKey, null))

            if (index == 0) todayTimes = times
            times.forEach { prayer ->
                val trigger = parseLocalDateTime(date, prayer.time) ?: return@forEach
                if (trigger > System.currentTimeMillis() + 10_000L) allEvents += PrayerAlarmEvent(prayer.name, trigger)
            }
        }

        val events = allEvents.sortedBy { it.atMillis }
        if (events.isNotEmpty()) {
            val array = JSONArray()
            events.forEach { array.put(JSONObject().put("name", it.name).put("at", it.atMillis)) }
            prefs.edit().putString("prayer_events_cache", array.toString()).apply()
        }
        if (todayTimes.isEmpty()) todayTimes = cachedTodayPrayerTimes(location)
        val pack = PrayerPack(todayTimes, events.ifEmpty { cachedPrayerEvents().filter { it.atMillis > System.currentTimeMillis() } }, todayKey)
        // `success` is retained for diagnostics/debugging and future offline banners.
        @Suppress("UNUSED_VARIABLE") val fetchedOnline = success
        pack
    }

    suspend fun downloadAdhanIfNeeded() = withContext(Dispatchers.IO) {
        val target = File(appContext.filesDir, "adhan.ogg")
        if (target.exists() && target.length() > 20_000) return@withContext
        runCatching {
            val connection = (URL("https://upload.wikimedia.org/wikipedia/commons/b/b0/Beautiful_adhan.ogg").openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "NurQuran-Android/1.0 (personal prayer application)")
            }
            connection.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            connection.disconnect()
            if (target.length() < 20_000) target.delete()
        }
        Unit
    }

    private fun prayerCacheKey(location: AppLocation, date: String) =
        "prayer_${date}_${String.format(Locale.US, "%.2f", location.latitude)}_${String.format(Locale.US, "%.2f", location.longitude)}"

    private fun parsePrayerTimes(raw: String?): List<PrayerTime> = runCatching {
        val arr = JSONArray(raw ?: "[]")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(PrayerTime(o.stringOrEmpty("name"), o.stringOrEmpty("time")))
            }
        }
    }.getOrDefault(emptyList())

    private fun parseApiTimes(times: JSONObject): List<PrayerTime> {
        val mapping = listOf("Fajr" to "Subuh", "Dhuhr" to "Zuhur", "Asr" to "Asar", "Maghrib" to "Maghrib", "Isha" to "Isya")
        return mapping.mapNotNull { (apiName, displayName) ->
            val raw = times.optString(apiName, "")
            val match = Regex("(\\d{1,2}:\\d{2})").find(raw)?.value ?: return@mapNotNull null
            PrayerTime(displayName, match.padStart(5, '0'))
        }
    }

    private fun parseLocalDateTime(date: Date, time: String): Long? = runCatching {
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("$formattedDate $time")!!.time
    }.getOrNull()

    private fun todayKey() = dateKey(Date())
    private fun dateKey(date: Date) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    private fun cleanText(raw: String): String = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim()

    private fun requestJson(address: String): JSONObject {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 18_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "NurQuran-Android/1.0")
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("Layanan mengembalikan HTTP $status")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
