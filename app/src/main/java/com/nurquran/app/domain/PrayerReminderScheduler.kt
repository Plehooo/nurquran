package com.nurquran.app.domain

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nurquran.app.R
import com.nurquran.app.data.PrayerTime
import java.time.LocalDateTime
import java.time.ZoneId

object PrayerReminderScheduler {
    internal const val CHANNEL_ID = "prayer_time_reminders"
    private const val PREFS = "prayer_reminder_runtime"
    private const val ENABLED = "enabled"
    internal const val REQUEST_BASE = 4920

    fun setEnabledMirror(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).apply()
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    /** Schedules only validated future events for the supplied local date. Returns whether exact timing is available. */
    fun scheduleToday(context: Context, times: List<PrayerTime>): Boolean {
        if (!isEnabled(context) || !PrayerTimeLogic.isValidPrayerTimes(times)) {
            cancelAll(context)
            return false
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        val now = LocalDateTime.now()
        val events = PrayerTimeLogic.upcomingEvents(times.filter { it.key in setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha") }, now)
        cancelAll(context)
        if (events.isEmpty()) return false
        var useExact = exactAllowed
        events.forEach { (prayer, localTime) ->
            val pending = pendingIntent(context, prayer.key.hashCode().and(0x7fffffff) % 10000, prayer.label, prayer.time)
            val trigger = localTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (useExact) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                } catch (_: SecurityException) {
                    // Permission may change between the check and scheduling; degrade safely.
                    useExact = false
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, trigger, 10 * 60 * 1000L, pending)
                }
            } else {
                // Inexact fallback: Android may defer this within the allowed window.
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, trigger, 10 * 60 * 1000L, pending)
            }
        }
        return useExact
    }

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { key ->
            manager.cancel(pendingIntent(context, key.hashCode().and(0x7fffffff) % 10000, "", ""))
        }
    }

    private fun pendingIntent(context: Context, id: Int, prayer: String, time: String): PendingIntent {
        val intent = Intent(context, PrayerReminderReceiver::class.java)
            .setAction("com.nurquran.app.PRAYER_REMINDER.$id")
            .putExtra("prayer_name", prayer)
            .putExtra("prayer_time", time)
        return PendingIntent.getBroadcast(
            context, REQUEST_BASE + id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!PrayerReminderScheduler.isEnabled(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(PrayerReminderScheduler.CHANNEL_ID, "Pengingat waktu sholat", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notifikasi lokal untuk waktu sholat yang telah dijadwalkan"
                }
            )
        }
        val prayerName = intent.getStringExtra("prayer_name").orEmpty().ifBlank { "Sholat" }
        val time = intent.getStringExtra("prayer_time").orEmpty()
        // Use the platform builder to keep this path independent of AndroidX builder APIs.
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, PrayerReminderScheduler.CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = notificationBuilder
            .setSmallIcon(R.drawable.ic_nurquran)
            .setContentTitle("Waktu $prayerName")
            .setContentText(if (time.isBlank()) "Waktu sholat telah tiba." else "Jadwal lokal pukul $time.")
            .setStyle(Notification.BigTextStyle().bigText("Waktu $prayerName telah tiba. Ini adalah pengingat notifikasi, bukan pemutar audio adzan."))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(PrayerReminderScheduler.REQUEST_BASE + prayerName.hashCode().and(0x7fffffff) % 10000, notification) }
    }
}
