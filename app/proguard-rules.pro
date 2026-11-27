# ProGuard and R8 Rules for AniVexa (com.github.dfedez920912.anivexa)

# Keep Line Numbers and Source File Attributes for Production Crash Logs
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep Room Database Entities & DAOs
-keep class com.github.dfedez920912.anivexa.data.local.** { *; }
-keep interface com.github.dfedez920912.anivexa.data.local.** { *; }

# Keep Moshi JSON Serialized Data Models & Codegen Adapters
-keep class com.github.dfedez920912.anivexa.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Keep AppWidgets and Broadcast Receivers
-keep class com.github.dfedez920912.anivexa.widget.** { *; }
-keep class com.github.dfedez920912.anivexa.notifications.** { *; }

# Keep Retrofit API Interface Methods
-keep interface com.github.dfedez920912.anivexa.data.** { *; }
