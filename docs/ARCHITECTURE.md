# Architecture

## Layers

- `ui/`: Compose navigation, presentation screens, and theme. UI delegates work to repositories; it does not make raw HTTP calls.
- `data/ApiServices.kt`: Retrofit services for the three remote providers. HTTP body logging is deliberately disabled.
- `data/AppRepositories.kt`: request orchestration, structural validation, paging limits, cache fallbacks, and domain mapping.
- `data/HadithJsonParser.kt`: explicit parser and rejection policy for book list / paginated hadith payloads.
- `domain/`: deterministic logic for time parsing, next prayer selection, Quran structural alignment, Qibla bearing, and reminder scheduling.
- `storage/JsonDiskCache.kt`: private app-files JSON cache; writes only data already validated by repository/parser policy.
- `storage/SettingsStore.kt`: Preferences DataStore for theme, bookmarks, last read, and reminder preference.

## Data flow

1. A screen requests data through `AppContainer`'s repository.
2. Repository attempts the upstream API with Retrofit timeouts.
3. It validates shape, IDs/order, counts, and required fields before caching.
4. On network/parse failure it returns the last valid cached entry where one exists, otherwise surfaces an error to the UI.
5. UI labels cached/fallback content where the current screen has this information available.

## Reliability boundaries

- The Quran catalog structural check expects 114 ordered surahs and 6,236 total ayat. A count/sequence check does not establish the textual correctness of every verse; the app relies on upstream edition data and verifies matching ayah indices between editions.
- Hadith list/page parsing checks structural consistency, but does not independently authenticate narrations, grades, or sanad.
- Prayer timings depend on the upstream result, device date/time zone, location and AlAdhan calculation method. Method ID `20` is sent as requested; users should verify local practice/corrections with trusted local authorities.
- Qibla is calculated relative to true north from the supplied coordinates. The UI does not read a magnetometer and cannot show live device heading.
- Reminder notifications are simple local alarms. Boot/timezone changes and automatic scheduling for tomorrow are not implemented in this version.
