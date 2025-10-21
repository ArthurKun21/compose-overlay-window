# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate
-keepattributes *Annotation*

# Preserve all benchmark code
-keep class com.github.only52607.compose.benchmark.** { *; }
