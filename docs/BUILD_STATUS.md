# Build and source verification report

**Snapshot:** 14 October 2026, Asia/Jakarta.

## Verified in this workspace

- Source files and project structure were created in this archive.
- Pure Kotlin core logic (`QiblaCalculator`, `PrayerTimeLogic`, `QuranIntegrity`, and `CacheWritePolicy`) compiled separately with lightweight test-model stubs and the independent assertion harness printed `CORE_LOGIC_CHECKS_PASSED`.
- Canonical 114-surah ayah-count fixtures total 6,236 and passed the structural validator; missing/changed count cases were rejected.
- Static verification completed before packaging: 5 XML resource/manifest files parsed; `gradle/libs.versions.toml` parsed; workflow YAML parsed with literal `on` and both expected jobs present; all 24 Gradle version-catalog accessor references in the build scripts resolve; no trailing whitespace or obvious credential patterns were found in tracked source/config files.
- A pure Kotlin harness compiled and executed the four core logic components plus `HadithPagingLogic` against lightweight test models; it checks Jakarta Qibla bearing, time parsing, Sunrise exclusion from next-prayer selection, the 6,236 ayah-count structure and rejection of malformed catalogs, cache write policy, and page de-duplication/end-of-pagination behavior. It printed `CORE_LOGIC_CHECKS_PASSED`.
- A parser-oriented Kotlin compiler attempt found no explicit syntax/parser diagnostics, but the compilation exited nonzero because Android, AndroidX/Compose, Retrofit, Gson, Coroutines, and JUnit dependencies are not present in this workspace. This is **not** a whole-app compile pass.

## Not verified

- Full Android Gradle build, Kotlin/Compose compilation, official JUnit run, lint, APK creation and APK signing.
- `gradle` and Android SDK/`adb` were not installed in the authoring workspace. Direct shell API checks failed during DNS lookup, so that failure is not proof that provider websites are down.
- The GitHub workflow has been authored but has **not been pushed or run**. No GitHub repository is connected in this workspace.
- Device UI, accessibility, location prompts, live provider JSON, notification delivery and prayer timetable correctness were not tested on an Android device.

For reproducible verification, use the included `.github/workflows/android.yml`, inspect its job logs, and complete `QA_CHECKLIST.md` before publishing.
