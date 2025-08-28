# FITFOAI ProGuard Rules
# Production-ready obfuscation rules for FITFOAI Android app

# Basic Android optimizations
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Preserve debugging information for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations for runtime reflection
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# === ANDROID SERVICES & COMPONENTS ===
# Keep all Services and their public methods
-keep public class * extends android.app.Service
-keepclassmembers class * extends android.app.Service {
    public <methods>;
}

# Keep Broadcast Receivers
-keep public class * extends android.content.BroadcastReceiver

# Keep background location service
-keep class com.runningcoach.v2.data.service.BackgroundLocationService {
    public <methods>;
    public <fields>;
}

# Keep Session Recovery Manager
-keep class com.runningcoach.v2.data.service.SessionRecoveryManager {
    public <methods>;
    public <fields>;
}

# === VOICE COACHING SERVICES ===
# Keep voice coaching manager and all its methods
-keep class com.runningcoach.v2.data.service.VoiceCoachingManager {
    public <methods>;
    public <fields>;
}

# Keep ElevenLabs service
-keep class com.runningcoach.v2.data.service.ElevenLabsService {
    public <methods>;
    public <fields>;
}

# Keep audio focus manager
-keep class com.runningcoach.v2.data.service.AudioFocusManager {
    public <methods>;
    public <fields>;
}

# === ROOM DATABASE & ENTITIES ===
# Keep all database entities
-keep class com.runningcoach.v2.data.local.entity.** {
    public <fields>;
    public <methods>;
}

# Keep DAOs
-keep interface com.runningcoach.v2.data.local.dao.** {
    public <methods>;
}

# Keep database class
-keep class com.runningcoach.v2.data.local.FITFOAIDatabase {
    public <methods>;
}

# Keep Room type converters
-keep class com.runningcoach.v2.data.local.converter.** {
    public <methods>;
}

# === KOTLIN SERIALIZATION ===
# Keep @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes in service layer
-keep,includedescriptorclasses class com.runningcoach.v2.data.service.**$$serializer { *; }
-keepclassmembers class com.runningcoach.v2.data.service.** {
    *** Companion;
}
-keepclasseswithmembers class com.runningcoach.v2.data.service.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# === COROUTINES ===
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# === KTOR CLIENT ===
# Keep Ktor client classes
-keep class io.ktor.client.** { *; }
-keep class io.ktor.http.** { *; }
-keep class io.ktor.serialization.** { *; }
-keep class io.ktor.utils.io.** { *; }

# === GOOGLE PLAY SERVICES ===
# Keep Google Maps classes
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.**

# Keep Google Fit classes
-keep class com.google.android.gms.fitness.** { *; }
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.auth.** { *; }

# === MEDIA & AUDIO ===
# Keep MediaPlayer and Media3 classes
-keep class androidx.media.** { *; }
-keep class androidx.media3.** { *; }
-keep class android.media.** { *; }

# Keep audio focus related classes
-keep class android.media.AudioManager$** { *; }
-keep class android.media.AudioFocusRequest$** { *; }

# === WORKMANAGER ===
# Keep WorkManager classes
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# === DEPENDENCY INJECTION (Manual DI) ===
# Keep AppContainer and its dependencies
-keep class com.runningcoach.v2.di.AppContainer {
    public <methods>;
    public <fields>;
}

# === DOMAIN LAYER ===
# Keep domain models and use cases
-keep class com.runningcoach.v2.domain.model.** {
    public <fields>;
    public <methods>;
}
-keep class com.runningcoach.v2.domain.usecase.** {
    public <methods>;
}
-keep interface com.runningcoach.v2.domain.repository.** {
    public <methods>;
}

# === COMPOSE UI ===
# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# === MEMORY OPTIMIZATIONS ===
# Remove logging in release builds (except error level)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# === CRASH REPORTING ===
# Keep stack traces for crash analysis
-keepattributes LineNumberTable,SourceFile

# === PREVENT ISSUES WITH REFLECTION ===
# Keep classes accessed via reflection
-keep class * extends android.app.Activity
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends android.app.Application

# === FINAL OPTIMIZATIONS ===
# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification