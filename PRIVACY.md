# Privacy notes

NurQuran Ultimate does not currently implement account registration, an analytics SDK, advertising SDK, or a first-party tracking endpoint.

- **Network requests:** Quran text/translation is requested from `api.alquran.cloud`; hadith books/pages are requested from `hadis-api-id.vercel.app`; prayer timings are requested from `api.aladhan.com`.
- **Location:** the prayer page can request Android location permission to read a last-known location. The user can also keep/change a location manually. The initial Jakarta coordinates are a default and must not be interpreted as detected location. Coordinates are sent to AlAdhan only as part of a prayer timetable request.
- **Local data:** validated API responses and preferences/bookmarks are stored in app-private storage/DataStore. Android backup and data extraction are disabled in the manifest resources. Clearing API cache removes cached API data, not all preferences.
- **Logs:** HTTP request/response body logging is disabled. Debug-only StrictMode policies are enabled to help flag accidental main-thread work.
- **Reminders:** local notification reminders require user opt-in. Android notification and exact-alarm controls remain under the user's system settings. No audio adhan is implemented.
- **Retention and deletion:** users can clear the API cache in settings or uninstall the application to remove app-private data. The upstream APIs have their own processing and privacy terms; review them before release.

This is an engineering note, not a legal assessment. Before publishing, add a contact/owner identity, final privacy policy URL, data safety declaration, and verify permissions and upstream terms for the actual release build.
