# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.appwidget.AppWidgetProvider

# Keep JSON parsing
-keepclassmembers class * {
    @org.json.* <methods>;
}
