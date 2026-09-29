# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

## Kotlin Serialization
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
# See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
-dontnote kotlinx.serialization.**

# Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
# If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
# However, since in this case they will not be used, we can disable these warnings
-dontwarn kotlinx.serialization.internal.ClassValueReferences


-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn com.google.firebase.perf.network.FirebasePerfOkHttpClient
-dontwarn com.google.firebase.perf.network.FirebasePerfUrlConnection
# opencc4j
-keep class com.github.houbb.opencc4j.** { *; }
-dontwarn com.huaban.analysis.jieba.JiebaSegmenter

# Keep Data data classes
-keep class com.my.kizzy.remote.** { <fields>; }
# Keep Gateway data classes
-keep class com.my.kizzy.gateway.entities.** { <fields>; }
# --- Muso: keep own viewmodels and preference constants. R8 stripped metadata from
# some of these classes in release builds, which crashed the library tab.
-keep class com.muso.music.viewmodels.** { *; }
-keep class com.muso.music.constants.** { *; }

# --- ArchiveTune settings-kit port (Round 180) --------------------------------
# Rules ported from AT's proguard-rules.pro for the libraries the kit brought in.

## Ktor (Music Together, AI, lyrics, updater clients).
# R8 reports ktor's cross-module references (HttpTimeout, io.ktor.utils.io, …)
# as missing classes under AGP9's jetified KMP jars; the classes are present at
# runtime — AT ships the same keep/dontwarn pair.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

## Markwon — optional GIF support (android-gif-drawable) not bundled
-dontwarn pl.droidsonroids.gif.**

## NewPipeExtractor (YouTube playback via the kit's NewPipe downloader)
-keep class org.schabi.newpipe.extractor.services.youtube.protos.** { *; }
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.schabi.newpipe.extractor.** { *; }
-keepclassmembers class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.javascript.engine.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn org.mozilla.javascript.tools.**
-keep class javax.script.** { *; }
-dontwarn javax.script.**
-keep class jdk.dynalink.** { *; }
-dontwarn jdk.dynalink.**

## Reflection/deserialization attributes the kit's models rely on
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

## Kuromoji (Japanese lyrics segmentation)
-keep class com.atilika.kuromoji.** { *; }

## Queue persistence (kit's MusicService serializes these via java serialization)
-keep class moe.rukamori.archivetune.models.PersistQueue { *; }
-keep class moe.rukamori.archivetune.models.PersistPlayerState { *; }
-keep class moe.rukamori.archivetune.models.QueueData { *; }
-keep class moe.rukamori.archivetune.models.QueueType { *; }
-keep class moe.rukamori.archivetune.playback.queues.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

## Jetpack Glance (AOD clock widget)
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
-keep class * implements androidx.glance.action.ActionCallback {
    public <init>();
}
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
