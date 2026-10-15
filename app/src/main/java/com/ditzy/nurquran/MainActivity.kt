package com.ditzy.nurquran

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ditzy.nurquran.data.AppLocation
import com.ditzy.nurquran.data.Ayah
import com.ditzy.nurquran.data.DailyAyah
import com.ditzy.nurquran.data.HadithItem
import com.ditzy.nurquran.data.IslamicRepository
import com.ditzy.nurquran.data.PrayerPack
import com.ditzy.nurquran.data.QuranBookmark
import com.ditzy.nurquran.data.QuranAudioController
import com.ditzy.nurquran.data.Surah
import com.ditzy.nurquran.prayer.PrayerScheduler
import com.ditzy.nurquran.ui.Cream
import com.ditzy.nurquran.ui.DeepEmerald
import com.ditzy.nurquran.ui.Emerald
import com.ditzy.nurquran.ui.Gold
import com.ditzy.nurquran.ui.Ink
import com.ditzy.nurquran.ui.Jade
import com.ditzy.nurquran.ui.MutedInk
import com.ditzy.nurquran.ui.NurQuranTheme
import com.ditzy.nurquran.ui.PaleGold
import com.ditzy.nurquran.ui.PaleGreen
import com.ditzy.nurquran.ui.SoftCream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(8, 47, 36)
        window.navigationBarColor = AndroidColor.rgb(247, 243, 232)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val repository = IslamicRepository(applicationContext)
        setContent { NurQuranTheme { NurQuranApp(repository) } }
    }
}

private enum class AppTab { HOME, QURAN, HADITH, PRAYER, QIBLA, SETTINGS, READER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NurQuranApp(repository: IslamicRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var tab by remember { mutableStateOf(AppTab.HOME) }
    var surahs by remember { mutableStateOf(repository.cachedSurahs()) }
    var selectedSurah by remember { mutableStateOf<Surah?>(null) }
    var verses by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    var isVersesLoading by remember { mutableStateOf(false) }
    var dailyAyah by remember { mutableStateOf<DailyAyah?>(null) }
    var location by remember { mutableStateOf(repository.getLocation()) }
    var prayerPack by remember { mutableStateOf(PrayerPack(repository.cachedTodayPrayerTimes(location), repository.cachedPrayerEvents(), "")) }
    var isPrayerLoading by remember { mutableStateOf(false) }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf("bukhari") }
    var hadiths by remember { mutableStateOf<List<HadithItem>>(emptyList()) }
    var hadithStart by remember { mutableIntStateOf(21) }
    var isHadithLoading by remember { mutableStateOf(false) }
    var adhanEnabled by remember { mutableStateOf(false) }
    var customAdhanAudio by remember { mutableStateOf(repository.isCustomAdhanAudioSelected()) }
    var permissionHint by remember { mutableStateOf("") }
    var playingGlobalAyah by remember { mutableIntStateOf(-1) }
    var quranBookmarks by remember { mutableStateOf(repository.getQuranBookmarks()) }
    var autoPlayRecitation by remember { mutableStateOf(repository.isAutoPlayRecitationEnabled()) }
    val quranAudio = remember { QuranAudioController() }


    val adhanAudioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val target = File(context.filesDir, "adhan.ogg")
                        val temp = File(context.filesDir, "adhan-selected.tmp")
                        val backup = File(context.filesDir, "adhan-previous.tmp")
                        temp.delete()
                        backup.delete()

                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("File audio tidak dapat dibaca.")
                        input.use { source ->
                            temp.outputStream().use { output ->
                                source.copyTo(output)
                            }
                        }

                        if (temp.length() < 20_000L) {
                            temp.delete()
                            error("File audio terlalu kecil atau kosong.")
                        }

                        if (target.exists() && !target.renameTo(backup)) {
                            temp.delete()
                            error("Tidak dapat mengganti audio lama.")
                        }

