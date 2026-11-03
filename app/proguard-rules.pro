# Jejak error yang terbaca bila ada crash di build release
-keepattributes SourceFile,LineNumberTable,*Annotation*

# Entity dan DAO Room dipakai lewat kode hasil generate; jangan diubah namanya
-keep class com.odocare.app.data.** { *; }

# Enum dibaca lewat nama (backup JSON, penanda notifikasi)
-keepclassmembers enum com.odocare.app.** { *; }
