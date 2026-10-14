package com.nurquran.app.data

import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface QuranCloudService {
    @GET("surah") suspend fun getSurahs(): QuranEnvelope<List<Surah>>
    @GET("surah/{number}/{edition}") suspend fun getSurah(
        @Path("number") number: Int,
        @Path("edition") edition: String
    ): QuranEnvelope<SurahContent>
}

interface HadithService {
    @GET("hadith") suspend fun getBooks(): JsonElement
    @GET("hadith/{slug}") suspend fun getPage(
        @Path("slug") slug: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): JsonElement
}

interface PrayerService {
    @GET("timings/{date}") suspend fun getTimings(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 20
    ): AlAdhanEnvelope
}

object ApiServices {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val quran: QuranCloudService by lazy { retrofit("https://api.alquran.cloud/v1/").create(QuranCloudService::class.java) }
    val hadith: HadithService by lazy { retrofit("https://hadis-api-id.vercel.app/").create(HadithService::class.java) }
    val prayer: PrayerService by lazy { retrofit("https://api.aladhan.com/v1/").create(PrayerService::class.java) }
}
