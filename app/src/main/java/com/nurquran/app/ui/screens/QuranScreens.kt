package com.nurquran.app.ui.screens

import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nurquran.app.AppContainer
import com.nurquran.app.data.Surah
import com.nurquran.app.data.SurahReading
import com.nurquran.app.ui.theme.Gold
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun QuranScreen(container: AppContainer, onOpenSurah: (Int) -> Unit) {
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var fromCache by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var retry by remember { mutableStateOf(0) }

    LaunchedEffect(retry) {
        loading = true; error = null
        try {
            val result = container.quran.getSurahs()
            surahs = result.value
            fromCache = result.isFromCache
            error = result.notice
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Katalog Al-Qur’an belum dapat dimuat. Pastikan internet aktif lalu coba lagi."
        } finally { loading = false }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Al-Qur’an", "114 surah · teks Arab dan terjemahan Indonesia")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Cari nama surah atau nomor") }
        )
        if (!error.isNullOrBlank()) SourceNote(error!!)
        if (loading && surahs.isEmpty()) LoadingState("Memuat katalog surah…")
        else if (surahs.isEmpty()) ErrorState(error ?: "Katalog belum tersedia.") { retry++ }
        else {
            if (fromCache) SourceNote("Katalog dibuka dari cache lokal.")
            val filtered = surahs.filter { surah ->
                val q = query.trim()
                q.isEmpty() || surah.number.toString() == q || surah.englishName.contains(q, true) ||
                    surah.englishNameTranslation.contains(q, true) || surah.name.contains(q)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.number }) { surah ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenSurah(surah.number) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium) {
                                Text("${surah.number}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(surah.englishName, fontWeight = FontWeight.SemiBold)
                                Text("${surah.englishNameTranslation} · ${surah.numberOfAyahs} ayat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(surah.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuranReaderScreen(
    container: AppContainer,
    number: Int,
    bookmarks: Set<String>,
    onBack: () -> Unit,
    onToggleBookmark: (String) -> Unit
) {
    var reading by remember(number) { mutableStateOf<SurahReading?>(null) }
    var loading by remember(number) { mutableStateOf(true) }
    var error by remember(number) { mutableStateOf<String?>(null) }
    var fromCache by remember(number) { mutableStateOf(false) }
    var retry by remember(number) { mutableStateOf(0) }
    var arabicSize by remember(number) { mutableStateOf(29) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(number, retry) {
        loading = true; error = null
        try {
            val result = container.quran.getReading(number)
            reading = result.value; fromCache = result.isFromCache; error = result.notice
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Teks Arab dan terjemahan tidak berhasil dipasangkan dengan aman. Coba lagi saat koneksi tersedia."
        } finally { loading = false }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
            Column(Modifier.weight(1f)) {
                Text(reading?.metadata?.englishName ?: "Surah $number", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(reading?.metadata?.englishNameTranslation ?: "Memuat bacaan…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { retry++ }) { Icon(Icons.Default.Refresh, contentDescription = "Muat ulang") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("Ukuran Arab", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { arabicSize = (arabicSize - 2).coerceAtLeast(20) }) { Text("A−") }
            TextButton(onClick = { arabicSize = (arabicSize + 2).coerceAtMost(44) }) { Text("A+") }
        }
        if (!error.isNullOrBlank()) SourceNote(error!!)
        if (loading && reading == null) LoadingState("Memuat teks ayat…")
        else if (reading == null) ErrorState(error ?: "Bacaan belum tersedia.") { retry++ }
        else {
            if (fromCache) SourceNote("Bacaan dari cache lokal. Sumber: Al Quran Cloud · quran-uthmani + id.indonesian.")
            reading!!.metadata.let { meta ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(meta.name, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
                        Text(meta.englishName, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        Text("${meta.numberOfAyahs} ayat · ${if (meta.revelationType.equals("Meccan", true)) "Makkiyah" else "Madaniyah"}", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(reading!!.ayahs, key = { it.globalNumber }) { ayah ->
                    val ref = "$number:${ayah.numberInSurah}"
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().clickable {
                            scope.launch { container.settings.setLastRead(ref) }
                        }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${ayah.numberInSurah}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { onToggleBookmark(ref) }) {
                                    Icon(if (ref in bookmarks) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = if (ref in bookmarks) "Hapus bookmark ayat $ref" else "Simpan bookmark ayat $ref",
                                        tint = if (ref in bookmarks) Gold else MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                ayah.arabic,
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(fontSize = arabicSize.sp, lineHeight = (arabicSize * 1.85).sp, textDirection = TextDirection.Rtl),
                                textAlign = TextAlign.Right
                            )
                            Text(ayah.translation, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)
                            Text("Surah ${reading!!.metadata.englishName} · Ayat ${ayah.numberInSurah} · Global ${ayah.globalNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
