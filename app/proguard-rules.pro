# PDFBox Android Proguard Rules
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Room Database Proguard Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines & Serialization
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# AndroidX Biometric & Security Rules
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# CameraX Proguard Rules
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Tesseract 4 Android (Offline OCR)
-keep class com.googlecode.tesseract.android.** { *; }
-dontwarn com.googlecode.tesseract.android.**
-keep class com.googlecode.leptonica.android.** { *; }
-dontwarn com.googlecode.leptonica.android.**
