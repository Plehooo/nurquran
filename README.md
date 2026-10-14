# Nur Qur’an — Android Native

Aplikasi ibadah Android berbasis Kotlin + Jetpack Compose. Desain menggunakan hijau zamrud, krem, dan emas; bukan aplikasi web/HTML.

## Fitur

- **Al-Qur’an:** daftar 114 surah, teks Arab Utsmani, terjemahan Indonesia, pencarian surah, posisi bacaan terakhir, cache surah yang pernah dibuka, serta audio murottal per ayat.
- **Hadis:** beberapa koleksi populer (Bukhari, Muslim, Tirmidzi, Abu Dawud, An-Nasa’i, Ibnu Majah), teks Arab dan terjemahan Indonesia, serta pemuatan bertahap.
- **Jadwal sholat:** Subuh, Zuhur, Asar, Maghrib, dan Isya berdasarkan koordinat dengan metode Kemenag RI (AlAdhan method 20).
- **Adzan otomatis:** penjadwalan alarm, audio adzan, notifikasi, penjadwalan ulang setelah ponsel dinyalakan, serta opsi menyalakan/mematikan fitur.
- **Kiblat:** perhitungan bearing menuju Ka’bah dan kompas menggunakan sensor rotasi perangkat.
- **Lokasi:** dapat diganti melalui nama kota dan koordinat latitude/longitude. Aplikasi tidak meminta izin lokasi karena koordinat dimasukkan pengguna.
- **Brand/UI:** ikon aplikasi dan ilustrasi logo khusus, kartu konten, navigasi bawah, dan layout ramah ponsel.

## Build APK melalui Termux → GitHub Actions

Build Android SDK dilakukan di GitHub Actions, jadi Termux tidak perlu memasang Android SDK lengkap.

1. Unduh ZIP proyek ke folder Downloads Android, lalu ekstrak dari Termux:

   ```bash
   termux-setup-storage
   pkg update
   pkg install git gh curl unzip openjdk-17 -y
   cd ~/storage/downloads
   unzip NurQuran-Termux-GitHub.zip -d ~/
   cd ~/NurQuran
   chmod +x gradlew termux-push.sh
   gh auth login
   ```

2. Buat repository kosong bernama `nurquran` di GitHub (jangan centang opsi README/license karena proyek ini sudah memilikinya). Kemudian push dengan script:

   ```bash
   ./termux-push.sh https://github.com/Plehooo/nurquran.git
   ```

   Ganti `Plehooo` bila username GitHub-mu berbeda. Script menjalankan `git init` bila diperlukan, membuat commit, lalu push ke branch `main`.

   Cara manual yang setara, bila dibutuhkan:

   ```bash
   git init
   git branch -M main
   git add .
   git commit -m "Initial Nur Quran Android app"
   git remote add origin https://github.com/Plehooo/nurquran.git
   git push -u origin main
   ```

3. Di GitHub buka **Actions → Build Nur Qur'an APK**. Tunggu sampai workflow selesai.
4. Buka run workflow yang hijau, lalu unduh artifact **NurQuran-debug-apk**. Di dalamnya ada `app-debug.apk`; pindahkan ke HP dan instal.

`app-debug.apk` adalah build debug untuk pengujian/sideload, bukan APK release bertanda tangan untuk distribusi Play Store. Workflow otomatis berjalan ketika ada push ke `main`/`master`, dan juga dapat dijalankan melalui **Run workflow**.

## Build lokal (opsional)

`./gradlew assembleDebug` membutuhkan Android SDK Platform 35 dan Android Build Tools 35.0.0. Di lingkungan Termux biasa, gunakan GitHub Actions di atas jika SDK belum terpasang.

## Izin dan perilaku alarm

- Pada Android 13+, izinkan **notifikasi**.
- Pada Android 12+, buka **pengaturan alarm tepat** melalui tombol *Atur izin adzan*. Jika izin tidak tersedia, aplikasi mencoba alarm inexact yang dapat tertunda oleh sistem.
- Beberapa perangkat menghentikan aplikasi latar belakang melalui penghemat baterai. Izinkan aktivitas latar belakang untuk aplikasi ini jika alarm tidak berbunyi.
- Audio adzan diunduh satu kali ketika internet tersedia. Jika gagal diunduh, pemutar memakai suara alarm bawaan perangkat sebagai fallback.
- Jadwal API dapat berbeda beberapa menit dari jadwal masjid atau otoritas setempat. Cocokkan waktu dengan jadwal resmi di wilayahmu.

## API dan atribusi

- Al-Qur’an, terjemahan, dan audio murottal: [Al Quran Cloud API](https://alquran.cloud/api). Data teks/terjemahan dimuat dari layanan dan cache lokal dibuat untuk surah yang pernah dibuka.
- Waktu sholat: [AlAdhan Prayer Times API](https://aladhan.com/prayer-times-api), metode 20 (Kementerian Agama Indonesia).
- Hadis berbahasa Indonesia: [gadingnst/hadith-api](https://github.com/gadingnst/hadith-api), yang mencantumkan tafsirq.com sebagai salah satu sumber data. Verifikasi edisi/rujukan saat mengutip hadis untuk keperluan resmi.
- Audio adzan: [Beautiful adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg), karya Adam-synagda di Wikimedia Commons, dilisensikan CC0 1.0 oleh pengunggah. File diunduh saat aplikasi berjalan dan tidak disertakan sebagai file audio di repositori.
- Logo aplikasi dibuat khusus untuk proyek Nur Qur’an.

## Struktur proyek

```text
NurQuran/
├── app/src/main/java/com/ditzy/nurquran/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── IslamicRepository.kt
│   │   └── QuranAudioController.kt
│   ├── prayer/PrayerAlarm.kt
│   └── ui/NurQuranTheme.kt
├── .github/workflows/android.yml
├── app/build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```
