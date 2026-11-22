# KomKy — fork dari Mihon v0.21.3

Proyek ini adalah turunan (fork) dari [Mihon](https://github.com/mihonapp/mihon) v0.21.3
(commit aa2fd41), dilisensikan Apache License 2.0 (lihat `LICENSE`).
Sesuai lisensi, file `LICENSE` dipertahankan dan perubahan dicatat di bawah.

## Perubahan dari Mihon asli (tahap 1: rebrand saja, fungsi tetap sama)

| Apa | File | Isi perubahan |
|---|---|---|
| ID aplikasi | `app/build.gradle.kts` | `app.mihon` -> `id.komky.app` |
| Nama aplikasi | `i18n/src/commonMain/moko-resources/base/strings.xml` | `Mihon` -> `KomKy` |
| Nama proyek Gradle | `settings.gradle.kts` | `Mihon` -> `KomKy` |
| Ikon peluncur | `app/src/main/res/drawable-nodpi/ic_launcher_{foreground,background}.png` | Ikon KomKy (karakter + logo) di atas latar ungu; file XML lama dan versi debug dihapus |
| Ikon notifikasi/monokrom | `ic_mihon.xml`, `ic_launcher_monochrome.xml` | Tetap ikon buku sederhana (satu warna) |
| Daftar paket telemetri | `telemetry/.../TelemetryConfig.kt` | Menyesuaikan ID baru (telemetri nonaktif secara default) |

Nama paket Kotlin (`eu.kanade.*`, `mihon.*`) sengaja TIDAK diubah di tahap ini agar
kode tetap aman dibuild. Redirect login tracker (`mihon://...`) juga dibiarkan.

## Yang perlu kamu ganti sendiri (satu tempat masing-masing)
- Nama tampilan: `i18n/.../base/strings.xml` (`app_name`).
- ID aplikasi: `app/build.gradle.kts` (`applicationId`). Ganti `id.komky.app`
  dengan ID milikmu, misalnya `com.namakamu.komik`.
- Warna ikon: `ic_launcher_background.xml` (`#5B3FD9`).

## Cara build (di komputer kamu)
Kebutuhan: Android Studio terbaru, JDK 21, koneksi internet (untuk unduh dependensi).

1. Ekstrak ZIP, lalu di Android Studio pilih **File > Open** dan buka folder proyek.
2. Tunggu Gradle Sync selesai (pertama kali bisa beberapa menit). Android Studio akan
   menawarkan unduh Android SDK (compileSdk 36) bila belum ada, setujui saja.
3. Pilih build variant **debug**: menu **Build > Select Build Variant**.
4. Jalankan **Build > Build APK(s)**, atau lewat terminal:
   `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
5. APK ada di `app/build/outputs/apk/debug/`. Pakai yang bertulisan `universal`
   bila ada, atau yang sesuai arsitektur HP-mu (biasanya `arm64-v8a`).
6. Build debug berID `id.komky.app.dev`; build release (default `apk.yml`) berID `id.komky.app`.

Untuk APK rilis kamu perlu keystore sendiri (tanda tangan). Bisa dibahas nanti.

## Catatan
- Pengecek update aplikasi dimatikan (default Mihon: hanya aktif dengan flag `enable-updater`),
  supaya tidak menawarkan update dari Mihon asli.
- Aplikasi ini hanya pembaca. Tidak ada sumber komik bawaan. Ekstensi dipasang pengguna sendiri.

## Build lewat GitHub Actions (tanpa Android Studio)
Workflow ada di `.github/workflows/apk.yml`. Workflow bawaan Mihon (release, update website)
sudah dihapus karena butuh rahasia/akses milik tim Mihon.

1. Buat repo baru di GitHub (kosong, tanpa README).
2. Dari folder proyek: `git init`, `git add .`, `git commit -m "KomKy awal"`,
   `git branch -M main`, `git remote add origin <URL-repo-kamu>`, `git push -u origin main`.
   (Atau pakai tombol Source Control di VS Code / GitHub Desktop.)
3. Buka repo di GitHub, tab **Actions**. Build jalan otomatis setiap push, atau klik
   **Build APK > Run workflow** untuk menjalankan manual.
4. Setelah centang hijau (sekitar 10-20 menit), buka run tersebut, bagian **Artifacts**,
   unduh `KomKy-debug-apk` (berupa ZIP berisi APK), ekstrak, lalu pasang di HP.

## Catatan versi 0.21.3
- Mihon 0.21 menambah skema redirect login Comick (`app.mihon://comick-auth`, di `AndroidManifest.xml`
  dan `ComickApi.kt`). Ini sengaja TIDAK diubah, sama seperti redirect tracker lain.
- Dasar kode: versionCode 39, versionName 0.21.3.


## Perubahan tahap 2 (22 Nov 2026)
- **Nama:** KomikKu -> **KomKy** (`strings.xml`, `settings.gradle.kts`, nama artifact di `apk.yml`).
- **ID aplikasi:** `id.komikku.app` -> **`id.komky.app`** (`app/build.gradle.kts`, `TelemetryConfig.kt`).
  Build debug menjadi `id.komky.app.dev`. Android menganggapnya aplikasi BARU: aplikasi lama tidak
  ikut terupdate dan datanya tidak terbawa (backup dulu lewat fitur Backup bila perlu).
- **Layar About** (`AboutScreen.kt`): dihapus menu Apa yang baru, Kebijakan privasi, dan ikon
  Website/Discord/Reddit milik Mihon. Ditambah teks **Tentang kami** (kalimat atribusi Mihon di bawahnya dihapus atas permintaan pemilik),
  dan ikon Instagram, Facebook, X, GitHub milik pemilik. Layar **Lisensi sumber terbuka tetap ada**.
