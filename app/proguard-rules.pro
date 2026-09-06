# Keep serializable data models intact for kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.sahin.smfinddevice.data.**$$serializer { *; }
-keepclassmembers class com.sahin.smfinddevice.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.sahin.smfinddevice.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
