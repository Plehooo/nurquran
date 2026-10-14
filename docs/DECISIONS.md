# Implementation decisions

- **Native Compose:** use Kotlin + Jetpack Compose rather than a web wrapper.
- **SDK/API:** min SDK 24, compile/target SDK 36, Java bytecode target 17, AGP 8.13.2, Gradle 8.13, Kotlin 2.2.21, Compose BOM 2026.06.00. These form a conservative version set for API 36. The workflow is the final arbiter of actual dependency compatibility.
- **Remote-first religious content:** data is requested from provider APIs rather than copying a large dataset into the app. This limits offline coverage but reduces unverified redistribution of copyrighted/transcription data. Valid successful responses are cached.
- **Quran edition pair:** fetch `quran-uthmani` and `id.indonesian` separately then pair them only if ayah indices and counts match.
- **Hadith pagination:** page 1+ and limits 1–50 are enforced by the client; empty result pages are rejected rather than cached as success. The interface must not invent authenticity grades.
- **Prayer method:** pass AlAdhan method 20; manual place/coordinates are available. Default Jakarta is clearly a fallback label, not a GPS claim.
- **Storage:** DataStore holds preferences; a private JSON file cache stores validated service results. Room/SQL was not needed for this first API-cache implementation.
- **Reminder safety:** notification-only, opt-in and off by default; exact alarms are used only if allowed, otherwise a window alarm is used. No audio/silent-mode bypass.
- **Qibla:** a mathematical bearing display, not a sensor compass; don't claim calibration or live compass behavior.
- **Doa/dzikir:** no sample/reconstructed devotional text is presented as authoritative. Screen remains intentionally transparent until a vetted source and usage rights are selected.
- **Signing:** release key/passwords are environment-injected and never checked into source. CI skips signed build if any required secret is missing.
- **Gradle wrapper bootstrap:** authoring environment lacked a wrapper JAR and couldn't resolve download hosts. The archive therefore contains a transparent shim; both CI and local setup instructions generate a standard wrapper using the installed pinned Gradle before first build.
