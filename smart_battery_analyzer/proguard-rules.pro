# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/elab/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:
#-ignorewarnings

# Found most of the below here:
# https://github.com/krschultz/android-proguard-snippets/tree/master/libraries

# Adding this according to Picasso on Github (https://github.com/square/picasso)
-dontwarn com.squareup.okhttp.**

# 05-Oct-2016. Problems with release build.
#-keepattributes *Annotation*
#-keepattributes Signature
#-keepattributes InnerClasses



# 03-Nov-2016. compileSdkVersion 23 + targetSdkVersion 22. Both old and new devices connect OK. Besides there are proguard warnings for these libraries.
-dontwarn android.support.v4.app.NotificationCompatBase
-dontwarn android.support.v4.app.NotificationCompatGingerbread
-dontwarn com.afollestad.materialdialogs.internal.MDButton

# Inserted these for Butterknife version 6
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewInjector { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}

## GreenRobot EventBus specific rules ##
# https://github.com/greenrobot/EventBus/blob/master/HOWTO.md#proguard-configuration
-keepclassmembers class ** {
    public void onEvent*(***);
}

# GreenDao rules
# Source: http://greendao-orm.com/documentation/technical-faq
#
#-keepclassmembers class * extends de.greenrobot.dao.AbstractDao {
#    public static final String TABLENAME;
#}
#-keep class **$Properties

# The above did not work but these are ok
-keep class greendao.*$Properties {
    public static <fields>;
}
-keepclassmembers class greendao.** {
    public static final *;
}

# Remove ALL logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Not sure that this fixes the bad parcelable crasches
# Parcel library
-keep class * implements android.os.Parcelable {
   public static final android.os.Parcelable$Creator *;
}
-keep class **$$Parcelable { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


# 14-Nov-2016.
-dontwarn android.support.v4.**
-dontwarn com.afollestad.materialdialogs.internal.MDButton.**

# 15-Nov-2016.
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

# 17-Nov-2016.
-keep public class * extends android.app.Dialog
-keep public class * extends android.app.AlertDialog
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

-keep public class * extends android.support.v7.app.AppCompatActivity
-keep public class * extends android.support.v4.app.Fragment

-keep public class * implements com.kbeanie.imagechooser.api.ImageChooserListener

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep public class * extends android.bluetooth.BluetoothGattCallback


# 08-May-2018. Serialization issues.
# https://www.guardsquare.com/en/proguard/manual/examples#serializable
#-addconfigurationdebugging
-keep public class * extends android.app.IntentService

-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepclassmembers class com.ctek.sba.rest.CIngestData {
    <fields>;
    <init>();
}
-keepclassmembers class com.ctek.sba.rest.CRequestProperties {
    <fields>;
    <init>();
}
-keepclassmembers class com.ctek.sba.rest.CSensor {
    <fields>;
    <init>();
}
-keepclassmembers class com.ctek.sba.rest.SrvPostSocs {
    <fields>;
    <init>();
}
-keepclassmembers class com.ctek.sba.rest.SrvPostCapacity {
    <fields>;
    <init>();
}
