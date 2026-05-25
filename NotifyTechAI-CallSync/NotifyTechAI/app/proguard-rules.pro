# Keep Retrofit interfaces
-keep interface com.notifytechai.callsync.ApiService { *; }

# Keep data classes used by Gson
-keep class com.notifytechai.callsync.CallRequest { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
