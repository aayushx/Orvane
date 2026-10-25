# Tasky proguard rules
-keepattributes *Annotation*

# kotlinx-serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.***$Companion { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.tasky.app.**$$serializer { *; }
-keepclassmembers class com.tasky.app.** { *** Companion; }
-keepclasseswithmembers class com.tasky.app.** { kotlinx.serialization.KSerializer serializer(...); }

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
