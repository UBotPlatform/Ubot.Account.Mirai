-dontwarn
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
-keepclassmembers class net.mamoe.mirai.** {
    volatile <fields>;
}
-keepclassmembernames class net.mamoe.mirai.** {
    volatile <fields>;
}
-keep,includedescriptorclasses class net.mamoe.mirai.**$$serializer { *; }
-keepclassmembers class net.mamoe.mirai.** {
    *** Companion;
}
-keepclasseswithmembers class net.mamoe.mirai.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Bouncy Castle
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

# Jansi
-keep class org.fusesource.jansi.internal.**{
    *;
}

# Ktor
-keepclassmembers class io.ktor.** {
    volatile <fields>;
}
-keepclassmembernames class io.ktor.** {
    volatile <fields>;
}
-keep class io.ktor.client.engine.** implements io.ktor.client.HttpClientEngineContainer

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembernames class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# Kotlin Reflect
-keep class kotlin.Metadata { *; }
-keep interface kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader
-keep class * implements kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader { public protected *; }
-keep interface kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition
-keep class * implements kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition { public protected *; }
-keepattributes InnerClasses,Signature,RuntimeVisible*Annotations,EnclosingMethod
-dontnote kotlin.internal.PlatformImplementationsKt

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# KtUBotCommon
-keep,includedescriptorclasses class ubot.**$$serializer { *; }
-keepclassmembers class ubot.** {
    *** Companion;
}
-keepclasseswithmembers class ubot.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# slf4j
-keep class org.slf4j.impl.** { *; }
-keep class net.mamoe.mirai.logger.bridge.slf4j.** { *; }

# Entry
-keepclasseswithmembers public class ubot.account.mirai.MiraiAccountKt {
    public static void main(java.lang.String[]);
}