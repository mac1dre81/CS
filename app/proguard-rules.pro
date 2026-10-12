# ============================================================================
# KeyVault - ProGuard / R8 rules
# ============================================================================
# R8 minify + shrink is ENABLED for release builds. These rules preserve
# reflection-dependent library code. API keys are never in source — they live
# only in the encrypted DB column — so obfuscation does not weaken security.
# Run `./gradlew lintRelease` after any rule change to catch regressions.
# ============================================================================

# --- Generics / reflection attributes (needed by many libraries above) -------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable

# --- Application code ----------------------------------------------------------
# Keep our full type tree (ViewModels, use-cases, entities, etc.). Our sealed
# Outcome/VaultException hierarchies and Provider/KeyStatus enums rely on
# reflection-based serialization and runtime type checks.
-keep public class com.keyvault.app.** { *; }
-dontwarn com.keyvault.app.**

# kotlinx-serialization uses reflection for (de)serialization.
-keepclassmembers class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.internal.** { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class kotlin.reflect.** { *; }

# --- Kotlin Coroutines ---------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.coroutines.debug.**
-keepclassmembers class kotlinx.coroutines.** {
    suspend <methods>;
}

# --- Room (KSP-generated DAO impls + entities) ---------------------------------
# KSP generates DAO implementations at compile time as subclasses. R8 must not
# remove the abstract Database class, the entity POJOs, or the generated DAO
# impls reachable through the abstract DAO interface methods.
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.Ignore <fields>;
    @androidx.room.Index <fields>;
}
-keep class com.keyvault.app.data.local.** { *; }
-keep class androidx.room.** { *; }

# --- OkHttp / Okio (CRITICAL) --------------------------------------------------
# OkHttp 4.x uses reflection and platform-specific classes (Conscrypt, etc.).
# Without these -dontwarn rules, R8 fails the build or OkHttp throws at runtime.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# --- AndroidX Security (EncryptedSharedPreferences / MasterKey) ----------------
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# --- AndroidX Biometric --------------------------------------------------------
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# --- AndroidX Activity / Compose runtime -------------------------------------
-keep class androidx.activity.** { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# --- Google Play Services / Mobile Ads (AdMob) ---------------------------------
# Banner + interstitial ads for free-tier users. Keep Parcelable and
# reflection-based classes; suppress warnings from the AAR consumer rules.
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.play.** { *; }
-dontwarn com.google.android.play.**

# --- Play Billing Library -----------------------------------------------------
-dontwarn com.android.billingclient.**
-keep class com.android.billingclient.api.** { *; }
-keep class com.android.billingclient.** { *; }

# --- WebView (PrivacyPolicyActivity) -------------------------------------------
-dontwarn android.webkit.**
-keep class android.webkit.** { *; }
