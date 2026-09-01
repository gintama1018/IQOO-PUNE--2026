# SkillLens ProGuard Rules

# Keep MediaPipe
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Keep Room entities
-keep class com.skilllens.app.data.database.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.skilllens.app.taskengine.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
