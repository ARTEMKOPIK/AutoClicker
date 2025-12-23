# AutoClicker ProGuard Rules

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn javax.annotation.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep data classes
-keep class com.autoclicker.app.util.ScriptStorage$Script { *; }
-keep class com.autoclicker.app.util.ScriptExporter$ExportedScript { *; }
-keep class com.autoclicker.app.util.ProfileManager$Profile { *; }
-keep class com.autoclicker.app.util.ScriptScheduler$ScheduledTask { *; }
-keep class com.autoclicker.app.service.MacroRecorderService$RecordedAction { *; }

# Keep services
-keep class com.autoclicker.app.service.** { *; }

# Keep widget
-keep class com.autoclicker.app.widget.** { *; }

# Keep scheduler receiver
-keep class com.autoclicker.app.util.ScriptScheduler$SchedulerReceiver { *; }
-keep class com.autoclicker.app.util.ScriptScheduler$BootReceiver { *; }

# Keep accessibility service
-keep class com.autoclicker.app.service.ClickerAccessibilityService { *; }

# Keep Application class
-keep class com.autoclicker.app.AutoClickerApp { *; }

# Keep CrashHandler for proper stack traces
-keep class com.autoclicker.app.util.CrashHandler { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Keep CancellationException for proper coroutine cancellation
-keep class kotlinx.coroutines.CancellationException { *; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep BuildConfig
-keep class com.autoclicker.app.BuildConfig { *; }

# ZXing QR
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
