# Research log and evidence boundaries

**Review date:** 14 October 2026 (Asia/Jakarta).

This log differentiates reviewing provider documentation in a web browser from issuing a runtime request from the build shell. Provider schemas can change; the app must still be verified on CI/device before release.

## Provider documentation reviewed

1. **AlQuran Cloud** — https://alquran.cloud/api
   - Documented API root: `https://api.alquran.cloud/v1/`.
   - Documented patterns include `GET /surah` and `GET /surah/{number}/{edition}`.
   - The app requests the `quran-uthmani` and `id.indonesian` editions, then checks surah/ayah counts and ayah index alignment.
   - Documentation review does not certify all verse text, typography, waqf marks, tajwid, or translation rights. The code performs structure/index checks, not a scholarly text collation.

2. **Hadis API Indonesia** — https://github.com/renomureza/hadis-api-id
   - The reviewed README documents `GET /hadith`, `GET /hadith/{perawiSlug}?page=1&limit=20`, and individual hadith lookup patterns. It identifies the API repository as MIT-licensed; upstream dataset origin/rights may differ.
   - Browser review of `https://hadis-api-id.vercel.app/hadith` rendered JSON describing nine books: Abu Dawud, Ahmad, Bukhari, Darimi, Ibnu Majah, Malik, Muslim, Nasai, and Tirmidzi. The browser result did not expose an inspectable HTTP status header; treat the status code as **UNVERIFIED**. The app does not embed those responses.
   - A browser probe without an explicit positive `limit` returned an empty page with `pageSize: 0`; the GitHub README sample link was also resolved by the browser tool as a request with a blank `limit` and returned 404. Neither is evidence that a valid `limit=20` page response failed or succeeded. Page fetching is **not runtime-verified here**.

3. **AlAdhan Prayer Times API** — https://aladhan.com/prayer-times-api
   - The documented timing path accepts a date with latitude, longitude and calculation method parameters. This app sends method ID `20` and validates the five required prayer times before caching.
   - Runtime validity for Indonesia/location/timezone and the method's current interpretation requires a device check and comparison against a trusted local timetable.

4. **Android exact alarms** — https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
   - Android versions targeting modern SDKs may restrict exact-alarm availability and user permission. The reminder code checks whether exact alarms can be scheduled, then attempts an inexact window fallback. This behavior needs device testing.

5. **Android splash screen** — https://developer.android.com/develop/ui/views/launch/splash-screen
   - Project uses the AndroidX splash screen library and a post-splash app theme.

6. **Android app signing** — https://developer.android.com/studio/publish/app-signing
   - Signing material is injected into the Gradle release build through environment variables; the repository has no keystore or passwords.

## Runtime checks attempted in the authoring environment

Direct `curl` attempts from the shell against the AlQuran Cloud, AlAdhan and Hadis API hostnames failed during DNS resolution (`curl` exit code 6 / HTTP result `000`); these are local resolver errors, not provider HTTP responses. No successful direct HTTP status code or full payload was captured in the shell. This environment also had no installed `gradle`, Android SDK (`ANDROID_HOME`/`ANDROID_SDK_ROOT`), or `adb`. Therefore:

- Endpoint reachability from this runtime: **UNVERIFIED** (DNS resolution was blocked here; do not interpret as the services being down).
- `gradle testDebugUnitTest`, `lintDebug`, and `assembleDebug`: **NOT RUN**.
- APK installation, UI, permission prompts, alarm delivery, and sensor behavior: **NOT TESTED**.
- Signed APK / apksigner verification: **NOT RUN**.

## Integrity and licensing limits

No API data file is included in this repository. Quran catalog/ayah index validators and hadith parser tests use synthetic fixture strings where appropriate; they are not substitutes for source validation of a released build. Review each upstream provider's current content/license/attribution rules before publishing, and create the final third-party dependency notices for the actual release.
