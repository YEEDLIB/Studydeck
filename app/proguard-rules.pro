# Add project-specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# -------------------------------------------------------------------------
# General Android / JVM Rules
# -------------------------------------------------------------------------

# Preserve line numbers and source file names for readable stack traces in crash logs
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve necessary attributes for reflection, generics, and annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# -------------------------------------------------------------------------
# Kotlin / Coroutines
# -------------------------------------------------------------------------
-keepclassmembers class * extends kotlin.properties.Delegates { *; }
-dontwarn kotlinx.coroutines.**

# -------------------------------------------------------------------------
# Jetpack Compose & Architecture Components
# -------------------------------------------------------------------------

# Keep ViewModels and their constructors so they can be instantiated via reflection
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Compose-related runtime classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# -------------------------------------------------------------------------
# Room Database Configuration
# -------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.example.data.model.** { *; }
-keep class com.example.data.dao.** { *; }

-dontwarn androidx.room.**

# -------------------------------------------------------------------------
# Moshi & JSON Serialization
# -------------------------------------------------------------------------
# Keep Moshi's generated JSON adapters and model classes to avoid reflection failures
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class *JsonAdapter { *; }
-dontwarn com.squareup.moshi.**

# -------------------------------------------------------------------------
# Media3 & ExoPlayer
# -------------------------------------------------------------------------
# ExoPlayer dynamically instantiates decoders and plugins by name, requiring keep rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# -------------------------------------------------------------------------
# Firebase & Google Play Services / Gemini API
# -------------------------------------------------------------------------
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.ai.client.generativeai.**
