package com.nurquran.app.ui.screens

import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nurquran.app.AppContainer
import com.nurquran.app.data.PrayerTimetable
import com.nurquran.app.domain.PrayerTimeLogic
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(container: AppContainer, onNavigate: (String) -> Unit, onOpenLastRead: (Int) -> Unit) {
    val city by container.settings.city.collectAsState(initial = "Jakarta (lokasi manual)")
    val latitude by container.settings.latitude.collectAsState(initial = -6.2088)
    val longitude by container.settings.longitude.collectAsState(initial = 106.8456)
    val lastRead by container.settings.lastRead.collectAsState(initial = null)
    var timetable by remember { mutableStateOf<PrayerTimetable?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var fromCache by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }

    LaunchedEffect(city, latitude, longitude, retry) {
        loading = true; error = null
        try {
            val response = container.prayer.getTimetable(LocalDate.now(), latitude, longitude, city)
            timetable = response.value
            fromCache = response.isFromCache
            error = response.notice
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Jadwal belum bisa dimuat. Periksa koneksi atau ubah lokasi di menu Sholat."
        } finally { loading = false }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Assalamu’alaikum", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Temani hari dengan Al-Qur’an", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(city, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("WAKTU SHOLAT BERIKUTNYA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                if (loading) {
                    Text("Memeriksa jadwal…", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                } else if (timetable != null) {
                    val now = LocalDateTime.now()
                    val next = PrayerTimeLogic.nextPrayer(timetable!!.times, now)
                    Text(next?.label ?: "Jadwal hari ini", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    Text(next?.time?.let { "Pukul $it" } ?: "Tidak ada waktu berikutnya hari ini", color = MaterialTheme.colorScheme.onPrimary)
                    Text(timetable!!.displayDate + (timetable!!.hijriDate?.let { " · $it H" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .85f))
                    Button(onClick = { onNavigate("Sholat") }) { Text("Lihat jadwal lengkap") }
                } else {
                    Text("Jadwal belum tersedia", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                    Text(error ?: "Data jadwal belum diterima.", color = MaterialTheme.colorScheme.onPrimary)
                    OutlinedButton(onClick = { retry++ }) { Text("Coba lagi") }
                }
            }
        }

        if (timetable != null) {
            Text("Jadwal hari ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                timetable!!.times.filter { it.key in setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha") }.forEach { prayer ->
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(prayer.label, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(PrayerTimeLogic.normalizedTime(prayer.time) ?: "—", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (!error.isNullOrBlank()) SourceNote(error!!)
            if (fromCache) SourceNote("Data berasal dari cache lokal.")
        }

        Text("Akses cepat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val shortcuts = listOf(
            Shortcut("Al-Qur’an", "Qur’an", Icons.Default.MenuBook),
            Shortcut("Hadis", "Hadis", Icons.Default.AutoStories),
            Shortcut("Sholat", "Sholat", Icons.Default.Schedule),
            Shortcut("Kiblat", "Kiblat", Icons.Default.Explore),
            Shortcut("Doa & Dzikir", "Doa & Dzikir", Icons.Default.SelfImprovement)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shortcuts) { shortcut ->
                Card(onClick = { onNavigate(shortcut.destination) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Icon(shortcut.icon, contentDescription = shortcut.label, tint = MaterialTheme.colorScheme.primary)
                        Text(shortcut.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Lanjutkan bacaan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(lastRead?.let { "Terakhir dibuka: $it" } ?: "Mulai membaca Al-Qur’an dan posisi terakhir akan disimpan di perangkat ini.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = {
                    val num = lastRead?.substringBefore(":")?.toIntOrNull()?.coerceIn(1, 114) ?: 1
                    onOpenLastRead(num)
                }) { Text("Buka Al-Qur’an") }
            }
        }
        SourceNote("Jadwal dihitung oleh AlAdhan metode 20 (KEMENAG Indonesia); tetap cocokkan dengan jadwal resmi setempat.")
    }
}

private data class Shortcut(val label: String, val destination: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
