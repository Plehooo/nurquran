# Retrofit/Gson models are Kotlin data classes. Keep generic signatures and annotations
# so Gson's reflective adapters continue to work in minified release builds.
-keepattributes Signature,*Annotation*
-keep class com.nurquran.app.data.** { *; }
-dontwarn sun.misc.**
