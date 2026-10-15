# Nur Qur’an — Android Native

Aplikasi ibadah Android native berbasis Kotlin + Jetpack Compose. Desain memakai hijau zamrud, krem, dan emas; bukan aplikasi web/HTML.

## Fitur

- **Al-Qur’an:** daftar 114 surah, teks Arab Utsmani, terjemahan Indonesia, pencarian surah, posisi bacaan terakhir, cache lokal, dan murottal per ayat.
- **Ngaji lebih nyaman:** putar murottal berurutan sampai akhir surah, bookmark ayat tersimpan secara lokal, daftar ayat tersimpan, dan lanjut kembali ke ayat yang ditandai.
- **Hadis:** koleksi populer (Bukhari, Muslim, Tirmidzi, Abu Dawud, An-Nasa’i, Ibnu Majah), teks Arab, terjemahan Indonesia, serta pemuatan bertahap.
- **Jadwal sholat:** Subuh, Zuhur, Asar, Maghrib, dan Isya memakai koordinat serta metode Kemenag RI (AlAdhan method 20), dengan hitung mundur menuju waktu berikutnya.
- **Adzan otomatis:** pengaktifan dengan izin Android, notifikasi kontrol audio, alarm tepat bila izin tersedia, jadwal yang diperbarui setelah adzan dan saat perangkat menyala/perubahan zona waktu, serta fallback saat layanan/jaringan gagal.
- **Audio Jiharkah:** impor rekaman Jiharkah milikmu dari penyimpanan HP satu kali; file disimpan privat, bisa dites langsung, dan dipakai otomatis pada waktu sholat. Audio bawaan umum tidak diklaim sebagai Jiharkah.
- **Kiblat:** perhitungan bearing menuju Ka’bah dan kompas dengan sensor perangkat.
- **Lokasi:** gunakan lokasi perangkat setelah izin diberikan, atau isi kota/koordinat manual. Tidak ada pelacakan latar belakang lokasi.
- **Cache:** teks dan jadwal yang berhasil dimuat disimpan agar tetap tersedia saat koneksi terbatas.

## Termux → GitHub Actions → APK

> Untuk memperbarui repository `Plehooo/nurquran` yang sudah ada, gunakan metode overlay di bawah. Ini mengganti file yang memang diperbarui saja, tidak menghapus file lain, dan tidak memakai `git push --force`.

### 1. Siapkan repository di Termux

```bash
pkg update
pkg install git gh unzip curl openjdk-17 -y
termux-setup-storage
gh auth login
cd ~
git clone https://github.com/Plehooo/nurquran.git NurQuran
cd ~/NurQuran
```

Kalau `~/NurQuran` sudah merupakan hasil `git clone`, langsung masuk ke folder itu lalu jalankan `git pull --rebase origin main` sebelum menimpa file.

### 2. Terapkan patch

Salin `NurQuran-Patch.zip` ke folder Downloads HP, lalu jalankan:

```bash
cd ~/NurQuran
unzip -o ~/storage/downloads/NurQuran-Patch.zip -d .
chmod +x gradlew termux-push.sh
./termux-push.sh https://github.com/Plehooo/nurquran.git
```

Script hanya menambahkan file yang telah diperbarui ke commit, mengambil riwayat remote terbaru, mencoba rebase, lalu push ke branch aktif. Jika terjadi konflik rebase, jangan hapus file secara massal; periksa `git status` dan selesaikan konflik sebelum melanjutkan.

### 3. Ambil APK

Buka GitHub `Plehooo/nurquran` → **Actions** → workflow **Build Nur Qur'an APK** → pilih run terbaru. Unduh artifact **NurQuran-debug-apk** dan instal `app-debug.apk` di HP.

Build APK debug terjadi di GitHub Actions, jadi Android SDK lengkap tidak wajib dipasang di Termux. Untuk build lokal, jalankan `./gradlew assembleDebug` setelah Android SDK Platform 35 dan Build Tools 35.0.0 tersedia.

## Izin dan catatan alarm

- Android 13+: berikan izin notifikasi.
- Android 12+: izinkan **Alarm & pengingat** agar alarm lebih presisi. Jika izin tidak tersedia, sistem mencoba alarm yang dapat tertunda.
- Izinkan aktivitas latar belakang untuk Nur Qur’an bila ponsel membatasi aplikasi secara agresif lewat penghemat baterai.
- Jadwal API dapat berbeda beberapa menit dari masjid atau otoritas setempat; cocokkan dengan jadwal resmi wilayahmu.
- Jika memakai audio Jiharkah tertentu, pilih file yang benar-benar merupakan rekaman Jiharkah. Aplikasi tidak mengambil atau membundel rekaman berhak cipta dari sumber yang tidak jelas izinnya.

## API dan atribusi

- Al-Qur’an, terjemahan, dan audio murottal: [Al Quran Cloud API](https://alquran.cloud/api).
- Waktu sholat: [AlAdhan Prayer Times API](https://aladhan.com/prayer-times-api), metode 20 (Kementerian Agama Indonesia). Dokumentasi menyatakan waktu dari API dapat berbeda dari otoritas lokal: [metode perhitungan](https://aladhan.com/calculation-methods).
- Hadis Indonesia: [gadingnst/hadith-api](https://github.com/gadingnst/hadith-api) dengan fallback [hadis-api-id](https://github.com/renomureza/hadis-api-id).
- Audio adzan bawaan: [Beautiful adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg), diunggah Adam-synagda di Wikimedia Commons dengan lisensi CC0 1.0. Rekaman ini adalah audio adzan umum, bukan klaim bahwa ia khusus Jiharkah.

## Struktur inti

```text
app/src/main/java/com/ditzy/nurquran/
├── MainActivity.kt
├── data/
│   ├── IslamicRepository.kt
│   └── QuranAudioController.kt
├── prayer/PrayerAlarm.kt
└── ui/NurQuranTheme.kt
.github/workflows/android.yml
termux-push.sh
```
