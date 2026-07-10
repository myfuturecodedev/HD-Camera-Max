# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ====================================================================
# 🛠️ SYSTEM & CODEBASE CORE OPTIMIZATION GUARDS
# ====================================================================
# Uncomment this to preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# AndroidX Fragment / Activity result APIs
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public <init>(...);
}
-keepclassmembers class * extends androidx.appcompat.app.AppCompatActivity {
    public <init>(...);
}

# Parcelize & Serialization Guards
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Kotlin Coroutines Infrastructure
-keepnames class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ====================================================================
# 📦 APP-SPECIFIC DATA MODELS (FutureCode Core Packages)
# ====================================================================
# Keep notification model properties safe from stripping
-keep class com.futurecode.hdcameramax.notification.NotificationModel { *; }

# Keep all data models mapped via Retrofit/Gson inside model package
-keep class com.futurecode.hdcameramax.model.** { *; }

# ====================================================================
# 🌐 RETROFIT, OKHTTP & GSON NETWORK LAYER GUARDS
# ====================================================================
# Retrofit infrastructure reflection rules
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}

# OkHttp structural optimization exceptions
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Gson specific reflection sweeping guards
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# ====================================================================
# 💰 GOOGLE PLAY IN-APP BILLING CLIENT PIPELINE
# ====================================================================
# Safeguard automated proxy interfaces from billing library package
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# ====================================================================
# 📢 ADMOB, FACEBOOK AUDIENCE NETWORK & MEDIATION MONETIZATION
# ====================================================================
# AdMob base framework exceptions
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# 🛑 CRITICAL SHIELD TO PREVENT ADMOB VERIFYERROR CRASH
-keep class com.google.android.gms.internal.ads.** { *; }
-dontwarn com.google.android.gms.internal.ads.**

# Facebook Audience Network SDK infrastructure
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.infer.annotation.**

# Meta Ad Mediation adapters rules
-keep class com.google.ads.mediation.facebook.** { *; }

# ====================================================================
# 🌠 GLIDE & LOTTIE ANIMATION MEDIA EXTENSIONS
# ====================================================================
# Glide Image Caching system configurations
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# Lottie animation layers parser exceptions
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ====================================================================
# ⚙️ JETPACK WORKMANAGER & GUAVA PIPELINES
# ====================================================================
# Prevent shrinking of structural dynamic background workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# Guava compilation references warnings suppressions
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**

# ====================================================================
# 📍 NAVIGATION SAFE ARGS & HILT INJECTIONS
# ====================================================================
# Navigation engine architecture components remapping safety
-keep class **Args { *; }
-keep class **Directions { *; }

# SDP / SSP Layout Scalers Dimens Guard
-keep class com.intuit.sdp.** { *; }
-keep class com.intuit.ssp.** { *; }

# ====================================================================
# 📸 CAMERAX HARDWARE INFRAS ENGINE PLUGINS
# ====================================================================
# Keep all core CameraX framework interface classes intact
-keep class androidx.camera.core.** { *; }
-dontwarn androidx.camera.core.**

# Protect internal lifecycle and state management configurations safely
-keep class androidx.camera.lifecycle.** { *; }
-dontwarn androidx.camera.lifecycle.**

# Camera2 interop layer directly references underlying OS hardware hooks via reflection
-keep class androidx.camera.camera2.** { *; }
-dontwarn androidx.camera.camera2.**

# Guard internal state tracking implementation hooks from being renamed
-keep class androidx.camera.core.impl.** { *; }
-keep class * implements androidx.camera.core.impl.Config { *; }
-keep class * extends androidx.camera.core.impl.Config$Option { *; }
-keep class * implements androidx.camera.core.impl.UseCaseConfig$Builder { *; }
-keep class * implements androidx.camera.core.impl.CameraConfig { *; }
-keep class * implements androidx.camera.core.impl.ReadableConfig { *; }
-keep class androidx.camera.camera2.impl.** { *; }

# Prevent optimization stripping from CameraView components (PreviewView layouts)
-keep class androidx.camera.view.** { *; }
-dontwarn androidx.camera.view.**

# Safeguard Video Capture Record tracking logic state machines
-keep class androidx.camera.video.** { *; }
-dontwarn androidx.camera.video.**

# Devices specific hardware compatibility configurations optimization layer
-keep class androidx.camera.extensions.** { *; }
-dontwarn androidx.camera.extensions.**