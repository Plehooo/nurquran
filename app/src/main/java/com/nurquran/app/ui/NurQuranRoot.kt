package com.nurquran.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import com.nurquran.app.AppContainer
import com.nurquran.app.ui.screens.AboutScreen
import com.nurquran.app.ui.screens.BookmarksScreen
import com.nurquran.app.ui.screens.DoaScreen
import com.nurquran.app.ui.screens.HadithScreen
import com.nurquran.app.ui.screens.HomeScreen
import com.nurquran.app.ui.screens.MoreScreen
import com.nurquran.app.ui.screens.PrayerScreen
import com.nurquran.app.ui.screens.QiblaScreen
import com.nurquran.app.ui.screens.QuranReaderScreen
import com.nurquran.app.ui.screens.QuranScreen
import com.nurquran.app.ui.screens.SettingsScreen
import com.nurquran.app.ui.theme.NurQuranTheme
import kotlinx.coroutines.launch

private data class NavTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun NurQuranRoot(container: AppContainer) {
    val themeMode by container.settings.themeMode.collectAsState(initial = "system")
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) { "dark" -> true; "light" -> false; else -> systemDark }
    var currentScreen by remember { mutableStateOf("Beranda") }
    var selectedSurah by remember { mutableStateOf<Int?>(null) }
    val bookmarks by container.settings.bookmarks.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val tabs = remember {
        listOf(
            NavTab("Beranda", "Beranda", Icons.Default.Home),
            NavTab("Al-Qur'an", "Al-Qur’an", Icons.Default.MenuBook),
            NavTab("Hadis", "Hadis", Icons.Default.AutoStories),
            NavTab("Sholat", "Sholat", Icons.Default.Schedule),
            NavTab("Lainnya", "Lainnya", Icons.Default.Apps)
        )
    }

    NurQuranTheme(dark = dark) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        val selected = when (currentScreen) {
                            "Beranda", "Al-Qur'an", "Hadis", "Sholat" -> currentScreen == tab.route
                            else -> tab.route == "Lainnya"
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = tab.route; if (tab.route != "Al-Qur'an") selectedSurah = null },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentScreen) {
                    "Beranda" -> HomeScreen(
                        container,
                        onNavigate = { currentScreen = it; if (it != "Al-Qur'an") selectedSurah = null },
                        onOpenLastRead = { number -> selectedSurah = number; currentScreen = "Al-Qur'an" }
                    )
                    "Al-Qur'an" -> if (selectedSurah == null) {
                        QuranScreen(container) { selectedSurah = it }
                    } else {
                        QuranReaderScreen(
                            container = container,
                            number = selectedSurah!!,
                            bookmarks = bookmarks,
                            onBack = { selectedSurah = null },
                            onToggleBookmark = { ref -> scope.launch { container.settings.toggleBookmark(ref, bookmarks) } }
                        )
                    }
                    "Hadis" -> HadithScreen(container)
                    "Sholat" -> PrayerScreen(container)
                    "Lainnya" -> MoreScreen(onNavigate = { currentScreen = it })
                    "Kiblat" -> QiblaScreen(container)
                    "Doa & Dzikir" -> DoaScreen()
                    "Bookmark" -> BookmarksScreen(container) { number -> selectedSurah = number; currentScreen = "Al-Qur'an" }
                    "Pengaturan" -> SettingsScreen(
                        container = container,
                        themeMode = themeMode,
                        onThemeChange = { value -> scope.launch { container.settings.setTheme(value) } },
                        onNavigate = { currentScreen = it }
                    )
                    "Tentang" -> AboutScreen()
                    else -> MoreScreen(onNavigate = { currentScreen = it })
                }
            }
        }
    }
}
