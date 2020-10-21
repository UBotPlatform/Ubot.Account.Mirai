-dontwarn
-allowaccessmodification
-dontobfuscate
-dontoptimize

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

# Data Classes
-keep,includedescriptorclasses class ubot.**$$serializer { *; }
-keepclassmembers class ubot.** {
    *** Companion;
}
-keepclasseswithmembers class ubot.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class net.mamoe.mirai.**$$serializer { *; }
-keepclassmembers class net.mamoe.mirai.** {
    *** Companion;
}
-keepclasseswithmembers class net.mamoe.mirai.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Remove slf4j log
-assumenosideeffects class * implements org.slf4j.Logger {
    public *** trace(...);
    public *** debug(...);
    public *** info(...);
    public *** warn(...);
    public *** error(...);
}

# Remove Null Check
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    public static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
    public static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    public static void checkNotNull(java.lang.Object);
    public static void checkNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
    public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    public static void throwUninitializedPropertyAccessException(java.lang.String);
}

# Entry
-keepclasseswithmembers public class ubot.account.mirai.MiraiAccountKt {
    public static void main(java.lang.String[]);
}