# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# OkHttp & Okio R8 / ProGuard rules
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.**
-dontwarn okio.**

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Preserve data models
-keep class com.example.data.model.** { *; }
