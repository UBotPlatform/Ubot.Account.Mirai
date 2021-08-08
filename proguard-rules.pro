-ignorewarnings
-allowaccessmodification
-dontobfuscate
-dontoptimize

# Mirai
-keep enum net.mamoe.mirai.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepnames class net.mamoe.mirai.Mirai
-keepnames class net.mamoe.mirai.MiraiImpl
-keepnames class net.mamoe.mirai.message.data.OfflineAudio.Factory
-keepnames class net.mamoe.mirai.internal.message.OfflineAudioFactoryImpl
-keepclassmembers class net.mamoe.mirai.** {
    volatile <fields>;
}

# Netty
-keepclassmembers class io.netty.channel.ChannelOutboundHandler { *; }
-keepclassmembers class io.netty.channel.ChannelHandler { *; }
-dontwarn io.netty.handler.codec.**
-dontwarn io.netty.handler.ssl.**
-dontwarn io.netty.channel.rxtx.**
-dontwarn io.netty.channel.udt.**
-dontwarn io.netty.util.internal.logging.**
-dontwarn io.netty.util.internal.svm.**
-dontwarn io.netty.util.NetUtilSubstitutions
-dontwarn io.netty.util.internal.Hidden$NettyBlockHoundIntegration
-dontwarn io.netty.util.internal.Hidden$NettyBlockHoundIntegration$1

# OkHttp3 Additional Rules
-dontwarn okhttp3.internal.platform.**

# Bouncy Castle
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-dontwarn org.bouncycastle.pqc.crypto.qtesla.QTeslaKeyEncodingTests

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}

# slf4j
-keep class org.slf4j.impl.** { *; }

# log4j2
-dontwarn org.apache.logging.log4j.**
-keep class org.apache.logging.log4j.** { *; }

# Entry
-keepclasseswithmembers public class ubot.account.mirai.MiraiAccountKt {
    public static void main(java.lang.String[]);
}