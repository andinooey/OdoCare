# OdoCare

Aplikasi Android native (Kotlin + Jetpack Compose) untuk mencatat kilometer, jadwal servis, dokumen kendaraan (pajak, STNK 5 tahunan, SIM, asuransi), dan pengingat lokal. Berjalan offline; tanpa akun, server, atau API eksternal.

## Fitur yang sudah ada
Multi-kendaraan • odometer manual + riwayat + konfirmasi jika nilai turun • jadwal servis berbasis km/waktu/gabungan dengan status otomatis • catat servis (biaya) • dokumen & legalitas dengan status dan catat perpanjangan • pengingat tanggal dengan jumlah/satuan/jam dinamis • pengingat update odometer • notifikasi lokal (3 channel, uji notifikasi, reboot/zona waktu/pembaruan aplikasi).

## Persyaratan build
JDK 17, Gradle 8.7, Android SDK 34. AGP 8.5.2, Kotlin 1.9.24.

## Build lokal
1. Sekali saja, buat wrapper: `gradle wrapper --gradle-version 8.7` lalu commit `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`.
2. `./gradlew testDebugUnitTest assembleDebug`
3. APK: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions
Push/PR ke `main` atau jalankan manual (tab Actions → Android Build). Unduh APK dari artifact **OdoCare-APK**.

## Rilis
Buat tag `vX.Y.Z` (mis. `git tag v1.0.0 && git push --tags`). Untuk APK bertanda tangan, tambahkan Secrets: `KEYSTORE_BASE64` (base64 file keystore), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Tanpa secrets, APK diberi akhiran `-UNSIGNED` dan tidak ditandatangani. Jangan commit keystore.

## Data & privasi
Semua data disimpan lokal di perangkat. Anda bertanggung jawab atas pencadangan data (fitur ekspor/cadangan belum tersedia di versi ini). Aplikasi tidak membaca odometer otomatis dan tidak memverifikasi status resmi dokumen.
