# XSecurity

**Unlocks for the HyperOS Security app, Battery and performance, Game Turbo and the system updater.**
An LSPosed module for the HyperOS 4 China ROM.

[Türkçe açıklama aşağıda](#türkçe)

## Why
The China ROM hides many features of the Security app: Game Turbo tools, the 60 FPS limit for games that are not on Xiaomi's list, battery health details, smart charging options, the full Toolbox and more. Modded Security APKs bring them back, but they replace a system app and break with every update.

XSecurity does the same on the **stock** apps with LSPosed hooks. Nothing is replaced, every feature has its own switch, and it keeps working after app updates.

## Features
### 🎮 Games
- Remove the 60 FPS cap: games run at the refresh rate chosen in Settings (120/144 Hz)
- Wild Boost, Balanced / Performance switch and performance mode
- Super resolution and frame insertion, game HDR, dynamic performance, touch settings, Mi Time
- Crosshair in every game, macros in every game, no game toasts over the game
- Game network tools: Wi-Fi low latency and network optimisation
- Joyose GPU tuner; optionally stop Joyose cloud updates

### 🔋 Battery and charging
- App battery ranking with screen and calls, real screen-on time
- Battery health %, temperature, cycles, real and design capacity on the battery protection page
- Smart charging, super wireless charging, night and smart charge protection options
- Battery life features, low-temperature endurance, extra power features
- Optional: quiet low battery warning

### 🌐 Network
- Mobile data stays on when the data plan runs out (only a warning)
- The hotspot stays on at its daily limit (only a warning)
- Data used this month in the shade and Control Center

### 📱 App info
- Package name and Open / Path / Open by default buttons
- Disable and Share APK for every app
- Version code, UID, data folder, APK file, minimum and target SDK (tap to copy)

### ⚙️ System
- Full global Toolbox, 50 apps in the sidebar, no 5-10 second permission countdown
- Video toolbox (theatre mode, Dolby, MEMC, AI picture, new video box) that stays on when the phone warms up
- Call toolbox: gestures, effects and beauty in video calls
- Second space and dual apps, car accident detection, car sickness relief, storage defragmentation (FBO)
- Optional: shuffled app lock PIN pad

### 📦 System updater
- No "Turn on automatic updates" dialog
- Optional, dangerous: remove OTA verification (VAB devices only)

### 🎨 App
- HyperOS (Miuix) or Material You look, light / dark / system theme, wallpaper colors, blurred top bar
- English, Türkçe, Русский, Español, 简体中文

## Screenshots
| Home | Games | Charging |
|---|---|---|
| <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/home.png" width="260"> | <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/games.png" width="260"> | <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/charging.png" width="260"> |
| **Settings** | **App info** | **More app details** |
| <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/settings.png" width="260"> | <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/app_info.png" width="260"> | <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/app_details.png" width="260"> |

The phone's language is Turkish, so some Security app labels are Turkish (Aç = Open, Yol = Path).

## Requirements
- A Xiaomi phone on the HyperOS 4 China ROM (Android 17)
- Root (KernelSU, SukiSU or Magisk) and LSPosed (API 102)

Tested on the Xiaomi 15 Ultra with Security 13.5.8 and 13.6.5.

## Install
1. Install XSecurity.
2. Enable it in LSPosed and tick the scope: Security, Battery and performance, Updater and Joyose.
3. Grant XSecurity its LSPosed permission, otherwise the app shows "Not active".
4. Choose your switches and tap **Restart the apps** (needs root), or reboot.

## Source code and support
- Source: https://github.com/Leekin2/XSecurity (AGPL-3.0)
- Feedback and complaints on Telegram: [@emcleek](https://t.me/emcleek)
- Support group: [Xgoogle for HyperOS](https://t.me/Xgooogle)

---

## Türkçe

**HyperOS Güvenlik uygulaması, Pil ve performans, Oyun Turbo ve sistem güncelleyicisi için kilit açıcı.**
HyperOS 4 Çin ROM'u için bir LSPosed modülü.

### Neden
Çin ROM'u Güvenlik uygulamasının birçok özelliğini gizliyor: Oyun Turbo araçları, Xiaomi'nin listesinde olmayan oyunlarda 60 FPS sınırı, pil sağlığı ayrıntıları, akıllı şarj seçenekleri, tam Araç kutusu ve daha fazlası. Modlu Güvenlik APK'ları bunları geri getiriyor ama bir sistem uygulamasının yerine geçiyor ve her güncellemede bozuluyor.

XSecurity aynısını LSPosed hook'larıyla **orijinal** uygulamalarda yapar. Hiçbir şeyin yerine geçmez, her özelliğin kendi anahtarı vardır ve uygulama güncellense de çalışmaya devam eder.

### Özellikler
#### 🎮 Oyunlar
- 60 FPS kilidi kalkar: oyunlar Ayarlar'da seçtiğiniz yenileme hızında (120/144 Hz) çalışır
- Wild Boost, Dengeli / Performans geçişi ve performans modu
- Süper çözünürlük ve kare ekleme, oyun HDR, dinamik performans, dokunma ayarları, Mi Time
- Her oyunda nişangah, her oyunda makro, oyunun üstünde bildirim çıkmaz
- Oyun ağ araçları: Wi-Fi düşük gecikme ve ağ optimizasyonu
- Joyose GPU ayarlayıcı; isteğe bağlı Joyose bulut güncellemesini kapatma

#### 🔋 Pil ve şarj
- Ekran ve aramalar dahil uygulama pil sıralaması, gerçek ekran süresi
- Pil koruması sayfasında sağlık %, sıcaklık, döngü, gerçek ve tasarım kapasitesi
- Akıllı şarj, süper kablosuz şarj, gece ve akıllı şarj koruması seçenekleri
- Pil ömrü özellikleri, düşük sıcaklıkta uzun pil ömrü, ek güç özellikleri
- İsteğe bağlı: düşük pil uyarısını susturma

#### 🌐 Ağ
- Veri paketi bitince mobil veri kapanmaz (yalnızca uyarı)
- Hotspot günlük sınırında kesilmez (yalnızca uyarı)
- Bu ay kullanılan veri bildirim panelinde ve Kontrol merkezinde

#### 📱 Uygulama bilgisi
- Paket adı ve Aç / Yol / Varsayılan aç düğmeleri
- Her uygulamada Devre dışı ve APK paylaş
- Sürüm kodu, UID, veri klasörü, APK dosyası, en düşük ve hedef SDK (dokununca kopyalanır)

#### ⚙️ Sistem
- Global sürümdeki tam Araç kutusu, yan çubukta 50 uygulama, 5-10 saniyelik izin geri sayımı yok
- Telefon ısınınca kapanmayan video araç kutusu (sinema modu, Dolby, MEMC, yapay zekâ görüntü, yeni video kutusu)
- Arama araç kutusu: görüntülü aramada hareketler, efektler ve güzellik
- İkinci alan ve çift uygulama, kaza algılama, araç tutması rahatlatma, depolama birleştirme (FBO)
- İsteğe bağlı: uygulama kilidinde karışık PIN klavyesi

#### 📦 Sistem güncelleyici
- "Otomatik güncellemeler açılsın mı?" diyaloğu çıkmaz
- İsteğe bağlı, tehlikeli: OTA doğrulamasını kaldırma (yalnızca VAB cihazlar)

#### 🎨 Uygulama
- HyperOS (Miuix) veya Material You görünümü, açık / koyu / sistem teması, duvar kâğıdı renkleri, bulanık üst çubuk
- English, Türkçe, Русский, Español, 简体中文

### Gereksinimler
- HyperOS 4 Çin ROM'lu (Android 17) bir Xiaomi telefon
- Root (KernelSU, SukiSU veya Magisk) ve LSPosed (API 102)

Xiaomi 15 Ultra'da Güvenlik 13.5.8 ve 13.6.5 ile test edildi.

### Kurulum
1. XSecurity'yi kurun.
2. LSPosed'da etkinleştirin ve kapsamı işaretleyin: Güvenlik, Pil ve performans, Güncelleyici ve Joyose.
3. XSecurity'ye LSPosed iznini verin; yoksa uygulama "Etkin değil" gösterir.
4. Anahtarlarınızı seçip **Uygulamaları yeniden başlat**'a dokunun (root gerekir) ya da telefonu yeniden başlatın.

### Kaynak kod ve destek
- Kaynak: https://github.com/Leekin2/XSecurity (AGPL-3.0)
- Geri bildirim ve şikayet için Telegram: [@emcleek](https://t.me/emcleek)
- Destek grubu: [Xgoogle for HyperOS](https://t.me/Xgooogle)
