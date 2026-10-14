package com.nurquran.app.ui.screens

import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nurquran.app.AppContainer
import com.nurquran.app.data.HadithBook
import com.nurquran.app.data.HadithItem
import com.nurquran.app.data.HadithPagingLogic

@Composable
fun HadithScreen(container: AppContainer) {
    var books by remember { mutableStateOf<List<HadithBook>>(emptyList()) }
    var selectedSlug by remember { mutableStateOf("bukhari") }
    var booksLoading by remember { mutableStateOf(true) }
    var booksError by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    var pageCount by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    val entries = remember { mutableStateListOf<HadithItem>() }

    LaunchedEffect(Unit) {
        booksLoading = true; booksError = null
        try {
            val result = container.hadith.getBooks()
            books = result.value
            booksError = result.notice
            if (books.none { it.slug == selectedSlug }) selectedSlug = books.firstOrNull()?.slug ?: "bukhari"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            booksError = "Daftar kitab hadis belum tersedia. Periksa internet lalu coba lagi."
        } finally { booksLoading = false }
    }

    LaunchedEffect(selectedSlug, page, retry) {
        if (selectedSlug.isBlank()) return@LaunchedEffect
        loading = true; error = null
        if (page == 1) entries.clear()
        try {
            val result = container.hadith.getPage(selectedSlug, page, 20)
            val incoming = result.value.items
            if (page == 1) entries.clear()
            val merged = HadithPagingLogic.mergeUnique(entries.toList(), incoming)
            entries.clear()
            entries.addAll(merged)
            pageCount = result.value.pagination?.totalPages?.takeIf { it > 0 }?.coerceAtLeast(page)
                ?: if (incoming.size < 20) page else page + 1
            notice = result.notice
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Halaman hadis gagal dimuat. Data cache yang valid akan digunakan bila tersedia. Coba ulang halaman ini."
        } finally { loading = false }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Hadis Indonesia", "Pilih kitab, baca bertahap, dan coba ulang jika jaringan gagal")
        if (booksLoading && books.isEmpty()) LoadingState("Memuat daftar kitab…")
        else if (books.isEmpty()) ErrorState(booksError ?: "Daftar kitab belum tersedia.") { retry++ }
        else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(books, key = { it.slug }) { book ->
                    FilterChip(
                        selected = book.slug == selectedSlug,
                        onClick = { if (selectedSlug != book.slug) { selectedSlug = book.slug; page = 1; entries.clear() } },
                        label = { Text(book.name) }
                    )
                }
            }
            books.firstOrNull { it.slug == selectedSlug }?.let { book ->
                Text("${book.name} · ${book.total} entri terdaftar di sumber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!booksError.isNullOrBlank()) SourceNote(booksError!!)
            if (!notice.isNullOrBlank()) SourceNote(notice!!)
            if (loading && entries.isEmpty()) LoadingState("Memuat halaman $page…")
            else if (error != null && entries.isEmpty()) ErrorState(error!!) { retry++ }
            else {
                if (error != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(error!!, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { retry++ }) { Text("Ulangi halaman $page") }
                        }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(entries, key = { "${selectedSlug}_${it.number ?: it.hashCode()}" }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("${bookLabel(books, selectedSlug)} · ${item.number?.let { "Nomor $it" } ?: "Nomor tidak disediakan sumber"}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (!item.arabic.isNullOrBlank()) {
                                    Text(item.arabic, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp, lineHeight = 39.sp, textDirection = TextDirection.Rtl), textAlign = TextAlign.Right)
                                }
                                if (!item.translation.isNullOrBlank()) Text(item.translation, style = MaterialTheme.typography.bodyLarge, lineHeight = 25.sp)
                                if (item.arabic.isNullOrBlank() && item.translation.isNullOrBlank()) Text("Sumber tidak menyediakan teks yang dapat dibaca.")
                            }
                        }
                    }
                }
                val nextPage = HadithPagingLogic.nextPage(page, pageCount, entries.size, 20)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Halaman $page", style = MaterialTheme.typography.labelLarge)
                    if (loading) Text("Memuat…", style = MaterialTheme.typography.labelMedium)
                    else Button(onClick = { nextPage?.let { page = it } }, enabled = error == null && nextPage != null) {
                        Text("Muat berikutnya")
                    }
                }
                SourceNote("Sumber: API Hadis Indonesia (repositori berlisensi MIT). Derajat hadis tidak ditampilkan kecuali disediakan dan diverifikasi oleh sumber.")
            }
        }
    }
}

private fun bookLabel(books: List<HadithBook>, slug: String) = books.firstOrNull { it.slug == slug }?.name ?: slug
