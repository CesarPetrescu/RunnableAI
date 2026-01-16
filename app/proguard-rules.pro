# Keep native JNI entry points
-keepclassmembers class * {
    native <methods>;
}

# ExecuTorch uses reflection for annotations
-keep class org.pytorch.executorch.** { *; }
-keep class com.facebook.** { *; }
