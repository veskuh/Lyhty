# Room ProGuard Rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-dontwarn androidx.room.paging.**

# Retrofit & OkHttp ProGuard Rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers enum * { *; }

# Kotlinx Serialization ProGuard Rules
-keepattributes *Annotation*, ElementValueAttribute
-keepclassmembers class * {
    @kotlinx.serialization.Serializer *;
    @kotlinx.serialization.Serializable *;
}

# Keep Miniflux DTO models
-keep class net.veskuh.lyhty.data.network.dto.** { *; }
-keep class net.veskuh.lyhty.data.local.entity.** { *; }
