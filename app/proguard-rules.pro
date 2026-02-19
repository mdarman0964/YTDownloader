# ProGuard rules
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Keep Room entities
-keep class com.ytdownloader.data.** { *; }

# Keep serialized classes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
