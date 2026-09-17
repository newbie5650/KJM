# Build APK WMS Alat Berat dari HP (Cara Cloud)

Project ini adalah source code Android, bukan APK. Cara paling sederhana dari HP adalah memakai GitHub Actions untuk melakukan build di cloud.

## Langkah

1. Buat akun/login GitHub di browser HP.
2. Buat repository baru, misalnya `wms-alat-berat`.
3. Upload **semua isi folder project ini** ke repository. Jangan upload folder pembungkus yang membuat `settings.gradle.kts` berada satu tingkat terlalu dalam.
4. Setelah upload selesai, buka tab **Actions**.
5. Pilih workflow **Build Android APK**.
6. Tekan **Run workflow** (atau lakukan commit ke branch `main`, workflow juga akan berjalan otomatis).
7. Setelah selesai, buka hasil workflow dan bagian **Artifacts**.
8. Download `wms-alat-berat-debug-apk.zip`, ekstrak, lalu instal `app-debug.apk` di HP.

## Catatan penting

- APK yang dihasilkan adalah **debug APK** untuk pengujian/internal use, bukan release APK Play Store.
- Project awal memiliki konfigurasi signing debug yang menunjuk ke `debug.keystore` yang tidak disertakan. Konfigurasi itu sudah dihapus dari paket build-ready ini agar Android Gradle Plugin menggunakan signing debug bawaan.
- Project tidak menyertakan `google-services.json`. Plugin Google Services sudah disetel ke WARN, jadi ketiadaan file tersebut tidak seharusnya menghentikan build.
- README asli menyebut `GEMINI_API_KEY`, tetapi kode yang diperiksa tidak menunjukkan pemakaian langsung Gemini API pada Activity utama. Jika fitur Gemini memang diperlukan, key harus dikonfigurasi secara aman; jangan menaruh API key langsung di source code atau repository publik.
- Paket build-ready ini mengganti credential Telegram yang tertanam di source dengan placeholder. Credential produksi harus dimasukkan melalui mekanisme konfigurasi yang aman, bukan hard-coded di APK.
