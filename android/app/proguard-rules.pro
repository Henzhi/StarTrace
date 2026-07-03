# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.startrace.core.database.entity.** { *; }

# Retrofit + Moshi
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.startrace.core.network.** { *; }

# Keystore
-keep class com.startrace.core.security.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
