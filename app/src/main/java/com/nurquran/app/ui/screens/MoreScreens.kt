package com.nurquran.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nurquran.app.AppContainer
import com.nurquran.app.domain.QiblaCalculator
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val actions = listOf(
        Triple("Kiblat", "Arah ke Ka’bah dari koordinat pilihanmu", "Kiblat"),
        Triple("Doa & Dzikir", "Sumber dan status kelengkapan konten", "Doa & Dzikir"),
        Triple("Bookmark", "Ayat yang disimpan pada perangkat", "Bookmark"),
        Triple("Pengaturan", "Tema, lokasi, cache, dan informasi aplikasi", "Pengaturan"),
        Triple("Tentang Nur Qur’an", "Sumber data, lisensi, dan privasi", "Tentang")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Lainnya", "Semua fitur tambahan dalam satu tempat")
        actions.forEach { (title, subtitle, route) ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onNavigate(route) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (route) {
                        "Kiblat" -> androidx.compose.material3.Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        "Doa & Dzikir" -> androidx.compose.material3.Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        "Bookmark" -> androidx.compose.material3.Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        "Pengaturan" -> androidx.compose.material3.Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        else -> androidx.compose.material3.Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        SourceNote("Nur Qur’an tidak memakai login, iklan, analytics, atau sinkronisasi cloud.")
    }
}

@Composable
fun QiblaScreen(container: AppContainer) {
    val city by container.settings.city.collectAsState(initial = "Jakarta (lokasi manual)")
    val latitude by container.settings.latitude.collectAsState(initial = -6.2088)
    val longitude by container.settings.longitude.collectAsState(initial = 106.8456)
    val bearing = runCatching { QiblaCalculator.bearing(latitude, longitude) }.getOrDefault(0.0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Arah Kiblat", city)
        val primaryColor = MaterialTheme.colorScheme.primary
        val goldColor = MaterialTheme.colorScheme.secondary
        Canvas(Modifier.size(250.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * .43f
            drawCircle(color = goldColor, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
            drawCircle(color = primaryColor, radius = radius * .10f, center = center)
            val rad = Math.toRadians(bearing)
            val end = Offset(center.x + radius * .78f * sin(rad).toFloat(), center.y - radius * .78f * cos(rad).toFloat())
            drawLine(color = primaryColor, start = center, end = end, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(color = goldColor, radius = 8.dp.toPx(), center = end)
        }
        Text("%.1f° dari utara geografis".format(java.util.Locale.US, bearing), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Ka’bah · 21.422487° N, 39.826206° E", style = MaterialTheme.typography.bodyMedium)
        InfoCard("Catatan arah", "Ini adalah bearing matematis dari koordinat tersimpan, bukan kompas sensor langsung. Arah di layar tidak mengikuti rotasi ponsel. Untuk arah fisik, gunakan kompas yang terkalibrasi.")
        Text("Untuk memperbarui koordinat, buka menu Sholat dan pilih lokasi perangkat atau masukkan koordinat manual.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DoaScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Doa & Dzikir", "Konten bersumber dan tidak dibuat-buat")
        InfoCard("Koleksi belum dimuat", "Paket awal ini belum menyertakan teks doa/dzikir karena setiap redaksi, transliterasi, keutamaan, jumlah pengulangan, dan rujukan harus diverifikasi dari sumber berlisensi. Tidak ada teks doa buatan yang ditampilkan sebagai riwayat agama.")
        InfoCard("Yang bisa kamu lakukan", "Baca Al-Qur’an dan hadis dari tab utama. Doa/dzikir akan ditambahkan setelah sumber serta lisensi kontennya dicatat dan diuji.")
    }
}

@Composable
fun BookmarksScreen(container: AppContainer, onOpenSurah: (Int) -> Unit) {
    val bookmarks by container.settings.bookmarks.collectAsState(initial = emptySet())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Bookmark", "Disimpan secara lokal di perangkat")
        if (bookmarks.isEmpty()) {
            InfoCard("Belum ada bookmark", "Buka Al-Qur’an, lalu tekan ikon pita pada ayat yang ingin disimpan.")
            Button(onClick = { onOpenSurah(1) }) { Text("Mulai membaca") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bookmarks.sorted(), key = { it }) { ref ->
                    val surah = ref.substringBefore(":").toIntOrNull()?.coerceIn(1, 114) ?: 1
                    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenSurah(surah) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Surah $surah · Ayat ${ref.substringAfter(":", "1")}", fontWeight = FontWeight.SemiBold)
                                Text("Buka bacaan surah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("›", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(container: AppContainer, onThemeChange: (String) -> Unit, themeMode: String, onNavigate: (String) -> Unit) {
    val city by container.settings.city.collectAsState(initial = "Jakarta (lokasi manual)")
    val latitude by container.settings.latitude.collectAsState(initial = -6.2088)
    val longitude by container.settings.longitude.collectAsState(initial = 106.8456)
    val bookmarks by container.settings.bookmarks.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    var cacheMessage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Pengaturan", "Preferensi disimpan lokal")
        Text("Tema tampilan", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Sistem", "light" to "Terang", "dark" to "Gelap").forEach { (value, label) ->
                FilterChip(selected = themeMode == value, onClick = { onThemeChange(value) }, label = { Text(label) })
            }
        }
        InfoCard("Lokasi", "$city\nLatitude: %.4f · Longitude: %.4f".format(java.util.Locale.US, latitude, longitude))
        InfoCard("Privasi", "Lokasi dan bookmark disimpan pada perangkat. Koordinat dikirim ke API AlAdhan hanya saat meminta jadwal sholat. Tidak ada akun, iklan, analytics, atau sinkronisasi cloud.")
        InfoCard("Cache API", "Cache berisi data katalog/bacaan Qur’an, halaman hadis, dan jadwal yang berhasil divalidasi. Menghapus cache tidak menghapus bookmark atau posisi bacaan.")
        OutlinedButton(onClick = {
            scope.launch { container.clearApiCache(); cacheMessage = "Cache API lokal dihapus. Bookmark dan posisi bacaan dipertahankan." }
        }, modifier = Modifier.fillMaxWidth()) { Text("Hapus cache API") }
        if (!cacheMessage.isNullOrBlank()) Text(cacheMessage!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { onNavigate("Tentang") }, modifier = Modifier.fillMaxWidth()) { Text("Tentang, sumber, lisensi & privasi") }
        Text("Bookmark tersimpan: ${bookmarks.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AboutScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Tentang Nur Qur’an", "Teman Ibadah Setiap Hari")
        InfoCard("Sumber data", "Al-Qur’an dan terjemahan Indonesia: Al Quran Cloud API (edisi quran-uthmani dan id.indonesian). Jadwal sholat: AlAdhan API, method 20. Hadis: API Hadis Indonesia oleh renomureza.")
        InfoCard("Peringatan jadwal", "Perhitungan API dapat berbeda dari jadwal resmi daerah, masjid, atau pengumuman Kementerian Agama. Gunakan jadwal resmi setempat sebagai rujukan akhir.")
        InfoCard("Batasan versi 1.0.0", "Pembacaan Qur’an dan hadis memerlukan internet pertama kali; konten yang berhasil divalidasi dapat dibuka kembali dari cache. Audio murottal/adzan, sensor kompas langsung, dan koleksi doa/dzikir offline belum disertakan dalam build awal ini.")
        InfoCard("Privasi", "Aplikasi tidak mengumpulkan akun, analytics, data iklan, atau mengirim bookmark/riwayat ke server. Lihat PRIVACY.md untuk detail alur data.")
    }
}
