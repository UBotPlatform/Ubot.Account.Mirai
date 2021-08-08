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

# Bouncy Castle
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

# Jansi
-keep class org.fusesource.jansi.internal.**{
    *;
}

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

# Entry
-keepclasseswithmembers public class ubot.account.mirai.MiraiAccountKt {
    public static void main(java.lang.String[]);
}