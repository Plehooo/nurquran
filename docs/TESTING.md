# Testing status

## Automated test source included

- `QiblaCalculatorTest`: known Jakarta bearing within tolerance, normalized compass range, and coordinate bounds.
- `PrayerTimeLogicTest`: time extraction/normalization, rejection of impossible times, next future prayer, five-prayer completeness, and date path formatting.
- `QuranIntegrityTest`: synthetic catalog structure and ayah-index alignment checks. The fixture uses deliberately synthetic labels; it is not Quranic source data.
- `HadithJsonParserTest`: synthetic book array/envelope, page parsing, empty page rejection, and requested-page mismatch rejection.
- `HadithPagingLogicTest`: known/unknown final-page behavior, duplicate suppression on retry, and malformed empty-item rejection.
- `HadithServiceContractTest`: a MockWebServer fixture verifies that Retrofit includes explicit `page` and `limit` query parameters and that the synthetic page parses. This is a fixture contract, **not** a live provider test.
- `CacheWritePolicyTest`: refuses null/empty/unvalidated payloads.

## Execution status

All JUnit/MockWebServer test files are **written but unexecuted** in the authoring environment. A separate minimal Kotlin harness has compiled and executed only pure core production files with stub models; it does not compile Compose, repository/network code, or Android classes. Gradle and Android SDK are unavailable locally and DNS retrieval was blocked, so no full Android compile/test/lint result is claimed. Run in the GitHub Actions workflow or with a working Android SDK:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Expected test report location after a successful Gradle run: `app/build/reports/tests/testDebugUnitTest/index.html`.

## Manual device acceptance required

1. Clean install on API 24+ and a recent Android version; check launch, splash, back behavior, navigation, light/dark mode and font sizes.
2. Fetch Quran catalog and several surahs; verify Arabic shaping/RTL, translation alignment, final ayah count, retry behavior, bookmarks and cache fallback after network is disabled.
3. Load first/next hadith page for every provider book available; inspect live JSON field mapping and errors/empty pages.
4. Compare prayer timings for a chosen Indonesian city and date with a trusted local schedule. Test permission denied, manually entered location, and location unavailable.
5. Test notification permission denial/grant; test exact-alarm permission denied/granted, app force-stop behavior, power saver, and next-day scheduling limitations.
6. Check Qibla values independently at multiple coordinates and ensure the app does not claim hardware compass direction.
7. Confirm cache clear and uninstall data behavior, verify no API payload/token/coordinates are emitted in logs, and review upstream license requirements.
8. After release signing secrets are added, download the release artifact, verify signature again, and compare published checksum.
