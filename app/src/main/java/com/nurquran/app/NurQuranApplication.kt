package com.nurquran.app

import android.app.Application
import android.os.StrictMode
import com.nurquran.app.data.ApiServices
import com.nurquran.app.data.HadithRepository
import com.nurquran.app.data.PrayerRepository
import com.nurquran.app.data.QuranRepository
import com.nurquran.app.storage.JsonDiskCache
import com.nurquran.app.storage.SettingsStore

class NurQuranApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        }
        container = AppContainer(this)
    }
}

class AppContainer(context: android.content.Context) {
    private val cache = JsonDiskCache(context.applicationContext)
    val settings = SettingsStore(context.applicationContext)
    val quran = QuranRepository(ApiServices.quran, cache)
    val hadith = HadithRepository(ApiServices.hadith, cache)
    val prayer = PrayerRepository(ApiServices.prayer, cache)
    suspend fun clearApiCache() = cache.deleteAll()
}
