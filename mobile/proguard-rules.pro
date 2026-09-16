# ============================================================
# Модели приложения (Gson работает через рефлексию)
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
# PDFBox-Android
# ============================================================
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.tom_roush.fontbox.**
-dontwarn com.tom_roush.harmony.**
-dontwarn org.apache.pdfbox.**

# ============================================================
# Jsoup
# ============================================================
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

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
# AlarmReceiver (регистрируется в манифесте)
# ============================================================
-keep class ru.abg.sstuschedule.alarm.AlarmReceiver { *; }

# ============================================================
# Data Layer / Wearable
# ============================================================
-keep class com.google.android.gms.wearable.** { *; }
-keep class * extends com.google.android.gms.wearable.WearableListenerService
-dontwarn com.google.android.gms.**

# ============================================================
# Отключить логирование в release
# ============================================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}