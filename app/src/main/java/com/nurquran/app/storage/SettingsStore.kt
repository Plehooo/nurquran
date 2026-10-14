package com.nurquran.app.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nurquran.app.domain.PrayerReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "nurquran_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val darkMode = stringPreferencesKey("theme_mode")
        val bookmarks = stringPreferencesKey("bookmarks_csv")
        val lastRead = stringPreferencesKey("last_read")
        val latitude = doublePreferencesKey("manual_latitude")
        val longitude = doublePreferencesKey("manual_longitude")
        val city = stringPreferencesKey("manual_city")
        val remindersEnabled = booleanPreferencesKey("prayer_reminders_enabled")
    }

    val themeMode: Flow<String> = context.userSettingsDataStore.data.map { it[Keys.darkMode] ?: "system" }
    val bookmarks: Flow<Set<String>> = context.userSettingsDataStore.data.map { raw ->
        raw[Keys.bookmarks].orEmpty().split(',').filter { it.isNotBlank() }.toSet()
    }
    val lastRead: Flow<String?> = context.userSettingsDataStore.data.map { it[Keys.lastRead] }
    val latitude: Flow<Double> = context.userSettingsDataStore.data.map { it[Keys.latitude] ?: -6.2088 }
    val longitude: Flow<Double> = context.userSettingsDataStore.data.map { it[Keys.longitude] ?: 106.8456 }
    val city: Flow<String> = context.userSettingsDataStore.data.map { it[Keys.city] ?: "Jakarta (lokasi manual)" }
    val remindersEnabled: Flow<Boolean> = context.userSettingsDataStore.data.map { it[Keys.remindersEnabled] ?: false }

    suspend fun setTheme(mode: String) { context.userSettingsDataStore.edit { it[Keys.darkMode] = mode } }
    suspend fun setLastRead(ref: String) { context.userSettingsDataStore.edit { it[Keys.lastRead] = ref } }
    suspend fun toggleBookmark(ref: String, current: Set<String>) {
        val changed = current.toMutableSet().also { if (!it.add(ref)) it.remove(ref) }
        context.userSettingsDataStore.edit { it[Keys.bookmarks] = changed.sorted().joinToString(",") }
    }
    suspend fun setLocation(city: String, latitude: Double, longitude: Double) {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        context.userSettingsDataStore.edit {
            it[Keys.city] = city.take(80)
            it[Keys.latitude] = latitude
            it[Keys.longitude] = longitude
        }
    }
    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { it[Keys.remindersEnabled] = enabled }
        PrayerReminderScheduler.setEnabledMirror(context, enabled)
    }
}
