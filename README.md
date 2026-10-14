# NurQuran Ultimate — Android source

**Status: source project created; Android build and APK signing have not yet been verified.** See [`docs/QA_CHECKLIST.md`](docs/QA_CHECKLIST.md) and [`docs/RESEARCH_LOG.md`](docs/RESEARCH_LOG.md) for precise status and evidence.

NurQuran Ultimate is a native Android app scaffold built with Kotlin and Jetpack Compose. It integrates remote Quran text/Indonesian translation, paginated hadith data, AlAdhan prayer times (method `20`), a mathematical Qibla bearing, local settings, a private JSON cache, and notification-only prayer reminders.

## Current scope

- Quran: request a validated 114-surah catalog, then load Arabic `quran-uthmani` and Indonesian `id.indonesian` editions for the selected surah. Valid responses are cached for repeat/offline reading. Initial content requires internet access.
- Hadith: the app requests a book list and pages from `hadis-api-id`; it refuses empty/mismatched page payloads and caches validated pages.
- Prayer times: AlAdhan API method 20, with a manually entered location or best-effort last-known device coordinates. The default city/coordinates are Jakarta and are labelled as the initial default—not as detected GPS.
- Reminders: local notifications only. They do **not** play audio adhan. Reminders default off; exact alarms may be unavailable on Android 12+ and use an inexact fallback. The current implementation schedules upcoming times for the supplied day only; reopen the app each day to refresh them.
- Qibla: spherical initial bearing from true north. The screen is a visual bearing aid, **not** a compass sensor/calibrated live direction.
- Privacy: no account or analytics SDK is wired in; API logging is disabled, backup/data extraction is disabled, and the cache is in app-private storage. Location is only used when the user requests a prayer timetable. Review [`PRIVACY.md`](PRIVACY.md).
- Doa/dzikir: the screen explicitly explains the collection has not been loaded because a suitable source/license has not yet been verified. It does not fabricate devotional text.

Not implemented in this initial source delivery: adhan/murottal audio, the SAF-based Jiharkah/audio-file picker, live sensor compass, a bundled offline Quran database/custom bundled Arabic font, a Room corpus database, WorkManager-based deferred work, reboot/timezone/next-day reminder rescheduling, and a populated vetted doa/dzikir collection. Do not consider those features complete.

## Build locally

Requirements: JDK 17, Android SDK platform/build-tools API 36, and Gradle 8.13. The project archive has a **bootstrap `gradlew` shim, not the official Gradle Wrapper JAR**, because the authoring environment could not retrieve the wrapper artifacts. Once Gradle is installed, create the standard wrapper as follows:

```sh
gradle wrapper --gradle-version 8.13 --gradle-distribution-sha256-sum 20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78 --distribution-type bin
chmod +x gradlew
./gradlew --version
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Debug APK (after a successful build): `app/build/outputs/apk/debug/app-debug.apk`.

### Termux (Android phone)

```sh
pkg update
pkg install git gh python openjdk-17 gradle unzip -y
cd ~/NurQuranUltimate
gradle wrapper --gradle-version 8.13 --gradle-distribution-sha256-sum 20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78 --distribution-type bin
chmod +x gradlew
./gradlew testDebugUnitTest assembleDebug
```

Android SDK packages are large and not always straightforward to install in Termux. The recommended build path is the included GitHub Actions workflow, which sets up a hosted Android SDK. Do not assume a local Termux build works until it completes successfully on your device.

## GitHub Actions / APK artifacts

`.github/workflows/android.yml` creates a standard wrapper in CI, validates it, installs Android API 36, runs unit tests and lint, and attempts a debug build. The debug APK and test reports are uploaded as an Actions artifact when files exist. An optional separate job builds a release APK only when all four signing secrets are present:

- `ANDROID_KEYSTORE_BASE64` — base64 of a private release keystore
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`

Never commit the keystore, passwords, or decoded signing file. Configure secrets in the repository's **Settings → Secrets and variables → Actions**. The workflow verifies the APK signature and writes a SHA-256 checksum before artifact upload. A workflow file being present is not proof that a CI run or APK build has passed; only the Actions run logs and artifacts establish that.

## Tests

Unit tests are provided for time parsing/scheduling logic, Qibla bearing, structural Quran validation, hadith JSON/page parsing, page de-duplication logic, a synthetic Retrofit pagination contract, and cache write policy. Fixtures are synthetic strings and do not embed actual Quran verses or hadith. These tests have **not run in this authoring environment** because Gradle/Android SDK are unavailable and external DNS retrieval was blocked. See `docs/TESTING.md`.

## Sources

See [`docs/RESEARCH_LOG.md`](docs/RESEARCH_LOG.md) for the reviewed API documentation and the difference between web documentation review and runtime tests. Sources are consumed remotely; this repository does not bundle or redistribute their datasets.
