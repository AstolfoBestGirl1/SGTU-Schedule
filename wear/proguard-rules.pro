
# ============================================================
# Модели приложения (Gson через рефлексию)
# ============================================================
-keep class ru.abg.sstuschedule.model.** { *; }
-keepclassmembers class ru.abg.sstuschedule.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ============================================================
# Gson
# ============================================================
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-dontwarn com.google.gson.**

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================
# WorkManager
# ============================================================
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# ============================================================
# Data Layer / Wearable
# ============================================================
-keep class com.google.android.gms.wearable.** { *; }
-keep class * extends com.google.android.gms.wearable.WearableListenerService
-keep class ru.abg.sstuschedule.service.DataLayerListenerService { *; }
-dontwarn com.google.android.gms.**

# ============================================================
# Tiles
# ============================================================
-keep class androidx.wear.tiles.** { *; }
-keep class * extends androidx.wear.tiles.TileService
-keep class * extends androidx.wear.tiles.Material3TileService
-dontwarn androidx.wear.tiles.**

# ============================================================
# ProtoLayout
# ============================================================
-keep class androidx.wear.protolayout.** { *; }
-dontwarn androidx.wear.protolayout.**

# ============================================================
# Complication
# ============================================================
-keep class * extends androidx.wear.watchface.complications.datasource.ComplicationDataSourceService

# ============================================================
# Отключить логирование в release
# ============================================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}