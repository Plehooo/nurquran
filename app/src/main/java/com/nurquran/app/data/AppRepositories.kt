package com.nurquran.app.data

import kotlinx.coroutines.CancellationException
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.nurquran.app.domain.PrayerTimeLogic
import com.nurquran.app.domain.QuranIntegrity
import com.nurquran.app.storage.JsonDiskCache
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.util.Locale

class QuranRepository(private val api: QuranCloudService, private val cache: JsonDiskCache) {
    suspend fun getSurahs(): CachedResult<List<Surah>> {
        val cached = cache.read<List<Surah>>("quran_surah_catalog")
        return try {
            val data = api.getSurahs().data ?: error("API katalog tidak memiliki data")
            require(validateCatalog(data)) { "Katalog Qur'an tidak lolos pemeriksaan integritas" }
            cache.write("quran_surah_catalog", data, valid = true)
            CachedResult(data, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached != null && validateCatalog(cached)) CachedResult(cached, true, "Koneksi gagal; katalog terakhir yang tersimpan ditampilkan.")
            else throw e
        }
    }

    suspend fun getReading(number: Int): CachedResult<SurahReading> = coroutineScope {
        require(number in 1..114) { "Nomor surah harus 1–114" }
        val arabDeferred = async { getEdition(number, "quran-uthmani") }
        val idDeferred = async { getEdition(number, "id.indonesian") }
        val arab = arabDeferred.await()
        val id = idDeferred.await()
        require(arab.value.number == number && id.value.number == number) { "Nomor surah dari sumber tidak cocok" }
        require(QuranIntegrity.alignEditions(arab.value.ayahs, id.value.ayahs)) { "Pemetaan ayat Arab/terjemahan tidak konsisten" }
        val pairs = arab.value.ayahs.zip(id.value.ayahs).map { (a, t) ->
            AyahPair(a.numberInSurah, a.number, a.text, t.text)
        }
        val metadata = Surah(
            arab.value.number, arab.value.name, arab.value.englishName,
            arab.value.englishNameTranslation, arab.value.numberOfAyahs, arab.value.revelationType
        )
        CachedResult(
            SurahReading(metadata, pairs),
            arab.isFromCache || id.isFromCache,
            listOfNotNull(arab.notice, id.notice).distinct().joinToString(" ").ifBlank { null }
        )
    }

    private suspend fun getEdition(number: Int, edition: String): CachedResult<SurahContent> {
        val key = "quran_surah_${number}_$edition"
        val cached = cache.read<SurahContent>(key)
        return try {
            val response = api.getSurah(number, edition).data ?: error("Respons API surah kosong")
            require(response.number == number && response.ayahs.isNotEmpty()) { "Respons surah kosong atau nomor salah" }
            require(QuranIntegrity.validateEdition(number, response.ayahs, response.numberOfAyahs)) { "Jumlah atau urutan ayat dari API tidak valid" }
            cache.write(key, response, valid = true)
            CachedResult(response, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached != null && cached.number == number && QuranIntegrity.validateEdition(number, cached.ayahs, cached.numberOfAyahs)) {
                CachedResult(cached, true, "Koneksi gagal; bacaan surah yang tersimpan ditampilkan.")
            } else throw e
        }
    }

    fun validateCatalog(surahs: List<Surah>): Boolean = QuranIntegrity.validateCatalog(surahs)
}

class HadithRepository(private val api: HadithService, private val cache: JsonDiskCache) {
    private val booksType = object : TypeToken<List<HadithBook>>() {}.type

    suspend fun getBooks(): CachedResult<List<HadithBook>> {
        val cached = cache.read<List<HadithBook>>("hadith_books", booksType)
        return try {
            val books = HadithJsonParser.parseBooks(api.getBooks().toString())
            cache.write("hadith_books", books, valid = true)
            CachedResult(books, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!cached.isNullOrEmpty()) CachedResult(cached, true, "Koneksi gagal; daftar kitab tersimpan digunakan.") else throw e
        }
    }

    suspend fun getPage(book: String, page: Int, limit: Int = 20): CachedResult<HadithPage> {
        require(book.matches(Regex("[a-z0-9-]+"))) { "Identitas kitab tidak valid" }
        require(page >= 1 && limit in 1..50) { "Pagination di luar batas" }
        val key = "hadith_${book}_p${page}_l$limit"
        val cached = cache.read<HadithPage>(key)
        return try {
            val result = HadithJsonParser.parsePage(api.getPage(book, page, limit).toString(), page)
            cache.write(key, result, valid = true)
            CachedResult(result, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached != null && cached.items.isNotEmpty()) CachedResult(cached, true, "Koneksi gagal; halaman tersimpan digunakan.") else throw e
        }
    }

}

class PrayerRepository(private val api: PrayerService, private val cache: JsonDiskCache) {
    suspend fun getTimetable(date: LocalDate, latitude: Double, longitude: Double, cityLabel: String, methodId: Int = 20): CachedResult<PrayerTimetable> {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "Koordinat tidak valid" }
        val datePath = PrayerTimeLogic.datePath(date)
        val key = "prayer_${date}_${"%.3f".format(Locale.US, latitude)}_${"%.3f".format(Locale.US, longitude)}_m$methodId"
        val cached = cache.read<PrayerTimetable>(key)
        return try {
            val payload = api.getTimings(datePath, latitude, longitude, methodId).data ?: error("API jadwal sholat tidak menyediakan data")
            val labels = listOf(
                "Fajr" to "Subuh", "Sunrise" to "Terbit", "Dhuhr" to "Zuhur", "Asr" to "Asar",
                "Maghrib" to "Maghrib", "Isha" to "Isya"
            )
            val times = labels.mapNotNull { (keyName, label) ->
                payload.timings[keyName]?.let { raw -> PrayerTime(keyName, label, raw) }
            }
            require(PrayerTimeLogic.isValidPrayerTimes(times)) { "Lima waktu sholat wajib tidak lengkap/invalid" }
            val result = PrayerTimetable(
                date.toString(), payload.date?.readable ?: date.toString(), payload.date?.hijri?.let { h ->
                    listOfNotNull(h.date, h.month?.en, h.year).joinToString(" ").ifBlank { null }
                }, cityLabel, latitude, longitude, methodId, payload.meta?.method?.name, times
            )
            cache.write(key, result, valid = true)
            CachedResult(result, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached != null && cached.dateKey == date.toString() && PrayerTimeLogic.isValidPrayerTimes(cached.times)) {
                CachedResult(cached.copy(cityLabel = cityLabel), true, "API tidak tersedia; jadwal terakhir yang valid untuk tanggal ini ditampilkan.")
            } else throw e
        }
    }
}
