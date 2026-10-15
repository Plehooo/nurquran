package com.ditzy.nurquran.prayer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.ditzy.nurquran.MainActivity
import com.ditzy.nurquran.data.PrayerAlarmEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val PREFS = "nurquran_preferences"
private const val ALARM_CHANNEL = "nurquran_adhan_playback"
private const val FALLBACK_CHANNEL = "nurquran_adhan_notice"
private const val NOTIFICATION_ID = 7314
private const val EXTRA_PRAYER_NAME = "prayer_name"

object PrayerScheduler {
    const val EXTRA_NAME = EXTRA_PRAYER_NAME

    fun scheduleEvents(context: Context, events: List<PrayerAlarmEvent>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("adhan_enabled", false)) {
            cancelSaved(context)
            return
        }
        cancelPendingFromSaved(context, prefs.getString("prayer_events", "[]") ?: "[]")
        val now = System.currentTimeMillis()
        val future = events.filter { it.atMillis > now + 5_000L }.distinctBy { it.atMillis }.sortedBy { it.atMillis }
        val persisted = JSONArray()
        future.forEach { persisted.put(JSONObject().put("name", it.name).put("at", it.atMillis)) }
        prefs.edit().putString("prayer_events", persisted.toString()).apply()

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        future.forEach { event ->
            val requestCode = ((event.atMillis / 60_000L) % Int.MAX_VALUE).toInt()
            val intent = Intent(context, AdhanReceiver::class.java).apply {
                putExtra(EXTRA_PRAYER_NAME, event.name)
                putExtra("event_at", event.atMillis)
            }
            val pending = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.atMillis, pending)
                } else {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.atMillis, pending)
                }
            }.onFailure {
                runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.atMillis, pending) }
            }
        }
    }

    fun cancelSaved(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        cancelPendingFromSaved(context, prefs.getString("prayer_events", "[]") ?: "[]")
        prefs.edit().putString("prayer_events", "[]").apply()
        runCatching { context.stopService(Intent(context, AdhanService::class.java)) }
    }

    private fun cancelPendingFromSaved(context: Context, raw: String) {
        val events = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(PrayerAlarmEvent(o.optString("name", "Waktu sholat"), o.optLong("at")))
                }
            }
        }.getOrDefault(emptyList())
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        events.forEach { event ->
            val requestCode = ((event.atMillis / 60_000L) % Int.MAX_VALUE).toInt()
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AdhanReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                runCatching { manager.cancel(pending) }
                pending.cancel()
            }
        }
    }

    fun rescheduleSaved(context: Context) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("prayer_events", "[]") ?: "[]"
        val events = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(PrayerAlarmEvent(o.optString("name", "Waktu sholat"), o.optLong("at")))
                }
            }
        }.getOrDefault(emptyList())
        scheduleEvents(context, events)
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }
}

class AdhanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("adhan_enabled", false)) return
        val name = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Waktu sholat"
        val serviceIntent = Intent(context, AdhanService::class.java).apply { putExtra(EXTRA_PRAYER_NAME, name) }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
            else context.startService(serviceIntent)
        } catch (_: Exception) {
            showFallbackNotification(context, name)
        }

        // Keep tomorrow's and the following day's alarms armed, even when the app UI stays closed.
        refreshPrayerEventsAsync(context, goAsync())
    }

    private fun showFallbackNotification(context: Context, name: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(FALLBACK_CHANNEL, "Pengingat adzan", NotificationManager.IMPORTANCE_HIGH))
        }
        val openIntent = PendingIntent.getActivity(
            context, 91, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, FALLBACK_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Waktu sholat $name")
            .setContentText("Saatnya menunaikan sholat. Buka Nur Qur’an untuk jadwal ibadah.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
            )) {
            refreshPrayerEventsAsync(context, goAsync())
        }
    }
}

private fun refreshPrayerEventsAsync(context: Context, pendingResult: BroadcastReceiver.PendingResult) {
    val appContext = context.applicationContext
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            val repository = com.ditzy.nurquran.data.IslamicRepository(appContext)
            if (!repository.isAdhanEnabled()) {
                PrayerScheduler.cancelSaved(appContext)
                return@launch
            }
            val location = repository.getLocation()
            val pack = repository.getPrayerPack(location)
            if (pack.events.isNotEmpty()) {
                PrayerScheduler.scheduleEvents(appContext, pack.events)
            } else {
                // Existing alarms are still better than leaving the device completely unscheduled.
                PrayerScheduler.rescheduleSaved(appContext)
            }
        } catch (_: Exception) {
            PrayerScheduler.rescheduleSaved(appContext)
        } finally {
            pendingResult.finish()
        }
    }
}

class AdhanService : Service() {
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finishPlayback()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Waktu sholat"
        createPlaybackChannel()
        val notification = playbackNotification(prayerName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        runCatching {
            player?.release()
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            val audio = File(filesDir, "adhan.ogg")
            if (audio.exists() && audio.length() > 20_000) {
                mediaPlayer.setDataSource(audio.absolutePath)
            } else {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: throw IllegalStateException("Tidak ada audio alarm di perangkat")
                mediaPlayer.setDataSource(this, uri)
            }
            mediaPlayer.setOnPreparedListener { it.start() }
            mediaPlayer.setOnCompletionListener { finishPlayback() }
            mediaPlayer.setOnErrorListener { _, _, _ -> finishPlayback(); true }
            mediaPlayer.prepareAsync()
        }.onFailure {
            finishPlayback()
        }
        return START_NOT_STICKY
    }

    private fun createPlaybackChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(ALARM_CHANNEL, "Adzan otomatis", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Menjaga pemutaran adzan selama audio berlangsung"
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun playbackNotification(name: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 92, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 93, Intent(this, AdhanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Adzan — $name")
            .setContentText("Audio adzan sedang diputar · Nur Qur’an")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "Hentikan audio", stopIntent)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun finishPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        else stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { player?.release() }
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.ditzy.nurquran.action.STOP_ADHAN"
    }
}