                        if (!temp.renameTo(target)) {
                            if (backup.exists()) backup.renameTo(target)
                            temp.delete()
                            error("Audio baru gagal disimpan.")
                        }
                        backup.delete()
                    }
                }

                if (result.isSuccess) {
                    repository.setCustomAdhanAudioSelected(true)
                    customAdhanAudio = true
                }
                permissionHint = if (result.isSuccess) {
                    "Audio pilihan tersimpan. Audio ini akan dipakai otomatis saat waktu sholat."
                } else {
                    "Gagal menyimpan audio: ${
                        result.exceptionOrNull()?.message ?: "format tidak didukung"
                    }"
                }
            }
        }
    }


    DisposableEffect(quranAudio) {
        onDispose { quranAudio.release() }
    }

    fun applyDeviceLocation(fix: Location?) {
        if (fix == null) {
            permissionHint = "Lokasi belum ditemukan. Aktifkan GPS/lokasi, coba lagi, atau masukkan koordinat secara manual."
            return
        }
        val updated = AppLocation("Lokasi perangkat", fix.latitude, fix.longitude)
        repository.saveLocation(updated)
        location = updated
        showLocationDialog = false
        permissionHint = "Lokasi diperbarui. Jadwal sholat dan arah kiblat disesuaikan dengan koordinat perangkat."
    }

    fun resolveDeviceLocation() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = runCatching { manager.getProviders(true) }
            .getOrDefault(emptyList())
            .sortedBy { provider ->
                when (provider) {
                    LocationManager.NETWORK_PROVIDER -> 0
                    LocationManager.GPS_PROVIDER -> 1
                    else -> 2
                }
            }
        val recent = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
        if (recent != null && System.currentTimeMillis() - recent.time < 6 * 60 * 60 * 1000L) {
            applyDeviceLocation(recent)
            return
        }
        if (providers.isEmpty()) {
            applyDeviceLocation(null)
            return
        }

        fun tryProvider(index: Int) {
            if (index >= providers.size) {
                applyDeviceLocation(null)
                return
            }
            val provider = providers[index]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching {
                    manager.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(context)) { fix ->
                        if (fix != null) applyDeviceLocation(fix) else tryProvider(index + 1)
                    }
                }.onFailure { tryProvider(index + 1) }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(fix: Location) {
                        runCatching { manager.removeUpdates(this) }
                        applyDeviceLocation(fix)
                    }

                    @Deprecated("Deprecated in Android")
                    override fun onStatusChanged(providerName: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(providerName: String) = Unit

                    override fun onProviderDisabled(providerName: String) {
                        runCatching { manager.removeUpdates(this) }
                        tryProvider(index + 1)
                    }
                }
                runCatching { manager.requestSingleUpdate(provider, listener, Looper.getMainLooper()) }
                    .onFailure { tryProvider(index + 1) }
            }
        }
        tryProvider(0)
    }

    fun scheduleAdhanEvents() {
        scope.launch {
            isPrayerLoading = true
            val updatedPack = repository.getPrayerPack(location)
            prayerPack = updatedPack
            if (repository.isAdhanEnabled()) PrayerScheduler.scheduleEvents(context, updatedPack.events)
            isPrayerLoading = false
            permissionHint = when {
                updatedPack.events.isEmpty() -> "Jadwal sholat belum tersedia. Periksa koneksi dan lokasi, lalu muat ulang jadwal."
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !PrayerScheduler.canScheduleExactAlarms(context) ->
                    "Adzan aktif dengan alarm cadangan. Aktifkan izin 'Alarm & pengingat' agar waktunya lebih presisi."
                else -> "Adzan otomatis aktif. Pastikan notifikasi diizinkan dan pembatasan baterai tidak memblokir aplikasi."
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            repository.setAdhanEnabled(true)
            adhanEnabled = true
            if (!PrayerScheduler.canScheduleExactAlarms(context)) PrayerScheduler.openExactAlarmSettings(context)
            scope.launch { repository.downloadAdhanIfNeeded() }
            scheduleAdhanEvents()
        } else {
            repository.setAdhanEnabled(false)
            adhanEnabled = false
            PrayerScheduler.cancelSaved(context)
            permissionHint = "Adzan dimatikan karena izin notifikasi ditolak. Kamu bisa mengaktifkannya kembali di Pengaturan."
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) resolveDeviceLocation()
        else permissionHint = "Izin lokasi ditolak. Jadwal tetap bisa diatur lewat kota atau koordinat manual."
    }

    fun requestDeviceLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) resolveDeviceLocation()
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    LaunchedEffect(Unit) {
        surahs = repository.getSurahs()
        dailyAyah = repository.getDailyAyah()
        adhanEnabled = repository.isAdhanEnabled()
        if (repository.isAdhanEnabled()) repository.downloadAdhanIfNeeded()
    }

    LaunchedEffect(location.latitude, location.longitude) {
        isPrayerLoading = true
        prayerPack = repository.getPrayerPack(location)
        if (repository.isAdhanEnabled()) PrayerScheduler.scheduleEvents(context, prayerPack.events)
        isPrayerLoading = false
    }

    LaunchedEffect(selectedBook) {
        hadiths = repository.cachedHadiths(selectedBook, 1)
        hadithStart = 21
        isHadithLoading = true
        val firstPage = repository.getHadiths(selectedBook, 1, 20)
        if (firstPage.isNotEmpty()) hadiths = firstPage
        isHadithLoading = false
    }

    DisposableEffect(lifecycleOwner, location.latitude, location.longitude) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    adhanEnabled = repository.isAdhanEnabled()
                    prayerPack = repository.getPrayerPack(location)
                    if (repository.isAdhanEnabled()) PrayerScheduler.scheduleEvents(context, prayerPack.events)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openSurah(surah: Surah) {
        quranAudio.stop()
        playingGlobalAyah = -1
        selectedSurah = surah
        verses = repository.cachedAyahs(surah.number)
        isVersesLoading = verses.isEmpty()
        tab = AppTab.READER
        scope.launch {
            val loaded = repository.getAyahs(surah.number)
            verses = loaded
            isVersesLoading = false
            repository.saveReadingProgress(surah.number, if (surah.number == repository.getLastReadSurah()) repository.getLastReadAyah() else 1)
        }
    }

    fun testAdhanAudio() {
        val intent = android.content.Intent(context, com.ditzy.nurquran.prayer.AdhanService::class.java).apply {
            putExtra(PrayerScheduler.EXTRA_NAME, "Tes audio")
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }.onSuccess {
            permissionHint = "Tes audio dimulai. Hentikan dari notifikasi jika ingin menghentikannya lebih awal."
        }.onFailure {
            permissionHint = "Tes audio gagal dimulai: ${it.message ?: "periksa izin Android"}"
        }
    }

    fun restoreDefaultAdhanAudio() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { File(context.filesDir, "adhan.ogg").delete() }
                repository.setCustomAdhanAudioSelected(false)
                customAdhanAudio = false
                repository.downloadAdhanIfNeeded()
            }.onSuccess {
                permissionHint = "Audio bawaan dipulihkan jika internet tersedia."
            }.onFailure {
                permissionHint = "Audio bawaan belum dapat dipulihkan: ${it.message ?: "coba lagi"}"
            }
        }
    }

    fun enableAdhan() {
        permissionHint = ""
        adhanEnabled = true
        repository.setAdhanEnabled(true)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            permissionHint = "Izinkan notifikasi agar status dan kontrol adzan tampil dengan benar."
            return
        }
        if (!PrayerScheduler.canScheduleExactAlarms(context)) {
            PrayerScheduler.openExactAlarmSettings(context)
            permissionHint = "Aktifkan izin 'Alarm & pengingat' di pengaturan Android. Alarm cadangan mungkin terlambat."
        }
        scope.launch { repository.downloadAdhanIfNeeded() }
        scheduleAdhanEvents()
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            if (tab in setOf(AppTab.HOME, AppTab.QURAN, AppTab.HADITH, AppTab.PRAYER)) {
                NavigationBar(containerColor = SoftCream, tonalElevation = 8.dp) {
                    val navItems = listOf(
                        Triple(AppTab.HOME, "Beranda", Icons.Filled.Home),
                        Triple(AppTab.QURAN, "Al-Qur’an", Icons.Filled.MenuBook),
                        Triple(AppTab.HADITH, "Hadis", Icons.Filled.CheckCircle),
                        Triple(AppTab.PRAYER, "Sholat", Icons.Filled.AccessTime)
                    )
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item.first,
                            onClick = { tab = item.first },
                            icon = { Icon(item.third, contentDescription = item.second) },
                            label = { Text(item.second, fontSize = 10.sp, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                AppTab.HOME -> HomeScreen(
                    location, prayerPack, dailyAyah, isPrayerLoading, currentTimeMillis,
                    onOpenQuran = { tab = AppTab.QURAN },
                    onOpenHadith = { tab = AppTab.HADITH },
                    onOpenPrayer = { tab = AppTab.PRAYER },
                    onOpenQibla = { tab = AppTab.QIBLA },
                    onOpenSettings = { tab = AppTab.SETTINGS },
                    onOpenLastRead = {
                        val number = repository.getLastReadSurah().coerceIn(1, 114)
                        val surah = surahs.firstOrNull { it.number == number }
                        if (surah != null) openSurah(surah) else tab = AppTab.QURAN
                    },
                    onEnableAdhan = { enableAdhan() }
                )
                AppTab.QURAN -> QuranScreen(
                    surahs = surahs,
                    lastReadSurah = repository.getLastReadSurah(),
                    bookmarks = quranBookmarks,
                    onRefresh = { scope.launch { surahs = repository.getSurahs() } },
                    onOpen = { openSurah(it) },
                    onOpenBookmark = { bookmark ->
                        repository.saveReadingProgress(bookmark.surahNumber, bookmark.ayahNumber)
                        surahs.firstOrNull { it.number == bookmark.surahNumber }?.let { openSurah(it) }
                    },
                    onRemoveBookmark = { bookmark ->
                        repository.toggleQuranBookmark(bookmark.surahNumber, bookmark.ayahNumber)
                        quranBookmarks = repository.getQuranBookmarks()
                    },
                    onBack = { tab = AppTab.HOME },
                    onSettings = { tab = AppTab.SETTINGS }
                )
                AppTab.READER -> ReaderScreen(
                    surah = selectedSurah,
                    verses = verses,
                    loading = isVersesLoading,
                    lastReadSurah = repository.getLastReadSurah(),
                    lastReadAyah = repository.getLastReadAyah(),
                    surahOffset = selectedSurah?.let { chosen -> surahs.filter { it.number < chosen.number }.sumOf { it.ayahCount } } ?: 0,
                    playingGlobalAyah = playingGlobalAyah,
                    bookmarkedAyahs = quranBookmarks.filter { it.surahNumber == selectedSurah?.number }.map { it.ayahNumber }.toSet(),
                    autoPlayRecitation = autoPlayRecitation,
                    onAutoPlayChange = { enabled ->
                        autoPlayRecitation = enabled
                        repository.setAutoPlayRecitationEnabled(enabled)
                    },
                    onBack = { quranAudio.stop(); playingGlobalAyah = -1; tab = AppTab.QURAN },
                    onSaveAyah = { ayah -> selectedSurah?.let { repository.saveReadingProgress(it.number, ayah) }; permissionHint = "Posisi bacaan disimpan." },
                    onToggleBookmark = { ayah ->
                        val current = selectedSurah
                        if (current != null) {
                            repository.toggleQuranBookmark(current.number, ayah)
                            repository.saveReadingProgress(current.number, ayah)
                            quranBookmarks = repository.getQuranBookmarks()
                        }
                    },
                    onPlayAyah = { global ->
                        if (playingGlobalAyah == global) {
                            quranAudio.stop()
                            playingGlobalAyah = -1
                        } else {
                            playingGlobalAyah = -1
                            val activeSurah = selectedSurah
                            if (autoPlayRecitation && activeSurah != null && verses.isNotEmpty()) {
                                val offset = surahs.filter { it.number < activeSurah.number }.sumOf { it.ayahCount }
                                val sequence = verses.map { ayah -> ayah.globalNumber.takeIf { it > 0 } ?: (offset + ayah.numberInSurah) }
                                quranAudio.playSequence(sequence, global, { started -> playingGlobalAyah = started }, { playingGlobalAyah = -1 })
                            } else {
                                quranAudio.play(global, { started -> playingGlobalAyah = started }, { playingGlobalAyah = -1 })
                            }
                        }
                    }
                )
                AppTab.HADITH -> HadithScreen(
                    selectedBook = selectedBook,
                    hadiths = hadiths,
                    loading = isHadithLoading,
                    onSelectBook = { book ->
                        selectedBook = book
                        hadiths = repository.cachedHadiths(book, 1)
                        hadithStart = 21
                    },
                    onRetry = {
                        scope.launch {
                            hadiths = repository.cachedHadiths(selectedBook, 1)
                            hadithStart = 21
                            isHadithLoading = true
                            val firstPage = repository.getHadiths(selectedBook, 1, 20)
                            if (firstPage.isNotEmpty()) hadiths = firstPage
                            isHadithLoading = false
                        }
                    },
                    onLoadMore = {
                        scope.launch {
                            isHadithLoading = true
                            val more = repository.getHadiths(selectedBook, hadithStart, hadithStart + 19)
                            if (more.isNotEmpty()) {
                                hadiths = (hadiths + more).distinctBy { it.number }
                                hadithStart += 20
                            }
                            isHadithLoading = false
                        }
                    }
                )
                AppTab.PRAYER -> PrayerScreen(
                    location = location,
                    prayerPack = prayerPack,
                    loading = isPrayerLoading,
                    nowMillis = currentTimeMillis,
                    adhanEnabled = adhanEnabled,
                    hint = permissionHint,
                    onSetLocation = { showLocationDialog = true },
                    onEnableAdhan = { enableAdhan() },
                    onRefresh = {
                        scope.launch {
                            isPrayerLoading = true
                            prayerPack = repository.getPrayerPack(location)
                            if (repository.isAdhanEnabled()) PrayerScheduler.scheduleEvents(context, prayerPack.events)
                            isPrayerLoading = false
                        }
                    }
                )
                AppTab.QIBLA -> QiblaScreen(location, onBack = { tab = AppTab.HOME }, onSetLocation = { showLocationDialog = true })
                AppTab.SETTINGS -> SettingsScreen(
                    location = location,
                    adhanEnabled = adhanEnabled,
                    customAdhanAudio = customAdhanAudio,
                    hint = permissionHint,
                    onBack = { tab = AppTab.HOME },
                    onSetLocation = { showLocationDialog = true },
                    onToggleAdhan = { enabled ->
                        if (enabled) {
                            enableAdhan()
                        } else {
                            adhanEnabled = false
                            repository.setAdhanEnabled(false)
                            PrayerScheduler.cancelSaved(context)
                            permissionHint = "Adzan otomatis dinonaktifkan. Alarm terjadwal dan audio yang sedang diputar dihentikan."
                        }
                    },
                    onChooseAdhanAudio = {
                        adhanAudioPicker.launch(arrayOf("audio/*"))
                    },
                    onTestAdhanAudio = { testAdhanAudio() },
                    onRestoreDefaultAdhanAudio = { restoreDefaultAdhanAudio() },
                    onOpenExactAlarm = { enableAdhan() }
                )
            }
        }
    }

    if (showLocationDialog) {
        LocationDialog(
            current = location,
            onDismiss = { showLocationDialog = false },
            onUseCurrentLocation = { requestDeviceLocation() },
            onSave = { updated ->
                repository.saveLocation(updated)
                location = updated
                showLocationDialog = false
                permissionHint = "Lokasi diperbarui. Jadwal sholat dan arah kiblat sedang disesuaikan."
            }
        )
    }
}

