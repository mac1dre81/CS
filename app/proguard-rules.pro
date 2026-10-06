# KeyVault - ProGuard / R8 rules
# v1.0 keeps minification OFF (isMinifyEnabled = false), so these are only
# relevant if/when shrinking is enabled. We still ship sensible keep rules.

# Keep our public API surface.
-keep public class com.keyvault.app.** { *; }

# kotlinx-serialization uses reflection for (de)serialization.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepclassmembers,allowobfuscation,allowshrinking class kotlin.Metadata
-keepclassmembers class kotlinx.** { *; }
-keep -keepclassmembers class kotlin.reflect.** { *; }
-keep,includedescriptorclasses,allowshrinking class kotlin.Metadata
-keepclassmembers class kotlinx.serialization.** { *; }
-keep,allowshrinking,allowobfuscation class kotlinx.serialization.internal.** { *; }

# Do NOT keep or log API keys. None of our key material is in source. The only
# place secrets live is the encrypted database column, produced at runtime.

# Play Billing / BillingClient keep rules.
-dontwarn com.android.billingclient.**
-keep class com.android.billingclient.** { *; }
