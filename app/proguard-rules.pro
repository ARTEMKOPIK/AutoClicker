# AutoClicker ProGuard Rules
# Generated for comprehensive app optimization and crash-free execution

# ==================== KOTLIN & JVM ====================

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# Coroutines - essential for async operations
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.android.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep CancellationException for proper coroutine cancellation
-keep class kotlinx.coroutines.CancellationException { *; }
-keepclassmembers class kotlinx.coroutines.CancellationException { *; }

# ==================== OKHTTP ====================

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ==================== GSON ====================

-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.Expose <fields>;
}

# ==================== ML KIT (Text Recognition) ====================

-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ML Kit callbacks
-keep class com.google.mlkit.vision.text.TextRecognizer { *; }
-keep class com.google.mlkit.vision.common.InputImage { *; }
-keep class com.google.mlkit.vision.text.** { *; }

# ==================== ZXING (QR Code) ====================

-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-keepclassmembers class com.google.zxing.** { *; }

# ==================== DATA CLASSES ====================

# Script storage and transfer
-keep class com.autoclicker.app.util.ScriptStorage$Script { *; }
-keep class com.autoclicker.app.util.ScriptExporter$ExportedScript { *; }
-keep class com.autoclicker.app.util.ScriptLogger$LogEntry { *; }
-keep class com.autoclicker.app.util.ScriptLogger$Level { *; }
-keep class com.autoclicker.app.util.ScriptLogger$LogListener { *; }

# Profile management
-keep class com.autoclicker.app.util.ProfileManager$Profile { *; }

# Scheduler
-keep class com.autoclicker.app.util.ScriptScheduler$ScheduledTask { *; }

# Services
-keep class com.autoclicker.app.service.MacroRecorderService$RecordedAction { *; }
-keep class com.autoclicker.app.service.MacroRecorderService$MacroActionType { *; }

# Visual editor
-keep class com.autoclicker.app.visual.BlockTypes$BlockType { *; }
-keep class com.autoclicker.app.visual.VisualScriptStorage$VisualScript { *; }

# Code editor
-keep class com.autoclicker.app.util.CodeEditor$OnSearchListener { *; }
-keep class com.autoclicker.app.util.CodeEditor$TextChange { *; }

# ==================== SERVICES & RECEIVERS ====================

# Keep all services
-keep class com.autoclicker.app.service.** { *; }
-keepnames class com.autoclicker.app.service.** { *; }

# Keep scheduler receivers
-keep class com.autoclicker.app.util.ScriptScheduler$SchedulerReceiver { *; }
-keep class com.autoclicker.app.util.ScriptScheduler$BootReceiver { *; }
-keepclassmembers class com.autoclicker.app.util.ScriptScheduler$SchedulerReceiver { *; }
-keepclassmembers class com.autoclicker.app.util.ScriptScheduler$BootReceiver { *; }

# Keep accessibility service
-keep class com.autoclicker.app.service.ClickerAccessibilityService { *; }
-keepclassmembers class com.autoclicker.app.service.ClickerAccessibilityService { *; }

# Keep widget
-keep class com.autoclicker.app.widget.** { *; }

# ==================== ANDROIDX ====================

-keep class androidx.** { *; }
-dontwarn androidx.**
-keep class androidx.core.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.fragment.** { *; }

# AndroidX lifecycle components
-keep class * extends androidx.lifecycle.Lifecycle$State { *; }
-keep class * extends androidx.lifecycle.Lifecycle$Event { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ==================== BUILD CONFIG ====================

-keep class com.autoclicker.app.BuildConfig { *; }
-keepclassmembers class com.autoclicker.app.BuildConfig { *; }
-keepclassmembers class **.BuildConfig { *; }

# ==================== CRASH HANDLER ====================

# Keep CrashHandler for proper stack traces
-keep class com.autoclicker.app.util.CrashHandler { *; }
-keepclassmembers class com.autoclicker.app.util.CrashHandler { *; }
-keep class com.autoclicker.app.util.CrashHandler$ErrorLevel { *; }
-keep class com.autoclicker.app.util.CrashHandler$QueuedMessage { *; }

# ==================== APPLICATION ====================

# Keep Application class
-keep class com.autoclicker.app.AutoClickerApp { *; }
-keepclassmembers class com.autoclicker.app.AutoClickerApp { *; }

# ==================== KOTLIN SERIALIZATION (if used) ====================

#-keepattributes *Annotation*, InnerClasses
#-dontnote kotlinx.serialization.AnnotationsKt
#-keepclassmembers class kotlinx.serialization.json.** {
#    *** Companion;
#}
#-keepclasseswithmembers class kotlinx.serialization.json.** {
#    kotlinx.serialization.KSerializer serializer(...);
#}
#-keep,includedescriptorclasses class com.autoclicker.app.**$serializer { *; }
#-keepclassmembers class com.autoclicker.app.** {
#    *** Companion;
#}
#-keepclasseswithmembers class com.autoclicker.app.** {
#    kotlinx.serialization.KSerializer serializer(...);
#}