private fun formatPrayerCountdown(targetMillis: Long, nowMillis: Long): String {
    val remainingMinutes = ((targetMillis - nowMillis).coerceAtLeast(0L) / 60_000L)
    val hours = remainingMinutes / 60L
    val minutes = remainingMinutes % 60L
    return if (hours > 0L) "$hours jam $minutes menit" else "$minutes menit"
}

@Composable
private fun HomeScreen(
    location: AppLocation,
    prayerPack: PrayerPack,
    dailyAyah: DailyAyah?,
    loading: Boolean,
    nowMillis: Long,
    onOpenQuran: () -> Unit,
    onOpenHadith: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLastRead: () -> Unit,
    onEnableAdhan: () -> Unit
) {
    val date = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date()) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.nur_logo),
                contentDescription = "Logo Nur Qur’an",
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NUR QUR’AN", color = Emerald, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                Text("Teman ibadah setiap hari", color = MutedInk, fontSize = 12.sp)
            }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Pengaturan", tint = Emerald) }
        }

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(DeepEmerald, Emerald, Jade)))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Assalamu’alaikum 🌙", color = Color(0xFFFFE6A0), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Tenangkan hati,\ndekatkan diri pada-Nya.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                    Text("${location.name}  ·  $date", color = Color(0xFFE5F0E8), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenLastRead, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepEmerald), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Lanjut membaca", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onEnableAdhan, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .13f), contentColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Adzan", fontSize = 12.sp)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Waktu sholat", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Metode perhitungan Kemenag RI", fontSize = 11.sp, color = MutedInk)
            }
            TextButtonWithIcon("Lihat semua", onOpenPrayer)
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SoftCream), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val next = prayerPack.events.firstOrNull()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (next != null) "Sholat berikutnya · ${next.name}" else "Jadwal sholat hari ini", color = MutedInk, fontSize = 12.sp)
                        Text(if (next != null) SimpleDateFormat("HH:mm", Locale.US).format(Date(next.atMillis)) else "${location.name}", color = Emerald, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        if (next != null) {
                            Text("Dalam ${formatPrayerCountdown(next.atMillis, nowMillis)}", color = Jade, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (loading) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Jade)
                    else Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp))
                }
                Divider(color = Color(0xFFE8E5DA))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val names = listOf("Subuh", "Zuhur", "Asar", "Maghrib", "Isya")
                    names.forEach { name ->
                        val time = prayerPack.today.firstOrNull { it.name == name }?.time ?: "--:--"
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, color = MutedInk, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(time, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Text("Jelajahi ibadah", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureTile("Al-Qur’an", "114 surah", Icons.Filled.MenuBook, Emerald, Modifier.weight(1f), onOpenQuran)
            FeatureTile("Hadis", "Kumpulan hadis", Icons.Filled.CheckCircle, Color(0xFF9A6B17), Modifier.weight(1f), onOpenHadith)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureTile("Arah kiblat", "Kompas langsung", Icons.Filled.Explore, Color(0xFF34785C), Modifier.weight(1f), onOpenQibla)
            FeatureTile("Jadwal sholat", "Pengingat adzan", Icons.Filled.AccessTime, Color(0xFF6C6A9E), Modifier.weight(1f), onOpenPrayer)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ayat pilihan hari ini", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dailyAyah == null) {
                    Text("Sambungkan internet untuk memuat ayat pilihan.", color = MutedInk, fontSize = 13.sp)
                } else {
                    Text(dailyAyah.arabic, color = Emerald, fontSize = 25.sp, textAlign = TextAlign.End, lineHeight = 43.sp, modifier = Modifier.fillMaxWidth())
                    Text(dailyAyah.translation, color = Ink, fontSize = 14.sp, lineHeight = 22.sp)
                    Text("QS. ${dailyAyah.reference}", color = Jade, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Text("Semoga harimu dipenuhi keberkahan. ✨", color = MutedInk, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
    }
}

@Composable
private fun FeatureTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = .11f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
            Text(subtitle, color = MutedInk, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TextButtonWithIcon(text: String, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Jade, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Jade, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun QuranScreen(
    surahs: List<Surah>,
    lastReadSurah: Int,
    bookmarks: List<QuranBookmark>,
    onRefresh: () -> Unit,
    onOpen: (Surah) -> Unit,
    onOpenBookmark: (QuranBookmark) -> Unit,
    onRemoveBookmark: (QuranBookmark) -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, surahs) {
        if (query.isBlank()) surahs else surahs.filter {
            it.nameLatin.contains(query, ignoreCase = true) || it.meaning.contains(query, ignoreCase = true) || it.number.toString() == query.trim()
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Al-Qur’an", "Baca dengan tenang dan pahami maknanya", onBack = onBack, trailing = { IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Pengaturan", tint = Emerald) } })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Cari surah atau nomor…", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Jade) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
        )
        if (surahs.isEmpty()) {
            EmptyPanel("Belum bisa memuat daftar surah", "Periksa koneksi lalu coba muat ulang.", onRefresh)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Emerald), modifier = Modifier.fillMaxWidth().clickable { surahs.firstOrNull { it.number == lastReadSurah }?.let(onOpen) }) {
                        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BookmarkBorder, contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Lanjutkan bacaan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Surah ${surahs.firstOrNull { it.number == lastReadSurah }?.nameLatin ?: "Al-Fatihah"}", color = Color(0xFFDEEBE1), fontSize = 12.sp)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Gold)
                        }
                    }
                }
                if (bookmarks.isNotEmpty()) {
                    item {
                        Text("Ayat tersimpan · ${bookmarks.size}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    items(bookmarks, key = { "bookmark-${it.surahNumber}-${it.ayahNumber}" }) { bookmark ->
                        val savedSurah = surahs.firstOrNull { it.number == bookmark.surahNumber }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PaleGreen)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().clickable { onOpenBookmark(bookmark) }.padding(start = 14.dp, end = 6.dp, top = 11.dp, bottom = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Jade, modifier = Modifier.size(21.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(savedSurah?.nameLatin ?: "Surah ${bookmark.surahNumber}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Ayat ${bookmark.ayahNumber} · ketuk untuk melanjutkan", color = MutedInk, fontSize = 11.sp)
                                }
                                IconButton(onClick = { onRemoveBookmark(bookmark) }) {
                                    Icon(Icons.Filled.BookmarkBorder, contentDescription = "Hapus bookmark", tint = MutedInk)
                                }
                            }
                        }
                    }
                }
                items(filtered, key = { it.number }) { surah -> SurahRow(surah, onClick = { onOpen(surah) }) }
                item { Text("Sumber teks dan terjemahan: Al Quran Cloud API", color = MutedInk, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(top = 5.dp), textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
                Text(surah.number.toString(), color = Emerald, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(surah.nameLatin, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
                Text("${surah.ayahCount} ayat · ${if (surah.revelation.equals("Meccan", true)) "Makkiyah" else "Madaniyah"}", color = MutedInk, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(surah.nameArabic, color = Emerald, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text(surah.meaning, color = MutedInk, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ReaderScreen(
    surah: Surah?,
    verses: List<Ayah>,
    loading: Boolean,
    lastReadSurah: Int,
    lastReadAyah: Int,
    surahOffset: Int,
    playingGlobalAyah: Int,
    bookmarkedAyahs: Set<Int>,
    autoPlayRecitation: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSaveAyah: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onPlayAyah: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(surah?.number, verses.size, lastReadSurah, lastReadAyah) {
        if (surah != null && verses.isNotEmpty() && surah.number == lastReadSurah) {
            listState.scrollToItem(lastReadAyah.coerceIn(1, verses.size))
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(surah?.nameLatin ?: "Baca Al-Qur’an", "Teks Arab · Terjemahan Indonesia", onBack = onBack)
        if (surah != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PaleGreen)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Murottal berurutan", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Putar ayat berikutnya otomatis sampai akhir surah", color = MutedInk, fontSize = 10.sp)
                    }
                    Switch(checked = autoPlayRecitation, onCheckedChange = onAutoPlayChange)
                }
            }
        }
        if (surah == null) {
            EmptyPanel("Pilih surah terlebih dahulu", "Buka daftar Al-Qur’an untuk memilih surah.", onBack)
        } else if (verses.isEmpty() && loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(color = Jade)
                    Spacer(Modifier.height(12.dp))
                    Text("Memuat ayat surah…", color = MutedInk, fontSize = 13.sp)
                }
            }
        } else if (verses.isEmpty()) {
            EmptyPanel("Ayat belum tersedia", "Buka surah ini saat internet tersedia agar teksnya tersimpan.", onBack)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 17.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(DeepEmerald, Emerald, Jade))).padding(20.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("سُورَةُ", color = Gold, fontSize = 14.sp)
                            Text(surah.nameArabic, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                            Text(surah.nameLatin, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("${surah.ayahCount} ayat · ${surah.meaning}", color = Color(0xFFDAE9DF), fontSize = 11.sp)
                            if (surah.number != 1 && surah.number != 9) {
                                Divider(color = Color.White.copy(alpha = .20f), modifier = Modifier.padding(vertical = 2.dp))
                                Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", color = Gold, fontSize = 23.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                items(verses, key = { "${surah.number}:${it.numberInSurah}" }) { ayah ->
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftCream), elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)) {
                        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            val globalAyahNumber = ayah.globalNumber.takeIf { it > 0 } ?: (surahOffset + ayah.numberInSurah)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(34.dp).clip(CircleShape).background(PaleGreen), contentAlignment = Alignment.Center) {
                                    Text(ayah.numberInSurah.toString(), color = Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.weight(1f))
                                if (ayah.numberInSurah == lastReadAyah && surah.number == lastReadSurah) Text("Terakhir dibaca", color = Jade, fontSize = 10.sp)
                                IconButton(onClick = { onPlayAyah(globalAyahNumber) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.VolumeUp, contentDescription = "Putar murottal ayat", tint = if (playingGlobalAyah == globalAyahNumber) Gold else Jade, modifier = Modifier.size(21.dp))
                                }
                                IconButton(onClick = { onToggleBookmark(ayah.numberInSurah) }, modifier = Modifier.size(36.dp)) {
                                    val saved = ayah.numberInSurah in bookmarkedAyahs
                                    Icon(
                                        if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = if (saved) "Hapus bookmark ayat" else "Simpan bookmark ayat",
                                        tint = if (saved) Gold else Jade,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { onSaveAyah(ayah.numberInSurah) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Tandai sebagai posisi terakhir dibaca", tint = Jade, modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                text = ayah.arabic,
                                color = DeepEmerald,
                                fontSize = 27.sp,
                                lineHeight = 48.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Divider(color = Color(0xFFECE9DF))
                            Text(ayah.translation, color = Ink, fontSize = 14.sp, lineHeight = 23.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithScreen(
    selectedBook: String,
    hadiths: List<HadithItem>,
    loading: Boolean,
    onSelectBook: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    val books = listOf("bukhari" to "Bukhari", "muslim" to "Muslim", "tirmidzi" to "Tirmidzi", "abudaud" to "Abu Dawud", "nasai" to "An-Nasa’i", "ibnumajah" to "Ibnu Majah")
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Hadis", "Kumpulan hadis dengan terjemahan Indonesia")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 17.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            books.forEach { (id, label) ->
                val chosen = id == selectedBook
                Box(
                    Modifier.clip(RoundedCornerShape(30.dp))
                        .background(if (chosen) Emerald else SoftCream)
                        .border(1.dp, if (chosen) Emerald else Color(0xFFE0E5DD), RoundedCornerShape(30.dp))
                        .clickable { onSelectBook(id) }
                        .padding(horizontal = 15.dp, vertical = 10.dp)
                ) { Text(label, color = if (chosen) Color.White else Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (hadiths.isEmpty() && loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = Jade) }
        } else if (hadiths.isEmpty()) {
            EmptyPanel("Hadis belum berhasil dimuat", "Periksa koneksi internet lalu coba ulang halaman pertama.", onRetry)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 17.dp, end = 17.dp, top = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(DeepEmerald, Jade))).padding(19.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("HADIS PILIHAN", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                            Text(books.firstOrNull { it.first == selectedBook }?.second ?: "Koleksi hadis", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Baca teks Arab dan terjemahan Indonesia.", color = Color(0xFFE2EEE5), fontSize = 12.sp)
                        }
                    }
                }
                items(hadiths, key = { "$selectedBook-${it.number}" }) { hadith ->
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftCream)) {
                        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("HR. ${books.firstOrNull { it.first == selectedBook }?.second ?: selectedBook} · No. ${hadith.number}", color = Jade, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Filled.BookmarkBorder, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                            }
                            if (hadith.arabic.isNotBlank()) Text(hadith.arabic, color = DeepEmerald, fontSize = 22.sp, lineHeight = 40.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            if (hadith.translation.isNotBlank()) Text(hadith.translation, color = Ink, fontSize = 14.sp, lineHeight = 23.sp)
                        }
                    }
                }
                item {
                    Button(onClick = onLoadMore, modifier = Modifier.fillMaxWidth(), enabled = !loading, shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Emerald)) {
                        if (loading) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("Muat hadis berikutnya")
                    }
                    Text("Data disediakan oleh layanan hadis pihak ketiga; periksa rujukan edisi saat mengutip.", color = MutedInk, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PrayerScreen(
    location: AppLocation,
    prayerPack: PrayerPack,
    loading: Boolean,
    nowMillis: Long,
    adhanEnabled: Boolean,
    hint: String,
    onSetLocation: () -> Unit,
    onEnableAdhan: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Jadwal sholat", "Waktu lokal berdasarkan koordinat lokasi", trailing = {
            IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Perbarui jadwal", tint = Emerald) }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Emerald)) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onSetLocation).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Gold, modifier = Modifier.size(27.dp))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(location.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude), color = Color(0xFFD8E6DC), fontSize = 11.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Atur lokasi", tint = Gold)
                }
            }
            prayerPack.events.firstOrNull()?.let { nextEvent ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Emerald, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Menuju ${nextEvent.name}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${SimpleDateFormat("HH:mm", Locale.US).format(Date(nextEvent.atMillis))} · ${formatPrayerCountdown(nextEvent.atMillis, nowMillis)} lagi", color = Jade, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Jadwal hari ini", color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
                if (loading) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Jade)
            }
            val iconFor = mapOf("Subuh" to "☀", "Zuhur" to "☼", "Asar" to "◒", "Maghrib" to "🌇", "Isya" to "☾")
            val rows = listOf("Subuh", "Zuhur", "Asar", "Maghrib", "Isya")
            rows.forEach { name ->
                val time = prayerPack.today.firstOrNull { it.name == name }?.time ?: "--:--"
                val nextName = prayerPack.events.firstOrNull()?.name
                val isNext = name == nextName
                Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = if (isNext) PaleGreen else SoftCream)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (isNext) Emerald else PaleGold), contentAlignment = Alignment.Center) {
                            Text(iconFor[name] ?: "•", color = if (isNext) Color.White else Emerald, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(if (isNext) "Sholat berikutnya" else "Waktu sholat", color = MutedInk, fontSize = 11.sp)
                        }
                        Text(time, color = Emerald, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftCream)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Gold, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Adzan otomatis", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if (adhanEnabled) "Pengingat dijadwalkan" else "Fitur sedang nonaktif", color = MutedInk, fontSize = 11.sp)
                        }
                    }
                    Text("Aktifkan izin alarm tepat dan notifikasi Android. Beberapa ponsel membatasi alarm ketika mode hemat baterai aktif.", color = MutedInk, fontSize = 12.sp, lineHeight = 18.sp)
                    Button(onClick = onEnableAdhan, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Emerald)) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Atur izin adzan")
                    }
                }
            }
            if (hint.isNotBlank()) Text(hint, color = Jade, fontSize = 12.sp, lineHeight = 18.sp)
            Text("Metode: Kemenag RI (AlAdhan method 20). Jadwal dapat berbeda dari masjid setempat; ikuti jadwal resmi wilayahmu.", color = MutedInk, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 18.dp))
        }
    }
}

