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

# Native JNI entrypoints
-keepclasseswithmembernames class * {
    native <methods>;
}

# Room Entities & DAOs
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
}

# Google Mobile Ads (AdMob)
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
-dontwarn com.google.android.gms.ads.**

# Guava / ListenableFuture
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Keep standard metadata for reflection & debugging
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# R8 Aggressive Code Shrinking, Inlining, and DEX Code Obfuscation
-repackageclasses 'a'
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively

# Remove sensitive logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
