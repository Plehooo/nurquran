# QA status checklist

Legend: `[x]` means the source/config item is present in this archive; it does not imply runtime pass. `[ ]` means test/release step remains outstanding.

- [x] Native Kotlin/Jetpack Compose source skeleton with home, Quran, hadith, prayer, Qibla, settings, bookmarks and About screens.
- [x] Quran API edition requests and structural catalog/ayah index validation.
- [x] Hadith API book/page models, pagination guard and empty-payload rejection.
- [x] AlAdhan timing request using method 20 and required-prayer validation.
- [x] Private JSON cache and DataStore preferences.
- [x] Notification-only reminder implementation defaults off; exact alarm permission checked before use.
- [x] HTTP body logging disabled; backup/data extraction disabled in manifest.
- [x] Unit test source files created for core pure logic, JSON parsing, pagination helpers, and a synthetic Retrofit request contract. Tests remain unexecuted until CI.
- [x] GitHub Actions workflow source created for unit tests, lint, debug APK, optional signed release.
- [x] Research, architecture, licensing, testing, and privacy boundary documents included.
- [ ] Standard official Gradle wrapper JAR added to the source archive; current archive's `gradlew` is a bootstrap shim. CI/local setup generates the wrapper before build.
- [ ] Gradle dependency resolution and Android Kotlin compilation pass.
- [ ] Unit tests execute and pass.
- [ ] Android lint is clean or reviewed.
- [ ] Debug APK built, installed, and smoke-tested on real/emulated Android devices.
- [ ] API response mapping/page semantics validated against live endpoints from a normal network.
- [ ] Quran text/translation verified against trusted sample/golden sources by a knowledgeable reviewer.
- [ ] Prayer results checked against a trusted Indonesian timetable.
- [ ] Reminder delivery tested under Doze/exact-alarm/notification permission conditions and next-day behavior completed.
- [ ] Audio adhan/murottal, live sensor compass, bundled offline Quran, automatic reboot/timezone rescheduling, and sourced doa/dzikir implemented or removed from product requirements.
- [ ] Third-party notices and publisher privacy policy reviewed for release.
- [ ] Release keystore configured privately, signed APK produced and verified by CI, checksum published.

## Release recommendation

**Do not publish this build yet.** It is an initial source implementation, not a verified release candidate. Clear the outstanding build, device, content, scheduling and licensing items before release.