@Composable
private fun QiblaScreen(location: AppLocation, onBack: () -> Unit, onSetLocation: () -> Unit) {
    val context = LocalContext.current
    var heading by remember { mutableFloatStateOf(0f) }
    val bearing = remember(location.latitude, location.longitude) { qiblaBearing(location.latitude, location.longitude) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    val declination = remember(location.latitude, location.longitude) {
        GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(), 0f, System.currentTimeMillis()).declination
    }
    val trueHeading = (heading + declination + 360f) % 360f
    val relative = (bearing - trueHeading + 360f) % 360f

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 19.dp, vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader("Arah kiblat", "Kompas mengikuti orientasi ponsel", onBack = onBack)
        Spacer(Modifier.height(12.dp))
        Text("Arah menuju Ka’bah", color = Emerald, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(location.name, color = MutedInk, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = Color(0xFFE7E0CE), radius = radius + 7.dp.toPx(), center = center)
                drawCircle(color = Emerald, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
                drawCircle(color = Gold.copy(alpha = .9f), radius = radius * .79f, center = center, style = Stroke(width = 1.5.dp.toPx()))
                for (angle in 0 until 360 step 30) {
                    val r1 = radius * .86f
                    val r2 = radius * (if (angle % 90 == 0) .73f else .79f)
                    val a = Math.toRadians((angle - 90).toDouble())
                    drawLine(
                        color = if (angle % 90 == 0) Gold else Emerald.copy(alpha = .45f),
                        start = androidx.compose.ui.geometry.Offset(center.x + cos(a).toFloat() * r1, center.y + sin(a).toFloat() * r1),
                        end = androidx.compose.ui.geometry.Offset(center.x + cos(a).toFloat() * r2, center.y + sin(a).toFloat() * r2),
                        strokeWidth = if (angle % 90 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                    )
                }
                val angle = Math.toRadians((relative - 90f).toDouble())
                val end = androidx.compose.ui.geometry.Offset(center.x + cos(angle).toFloat() * radius * .68f, center.y + sin(angle).toFloat() * radius * .68f)
                val tail = androidx.compose.ui.geometry.Offset(center.x - cos(angle).toFloat() * radius * .25f, center.y - sin(angle).toFloat() * radius * .25f)
                drawLine(color = Gold, start = center, end = end, strokeWidth = 8.dp.toPx())
                drawLine(color = Emerald, start = center, end = tail, strokeWidth = 6.dp.toPx())
                drawCircle(color = SoftCream, radius = 9.dp.toPx(), center = center)
                drawCircle(color = Gold, radius = 6.dp.toPx(), center = center)
            }
            Text("N", color = Emerald, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 25.dp))
            Text("S", color = MutedInk, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
            Text("W", color = MutedInk, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 25.dp))
            Text("E", color = MutedInk, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 25.dp))
        }
        Spacer(Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Emerald)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("BEARING KIBLAT", color = Color(0xFFDAE7DD), fontSize = 11.sp, letterSpacing = 1.5.sp)
                Text("${bearing.roundToInt()}° dari utara", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Putar ponsel sampai jarum emas mengarah ke Ka’bah.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(15.dp))
        TextButtonWithIcon("Ubah lokasi", onSetLocation)
        Text("Kalibrasikan kompas dengan menggerakkan ponsel membentuk angka delapan. Jauhkan dari magnet atau casing bermagnet.", color = MutedInk, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
    }
}

