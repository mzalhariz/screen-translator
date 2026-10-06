# ML Kit on-device translation
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep the accessibility service entry point
-keep class com.example.screentranslator.overlay.TranslateAccessibilityService { *; }
