# SEHATI - Sekolah Hijau dan Anti Plastik

SEHATI adalah sebuah aplikasi Android yang dirancang untuk mendorong lingkungan sekolah yang berkelanjutan dan sehat dengan membiasakan kebiasaan ramah lingkungan di kalangan siswa. Aplikasi ini mengintegrasikan sistem penghargaan koin dan kupon, pelacakan kehadiran, pelaporan pelanggaran, serta fungsionalitas penukaran koin/kupon.

### Fitur

* **Peran Pengguna**: Aplikasi ini mendukung berbagai peran pengguna, masing-masing dengan fungsionalitas khusus:
    * **Admin**: Mengelola akun pengguna untuk Guru dan staf Kantin.
    * **Guru**:
        * Mengelola absensi siswa untuk tindakan ramah lingkungan (membawa tumbler, wadah bekal, dan tas belanja).
        * Melaporkan pelanggaran siswa (misalnya, menggunakan kantong plastik, membeli minuman botol).
        * Melihat statistik absensi mingguan untuk kelas mereka.
    * **Siswa**:
        * Melihat saldo koin mereka saat ini.
        * Memeriksa riwayat pelanggaran mereka.
        * Memantau riwayat penukaran koin mereka.
        * Melihat kupon yang tersedia dan tanggal kedaluwarsanya.
        * Mengakses peringkat kelas berdasarkan akumulasi koin.
    * **Kantin**:
        * Mengelola penukaran koin untuk siswa (untuk barang).
        * Mengelola penukaran kupon untuk siswa (untuk saldo kantin).
        * Melihat saldo kantin mereka saat ini.
        * Meninjau riwayat transaksi.
        * Memungkinkan konversi saldo kantin (koin) ke Rupiah.

* **Sistem Penghargaan**: Siswa mendapatkan koin untuk tindakan ramah lingkungan (membawa tumbler, wadah bekal, tas belanja) dan dapat kehilangan koin karena pelanggaran. Koin-koin ini dapat ditukarkan dengan barang atau diubah menjadi kupon.
* **Sistem Kupon**: Siswa dapat memperoleh kupon dan menukarkannya di kantin dengan rupiah.
* **Peringkat Kelas**: Sistem peringkat mingguan mendorong persaingan antar kelas berdasarkan total koin yang terkumpul, mempromosikan tanggung jawab lingkungan secara kolektif.
* **Integrasi Firebase**: Menggunakan Firebase Realtime Database untuk penyimpanan dan sinkronisasi data real-time, memastikan informasi yang mutakhir di semua peran pengguna.

### Teknologi yang Digunakan

* **Pengembangan Android**: Aplikasi ini dibuat secara native untuk Android.
* **Bahasa Pemrograman**:
    * Java
* **Firebase**:
    * Firebase Realtime Database: Untuk menyimpan dan mengelola data pengguna, absensi, transaksi, dan peringkat.

### Pengaturan dan Instalasi

Untuk menyiapkan proyek SEHATI secara lokal, ikuti langkah-langkah berikut:

1.  **Clon Repositori**:
    ```bash
    git clone https://github.com/alfizamriza/sehati.git
    cd sehati
    ```

2.  **Buka di Android Studio**:
    * Buka Android Studio dan pilih "Open an existing Android Studio project".
    * Navigasikan ke direktori `sehati` yang telah dikloning dan buka.

3.  **Pengaturan Proyek Firebase**:
    * Buat proyek Firebase baru di Firebase Console.
    * Tambahkan aplikasi Android ke proyek Firebase Anda.
    * Daftarkan `com.example.sehati` sebagai nama paket (seperti yang terlihat di `app/build.gradle.kts` dan `app/src/main/AndroidManifest.xml`).
    * Unduh file `google-services.json` dari pengaturan proyek Firebase Anda dan letakkan di direktori `app/` proyek Android Anda. (Contoh `google-services.json` disediakan dalam file yang diunggah).
    * Aktifkan **Realtime Database** di proyek Firebase Anda. Pastikan aturan keamanan dikonfigurasi untuk memungkinkan akses baca/tulis untuk tujuan pengujian, atau terapkan otentikasi dan otorisasi yang tepat.