private fun qiblaBearing(latitude: Double, longitude: Double): Float {
    val kaabaLat = Math.toRadians(21.4225)
    val kaabaLon = Math.toRadians(39.8262)
    val userLat = Math.toRadians(latitude)
    val deltaLon = kaabaLon - Math.toRadians(longitude)
    val y = sin(deltaLon)
    val x = cos(userLat) * kotlin.math.tan(kaabaLat) - sin(userLat) * cos(deltaLon)
    return ((Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0).toFloat()
}

@Composable
private fun SettingsScreen(
    location: AppLocation,
    adhanEnabled: Boolean,
    customAdhanAudio: Boolean,
    hint: String,
    onBack: () -> Unit,
    onSetLocation: () -> Unit,
    onToggleAdhan: (Boolean) -> Unit,
    onChooseAdhanAudio: () -> Unit,
    onTestAdhanAudio: () -> Unit,
    onRestoreDefaultAdhanAudio: () -> Unit,
    onOpenExactAlarm: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Pengaturan", "Atur lokasi dan pengingat ibadah", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftCream)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Lokasi perhitungan", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(location.name, color = Emerald, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(String.format(Locale.US, "Latitude %.5f · Longitude %.5f", location.latitude, location.longitude), color = MutedInk, fontSize = 12.sp)
                    Button(onClick = onSetLocation, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Emerald), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Ubah kota / koordinat")
                    }
                }
            }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftCream)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Adzan otomatis", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Putar audio pada waktu sholat", color = MutedInk, fontSize = 12.sp)
                        }
                        Switch(checked = adhanEnabled, onCheckedChange = onToggleAdhan)
                    }
                    Divider(color = Color(0xFFE5E5DA))
                    Text("Audio adzan pilihan", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (customAdhanAudio) {
                            "Audio pilihan tersimpan di aplikasi dan akan diputar otomatis saat waktu sholat. Pastikan file yang dipilih memang rekaman Jiharkah."
                        } else {
                            "Audio bawaan adalah rekaman adzan umum, bukan rekaman yang diverifikasi sebagai Jiharkah. Impor rekaman Jiharkah dari HP agar dipakai otomatis."
                        },
                        color = MutedInk, fontSize = 12.sp, lineHeight = 18.sp
                    )
                    Button(
                        onClick = onChooseAdhanAudio,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Jade)
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih audio Jiharkah dari HP")
                    }
                    OutlinedButton(
                        onClick = onTestAdhanAudio,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Emerald)
                        Spacer(Modifier.width(8.dp))
                        Text("Tes audio adzan", color = Emerald)
                    }
                    if (customAdhanAudio) {
                        TextButton(onClick = onRestoreDefaultAdhanAudio, modifier = Modifier.fillMaxWidth()) {
                            Text("Kembalikan audio bawaan", color = Jade)
                        }
                    }
                    Text("Untuk adzan yang lebih presisi, izinkan notifikasi dan 'Alarm & pengingat' di Android. Kamu bisa mematikan fitur ini kapan saja.", color = MutedInk, fontSize = 12.sp, lineHeight = 18.sp)
                    if (hint.isNotBlank()) Text(hint, color = Jade, fontSize = 12.sp, lineHeight = 18.sp)
                    TextButton(onClick = onOpenExactAlarm) { Text("Buka pengaturan izin adzan", color = Jade) }
                }
            }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Tentang Nur Qur’an", color = Emerald, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Versi 1.3.0 · Android Kotlin + Jetpack Compose", color = Ink, fontSize = 12.sp)
                    Text("Teks Qur’an dan terjemahan: Al Quran Cloud API. Waktu sholat: AlAdhan API. Hadis: Hadith API gading.dev. Data online disimpan sebagai cache setelah berhasil dimuat.", color = MutedInk, fontSize = 11.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun LocationDialog(
    current: AppLocation,
    onDismiss: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSave: (AppLocation) -> Unit
) {
    var name by remember(current) { mutableStateOf(current.name) }
    var latitude by remember(current) { mutableStateOf(current.latitude.toString()) }
    var longitude by remember(current) { mutableStateOf(current.longitude.toString()) }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur lokasi", color = Emerald, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Gunakan lokasi perangkat dengan izin Android, pilih kota cepat, atau masukkan koordinat sendiri.", fontSize = 12.sp, color = MutedInk, lineHeight = 18.sp)
                Button(
                    onClick = onUseCurrentLocation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Gunakan lokasi perangkat")
                }
                val presets = listOf(
                    AppLocation("Jakarta", -6.2088, 106.8456),
                    AppLocation("Bandar Lampung", -5.3971, 105.2668),
                    AppLocation("Bandung", -6.9175, 107.6191),
                    AppLocation("Yogyakarta", -7.7956, 110.3695),
                    AppLocation("Surabaya", -7.2575, 112.7521),
                    AppLocation("Medan", 3.5952, 98.6722),
                    AppLocation("Makassar", -5.1477, 119.4327),
                    AppLocation("Denpasar", -8.6500, 115.2167)
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    presets.forEach { city ->
                        val selected = name == city.name
                        Box(Modifier.clip(RoundedCornerShape(25.dp)).background(if (selected) Emerald else PaleGreen)
                            .clickable { name = city.name; latitude = city.latitude.toString(); longitude = city.longitude.toString() }
                            .padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Text(city.name, color = if (selected) Color.White else Emerald, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kota") }, singleLine = true)
                OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Latitude") }, singleLine = true)
                OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Longitude") }, singleLine = true)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                val lat = latitude.trim().replace(',', '.').toDoubleOrNull()
                val lon = longitude.trim().replace(',', '.').toDoubleOrNull()
                if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                    error = "Koordinat tidak valid. Latitude -90..90, longitude -180..180."
                } else onSave(AppLocation(name.trim().ifBlank { "Lokasi saya" }, lat, lon))
            }, colors = ButtonDefaults.buttonColors(containerColor = Emerald)) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = MutedInk) } },
        containerColor = SoftCream
    )
}

@Composable
private fun ScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().padding(start = 9.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Emerald) }
        Column(Modifier.weight(1f).padding(start = if (onBack == null) 10.dp else 0.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Emerald, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MutedInk, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

@Composable
private fun EmptyPanel(title: String, subtitle: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp).clip(RoundedCornerShape(24.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Emerald, modifier = Modifier.size(31.dp))
        }
        Spacer(Modifier.height(15.dp))
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = MutedInk, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onRetry) { Icon(Icons.Filled.Refresh, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Coba lagi") }
    }
}
