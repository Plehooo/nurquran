package com.nurquran.app.ui.screens

import kotlinx.coroutines.CancellationException
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nurquran.app.domain.PrayerReminderScheduler
import com.nurquran.app.AppContainer
import com.nurquran.app.data.PrayerTimetable
import com.nurquran.app.domain.PrayerTimeLogic
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun PrayerScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val city by container.settings.city.collectAsState(initial = "Jakarta (lokasi manual)")
    val latitude by container.settings.latitude.collectAsState(initial = -6.2088)
    val longitude by container.settings.longitude.collectAsState(initial = 106.8456)
    var cityInput by remember(city) { mutableStateOf(city) }
    var latitudeInput by remember(latitude) { mutableStateOf(latitude.toString()) }
    var longitudeInput by remember(longitude) { mutableStateOf(longitude.toString()) }
    var timetable by remember { mutableStateOf<PrayerTimetable?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var fromCache by remember { mutableStateOf(false) }
    val remindersEnabled by container.settings.remindersEnabled.collectAsState(initial = false)

    fun setReminderOn() {
        val schedule = timetable
        if (schedule == null) { message = "Jadwal valid belum tersedia. Muat jadwal sebelum mengaktifkan pengingat."; return }
        scope.launch {
            container.settings.setRemindersEnabled(true)
            val exact = PrayerReminderScheduler.scheduleToday(context, schedule.times)
            message = if (exact) "Pengingat notifikasi dijadwalkan untuk waktu yang tersisa hari ini. Bukan audio adzan." else "Pengingat mungkin menggunakan alarm tidak presisi atau tidak ada waktu tersisa hari ini; buka aplikasi kembali untuk memperbarui jadwal harian."
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) setReminderOn() else message = "Izin notifikasi ditolak, pengingat tidak diaktifkan."
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            val location = readLastKnownLocation(context)
            if (location != null) {
                scope.launch {
                    runCatching {
                        container.settings.setLocation("Lokasi perangkat", location.latitude, location.longitude)
                        message = "Lokasi diperbarui dari penyedia lokasi Android. Periksa kembali kota dan jadwalnya."
                    }.onFailure { message = "Lokasi ditemukan, tetapi tidak dapat disimpan." }
                }
            } else message = "Izin diberikan, tetapi lokasi terakhir belum tersedia. Aktifkan layanan lokasi atau masukkan koordinat manual."
        } else message = "Izin lokasi ditolak. Kamu tetap bisa mengatur kota dan koordinat secara manual."
    }

    LaunchedEffect(city, latitude, longitude, retry, remindersEnabled) {
        loading = true; error = null
        try {
            val result = container.prayer.getTimetable(LocalDate.now(), latitude, longitude, city)
            timetable = result.value; fromCache = result.isFromCache; error = result.notice
            if (remindersEnabled) {
                val exact = PrayerReminderScheduler.scheduleToday(context, result.value.times)
                if (!exact) message = "Pengingat aktif pada mode tidak presisi atau belum ada jadwal tersisa hari ini."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Jadwal gagal dimuat. Periksa koneksi dan koordinat. Jadwal resmi masjid/Kemenag setempat tetap menjadi acuan."
        } finally { loading = false }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Jadwal Sholat", "Perhitungan berdasarkan tanggal dan lokasi pilihanmu")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pengingat waktu sholat", fontWeight = FontWeight.Bold)
                    Text(if (remindersEnabled) "Aktif · notifikasi sistem, bukan audio adzan" else "Nonaktif secara default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = remindersEnabled, onCheckedChange = { enabled ->
                    if (!enabled) {
                        scope.launch {
                            container.settings.setRemindersEnabled(false)
                            PrayerReminderScheduler.cancelAll(context)
                            message = "Pengingat dinonaktifkan dan alarm lokal dibatalkan."
                        }
                    } else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else setReminderOn()
                })
            }
        }
        if (remindersEnabled) SourceNote("Bila izin alarm presisi tidak tersedia, Android menerima jadwal tidak presisi yang dapat meleset. Jadwal saat ini mencakup waktu tersisa hari ini; buka aplikasi setiap hari agar diperbarui. Audio adzan otomatis belum disertakan.")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Lokasi: $city", fontWeight = FontWeight.SemiBold)
                Text("Koordinat: %.4f, %.4f".format(java.util.Locale.US, latitude, longitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine || coarse) {
                        val location = readLastKnownLocation(context)
                        if (location != null) scope.launch {
                            container.settings.setLocation("Lokasi perangkat", location.latitude, location.longitude)
                            message = "Lokasi berhasil dibaca dari perangkat."
                        } else message = "Lokasi terakhir belum tersedia. Aktifkan GPS atau gunakan koordinat manual."
                    } else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                }, modifier = Modifier.fillMaxWidth()) { Text("Gunakan lokasi perangkat") }
                OutlinedTextField(value = cityInput, onValueChange = { cityInput = it }, label = { Text("Nama kota / lokasi manual") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = latitudeInput, onValueChange = { latitudeInput = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = longitudeInput, onValueChange = { longitudeInput = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Button(onClick = {
                    val lat = latitudeInput.toDoubleOrNull(); val lon = longitudeInput.toDoubleOrNull()
                    if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0 || cityInput.isBlank()) {
                        message = "Masukkan nama lokasi, latitude −90…90, dan longitude −180…180."
                    } else scope.launch {
                        runCatching { container.settings.setLocation(cityInput.trim(), lat, lon) }
                            .onSuccess { message = "Lokasi manual disimpan." }
                            .onFailure { message = "Lokasi tidak valid." }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Simpan lokasi manual") }
                if (!message.isNullOrBlank()) Text(message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (!error.isNullOrBlank()) ErrorState(error!!) { retry++ }
        else if (loading && timetable == null) LoadingState("Menghitung jadwal…")
        else if (timetable != null) {
            Text("${timetable!!.displayDate}${timetable!!.hijriDate?.let { " · $it H" } ?: ""}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    timetable!!.times.filter { it.key in setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha") }.forEach { prayer ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(prayer.label, style = MaterialTheme.typography.bodyLarge)
                            Text(PrayerTimeLogic.normalizedTime(prayer.time) ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Text("Metode: ${timetable!!.methodName ?: "KEMENAG Indonesia (AlAdhan method 20)"}", style = MaterialTheme.typography.bodySmall)
            if (fromCache) SourceNote("Jadwal dibaca dari cache lokal.")
            SourceNote("Jadwal API dapat berbeda dari jadwal resmi daerah/masjid. Cocokkan dengan sumber resmi setempat.")
            TextButton(onClick = { retry++ }) { Text("Perbarui jadwal") }
        }
    }
}

private fun readLastKnownLocation(context: Context): Location? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return runCatching {
        manager.getProviders(true).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }.getOrNull()
}