4.  **Sinkronkan Gradle**:
    * Setelah proyek dibuka, Android Studio akan meminta Anda untuk menyinkronkan file Gradle. Izinkan untuk menyinkronkan dependensi.
    * Proyek ini menggunakan Gradle 8.10.2.

5.  **Build dan Jalankan**:
    * Hubungkan perangkat Android atau mulai AVD (Android Virtual Device).
    * Klik tombol "Run 'app'" di Android Studio untuk membangun dan menginstal aplikasi di perangkat/emulator Anda.

### Sorotan Struktur Proyek

* `app/src/main/java/com/example/sehati/`: Berisi kode sumber Java dan Kotlin utama untuk aktivitas, fragmen, dan model data.
    * `Login.java`: Menangani otentikasi pengguna untuk semua peran (Admin, Guru, Siswa, Kantin).
    * `AdminActivity.java`: Dashboard admin dengan opsi untuk mendaftarkan guru dan staf kantin.
    * `Guru.java`, `HomeGuruFragment.java`, `AbsenFragment.java`, `PelaporanFragment.java`: Fungsionalitas khusus guru untuk absensi dan pelaporan pelanggaran.
    * `Murid.java`, `HomeMuridFragment.java`, `PeringkatMuridFragment.java`, `kuponMuridFragment.java`: Fungsionalitas khusus siswa untuk melihat koin, riwayat, peringkat, dan kupon.
    * `Kantin.java`, `HomeKantinFragment.java`, `PenukaranKoinFragment.java`, `PenukaranKuponFragment.java`, `TukarRupiah.java`: Fungsionalitas khusus kantin untuk penukaran koin/kupon dan pengelolaan saldo.
    * Model Data (`Guru.java`, `Kantin.java`, `Murid.java`): Merepresentasikan struktur pengguna.
* `app/src/main/res/`: Berisi file sumber daya Android (layout, drawable, menu, values).
    * `layout/`: Mendefinisikan UI untuk berbagai aktivitas dan fragmen.
    * `drawable/`: Berisi berbagai aset drawable yang digunakan dalam aplikasi, seperti ikon untuk absensi, koin, kupon, dan elemen navigasi.
    * `menu/`: Mendefinisikan menu navigasi bawah untuk berbagai peran pengguna.
    * `values/colors.xml`: Mendefinisikan warna kustom yang digunakan di seluruh aplikasi.
    * `font/montserat.xml`: Mendefinisikan font kustom yang digunakan dalam aplikasi.

### Penggunaan

1.  **Login**: Gunakan aktivitas `Login` untuk masuk dengan berbagai peran pengguna (Admin, Guru, Siswa, Kantin). Awalnya, Anda mungkin perlu membuat pengguna "Admin" secara manual di Firebase Realtime Database Anda untuk mendaftarkan pengguna lain.
    * ID aplikasi adalah `com.example.sehati`.
    * Aplikasi ini dikompilasi dengan SDK versi 35 dan memiliki SDK minimum 26.
    * Kompatibilitas sumber dan target diatur ke Java 11.
2.  **Admin**: Daftarkan guru dan staf kantin baru melalui dashboard Admin.
3.  **Guru**: Tandai absensi siswa setiap hari dan laporkan pelanggaran. Pantau statistik absensi mingguan.
4.  **Siswa**: Lacak kegiatan ramah lingkungan, periksa saldo koin, lihat riwayat pelanggaran dan penukaran, serta lihat peringkat kelas.
5.  **Kantin**: Memfasilitasi penukaran koin dan kupon dengan siswa, mengelola saldo kantin, dan melihat catatan transaksi.

Aplikasi ini bertujuan untuk menciptakan pendekatan yang lebih terorganisir dan terinsentif untuk keberlanjutan lingkungan dalam lingkungan sekolah.