- **Menu Dukung kami, Bantuan, dan ikon bantuan dihapus** (permintaan pemilik, 22 Nov 2026):
  item *Dukung kami* dan *Bantuan* di tab Lainnya (`MoreScreen.kt`, `MoreTab.kt`); popup donasi tidak
  dipanggil lagi (`MainActivity.kt`, fungsi `ShowDonationCampaign` dibiarkan tidak terpakai);
  ikon bantuan di Data dan penyimpanan, Pelacakan (tracking), Mendatang (`UpcomingScreenContent.kt`),
  tombol bantuan penyimpanan di onboarding (`StorageStep.kt`), dan tombol Bantuan di layar sumber
  kosong / menu sumber lokal (`BrowseSourceScreen.kt`, `BrowseSourceToolbar.kt`). `SupportUsScreen.kt`
  masih ada di kode tetapi tidak bisa dibuka dari mana pun.
- **Fitur Poster komik (P1)** (22 Nov 2026, rencana di PRD-Poster-Komik.md): item baru **Poster komik**
  di tab Lainnya (antara Pengaturan dan Tentang). Pilih foto (Photo Picker) + 3-5 komik dari pustaka;
  poster 1080x1920 berisi kartu komik miring (sampul, judul, penulis, progres baca) di atas foto;
  tombol Susun ulang, Watermark KomKy, Simpan (Pictures/KomKy) dan Bagikan. File baru:
  `ui/poster/PosterScreen.kt`, `PosterViewModel.kt`, `PosterRenderer.kt`; diubah: `MoreScreen.kt`, `MoreTab.kt`.
  Dibuat sendiri, tidak menyalin kode/aset/model dari Auralys (GPL-3.0). **Belum dibuild/diuji.**
- **Versi KomKy 0.2.1:** `versionName` di `app/build.gradle.kts` diganti `0.2.1` (basis Mihon v0.21.3,
  `versionCode` tetap 39). Semua tampilan versi memakai satu sumber ini lewat `AboutScreen.getVersionName()`:
  Pengaturan > Tentang menampilkan "KomKy Stable v0.2.1"; baris Versi di Tentang menampilkan
  "Stable v0.2.1 (dd/MM/yy HH.mm)" (format terkunci, waktu build otomatis, zona waktu HP).
  Awalan "-<jumlah commit>" bawaan Mihon pada build debug dihapus (`app/build.gradle.kts`), jadi nama versi selalu bersih. Naikkan `versionCode` setiap kali versi naik. Info debug lengkap tetap tersalin saat baris Versi diketuk.
- **Ikon splash:** `Theme.Tachiyomi.SplashScreen` (`values/themes.xml`) memakai `ic_launcher_foreground`
  (ikon KomKy), bukan lagi `ic_mihon_splash` (buku putih).
- **Logo di dalam aplikasi:** `LogoHeader.kt` memakai `drawable-nodpi/ic_komky_logo.png` (berwarna).
- **Ikon Instagram baru:** `icons/simple-icons/src/main/valkyrieResources/instagram.svg`
  (gambar sederhana buatan sendiri, bukan logo resmi).
- **Workflow:** `apk.yml` menambah langkah `chmod +x ./gradlew`.

## Wajib dipertahankan (Apache 2.0)
File `LICENSE`, layar Lisensi sumber terbuka (tempat atribusi Mihon dan lisensi lengkap), dan
catatan perubahan ini. Jangan memakai nama atau logo Mihon sebagai identitas produk.

## Belum dikerjakan
- Splash screen masih memakai ikon buku sederhana (`ic_mihon_splash.xml`).
- Nama paket Kotlin (`eu.kanade.*`) dan redirect tracker (`mihon://`, `app.mihon://comick-auth`) tetap.
- File `app/google-services.json` bawaan Mihon masih ada (dibersihkan di tahap lain).

## Build ringan (release) dan kunci tanda tangan (23 Nov 2026)
**Kenapa aplikasi terasa berat/delay?** Build *debug* tidak dioptimalkan (tanpa R8, tanpa baseline
profile, Compose berjalan jauh lebih lambat). Mihon resmi memakai build *release*. Karena itu
`apk.yml` sekarang membuat **release** secara default (kode disusutkan, baseline profile ikut) dan
hanya **arm64-v8a** (flag `-Parm64-only`, ditambahkan di `app/build.gradle.kts`).
Build debug tetap bisa dipilih manual: Actions > Build APK > Run workflow > variant `debug`.
Build release lebih lama (sekitar 10-20 menit) tetapi APK lebih kecil dan aplikasi lebih lancar.

**Tanda tangan tetap (opsional, sangat disarankan):** tanpa ini, kunci tanda tangan tiap build
berbeda sehingga APK baru TIDAK bisa menimpa APK lama (harus hapus dulu, data hilang).
Cara membuatnya sekali saja (di laptop, PowerShell, JDK sudah terpasang):

1. `keytool -genkeypair -v -keystore komky.jks -alias komky -keyalg RSA -keysize 2048 -validity 10000`
   (isi password dan data diri; CATAT password. **Simpan `komky.jks` di tempat aman dan JANGAN di-push ke GitHub.**)
2. `[Convert]::ToBase64String([IO.File]::ReadAllBytes("komky.jks")) | Set-Clipboard`
   (hasilnya tersalin ke clipboard)
3. Di repo GitHub: **Settings > Secrets and variables > Actions > New repository secret**, buat 4 secret:
   `KEYSTORE_BASE64` (tempel isi clipboard), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (= `komky`), `KEY_PASSWORD`.
4. Jalankan build lagi. Selama kunci yang sama dipakai, APK baru bisa menimpa APK lama.

Peringatan: kalau `komky.jks` hilang, aplikasi tidak bisa diperbarui lagi (harus hapus dan pasang ulang).
