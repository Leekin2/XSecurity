# XSecurity

> [!IMPORTANT]
> **XSecurity has moved to [XApps](https://github.com/Leekin2/XApps).** XGoogle, XSecurity and XNotify are now one app and one root module.
> Download **XApps** from its [latest release](https://github.com/Leekin2/XApps/releases/latest). Flashing the XApps zip moves your XSecurity settings over and removes XSecurity by itself.
> This repository stays for history; new versions are released only in XApps.
>
> **XSecurity artık [XApps](https://github.com/Leekin2/XApps) içinde.** Yeni sürümler yalnızca XApps olarak yayınlanıyor; XApps zip'i ayarlarınızı taşır ve XSecurity'i kendisi kaldırır.


**Unlocks for the HyperOS Security app, Battery and performance, Game Turbo and the system updater.**
An LSPosed module for the HyperOS 4 China ROM.

[Türkçe açıklama aşağıda](#türkçe)

## Why
The China ROM hides many features of the Security app: Game Turbo tools, the 60 FPS limit for games that are not on Xiaomi's list, battery health details, smart charging options, the full Toolbox and more. Modded Security APKs bring them back, but they replace a system app and break with every update.

XSecurity enables these features on the **stock** apps with LSPosed hooks. No system APK replacement is required, and each feature has its own switch. Compatibility depends on the ROM and app versions.

## Features
### 🎮 Games
- Remove the 60 FPS cap: games run at the refresh rate chosen in Settings (120/144 Hz)
- Wild Boost, Balanced / Performance switch and performance mode
- Super resolution and frame insertion, game HDR, dynamic performance, touch settings, Mi Time
- Crosshair in every game, macros in every game, no game toasts over the game
- Game network tools: Wi-Fi low latency and network optimisation
- Joyose GPU tuner; optionally stop Joyose cloud updates
- Optional: keep the Game Turbo master switch enabled (does not force a permanent game session)

### 🔋 Battery and charging
- App battery ranking with screen and calls, real screen-on time
- Battery health %, temperature, cycles, real and design capacity on the battery protection page
- Smart charging, super wireless charging, night and smart charge protection options
- Battery life features, low-temperature endurance, extra power features
- Optional: quiet low battery warning
- Separate optional switches keep refresh rate and brightness in Battery Saver; thermal limits remain active

### 🌐 Network
- Mobile data stays on when the data plan runs out (only a warning)
- The hotspot stays on at its daily limit (only a warning)
- Data used this month in the shade and Control Center
- Optional: extend Xiaomi's Wi-Fi/background network controls to eligible system apps; configure rules in Security → Data usage
- Optional: the Global ROM's original Vo LTE / Vo NR / Vo WiFi status bar icons on the China ROM, driven by the real IMS registration (System UI scope)
- Optional: Xiaomi's own **Use VoLTE** switch and Wi-Fi calling **Network preferences** on the SIM card settings page, kept after reboot (Phone scope)

### 📱 App info
- Package name and Open / Path / Open by default buttons
- Disable and Share APK for every app
- Version code, UID, data folder, APK file, minimum and target SDK (tap to copy)
- Open in Play Store in additional app details; falls back to the Play website if the app is unavailable
- A chain icon for Open by default in the header, with the duplicate lower row removed

- Separate **App info buttons** switch controls Open / Path / Open by default, while the main action switch retains package details and menu actions. Existing installs inherit their previous header setting.
- Optional **Tap app icon to launch** works independently of the header buttons. Turning off header buttons retains the stock Open by default row.

### Messages and Launcher (optional)
- Xiaomi Messages: hide promotional controls, separately hide fraud or overseas risk labels, optionally select the newer interface, and copy explicitly marked numeric 4–8 digit verification codes. Messages 18's notification factory and broadcast PendingIntent path are supported. Risk-warning options reduce the warnings you see and default to off. Codes are marked sensitive, never logged or saved to files.
- Xiaomi Launcher: disable app prestart checks and hide the uninstall-dialog report option without changing the system property store or the actual package installer identity.
- Requires Messages (`com.android.mms`) / Launcher (`com.miui.home`) scope and restarting the affected app. Supports the matching OEM methods only; unavailable targets are logged and retain stock behavior. Google Messages, other launchers and native-only OEM paths are not covered. New options are off by default; runtime testing on the target ROM is still required.
- Native-only Launchers show an unavailable explanation for the two Launcher options. The installed HyperOS 4 Launcher 8.01.02.8593 cannot use these Java hooks; another ROM's native patch is not included.
- Optional **Keep Battery Saver at boot** honors the existing **Turn off when charged** preference when Security's BootReceiver requests its extra saver-close check. It does not enable Saver itself or intercept manual changes and schedules. Requires Security scope and restarting the relevant process; a real reboot test remains necessary.

### ⚙️ System
- Full global Toolbox, 50 apps in the sidebar, no 5-10 second permission countdown
- Video toolbox (theatre mode, Dolby, MEMC, AI picture, new video box) that stays on when the phone warms up
- Call toolbox: gestures, effects and beauty in video calls
- Second space and dual apps, car accident detection, car sickness relief, storage defragmentation (FBO)
- Dark mode: list every installed app under "Individual apps", with a search action for app names and package names. Clear the query to restore the list without changing app toggles. Search also requires Settings (`com.android.settings`) in LSPosed scope.
- App store: show a chooser for `market://` links instead of forcing GetApps, and open GetApps detail pages in a floating window
- Background clipboard access for selected apps, with an app picker
- Optional: stop Xiaomi Guard Provider from uploading the installed app list
- Optional: prevent an applied theme from resetting to default
- Optional: shuffled app lock PIN pad
- Optional: a separate shuffled phone lock-screen PIN keypad, requiring System UI scope; digit verification and emergency/delete controls are preserved

The new optional switches default to off. Framework display/capture and signature-verification changes require **System Framework** scope (`system` in modern libxposed) and a reboot. Scope selection is fixed to the module's twelve system/app targets; unrelated apps are hidden. Opening XSecurity requests missing targets and removes obsolete scope entries. A compiled hook is not proof that every ROM implements its target; unavailable targets keep stock behaviour and are logged.

### Signature verification bypass and installation options
- Twelve separate, off-by-default switches: downgrade, digest verification, resource-table alignment, signer comparison, exact matching, installed-signature compatibility, system hidden APIs, shared UID, verification agent, vendor blocklist, persistent system app updates and SystemUI UID preservation
- Shared UID needs signer-comparison bypass. The vendor blocklist target is specific to supported ROMs such as Nothing OS.
- Signer recovery uses an installed certificate or a certificate present in the APK. Missing ROM targets are reported in the LSPosed log. An update must retain the installed package's shared-user identity; these options do not migrate it to another UID.
- Persistent update hooks support both framework preparation method names and record limited installation diagnostics in LSPosed logs.
- Optional SystemUI UID preservation uses the phone’s recorded system shared-user identity for parsed updates, including cached parses at boot. It never migrates installed UID ownership or rewrites APKs. The module must remain active with differently-declared updates. Android/API and ROM compatibility checks remain in force.

### Screenshots and screen recording
- The **Remove screenshot block** switch relaxes supported `FLAG_SECURE` capture restrictions, including relevant HyperOS screenshot and screen-recording checks.
- Requires System Framework scope and a reboot. DRM-protected content is not guaranteed capturable.
- The Background group and developer package/certificate test mode have been removed, including their hooks.

### 📦 System updater
- No "Turn on automatic updates" dialog
- Optional, dangerous: remove OTA verification (VAB devices only)

### 🎨 App
- HyperOS (Miuix) or Material You look, light / dark / system theme, wallpaper colors, blurred top bar
- English, Türkçe, Русский, Español, 简体中文
- Hide the home screen icon; once hidden, open the app from LSPosed

## Screenshots
| Home | Games | Charging |
|---|---|---|
| <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/home.png" width="260"> | <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/games.png" width="260"> | <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/charging.png" width="260"> |
| **Settings** | **App info** | **More app details** |
| <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/settings.png" width="260"> | <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/app_info.png" width="260"> | <img src="https://raw.githubusercontent.com/Leekin2/XSecurity/main/screenshots/app_details.png" width="260"> |

The phone's language is Turkish, so some Security app labels are Turkish (Aç = Open, Yol = Path).


> **Advanced users only:** persistent system-app updates and SystemUI UID preservation may cause SystemUI crashes or prevent booting with an incompatible APK. Enable only if you know how to recover your device. Keep XSecurity and UID preservation enabled while a differently-declared SystemUI update is installed. A tester reported a successful update; this is not a guarantee for other ROMs or subsequent reboots.

## Requirements
- A Xiaomi phone on the HyperOS 4 China ROM (Android 17)
- Root (KernelSU, SukiSU or Magisk) and LSPosed (API 102)

Tested on the Xiaomi 15 Ultra with Security 13.5.8 and 13.6.5.

## Install
1. Install XSecurity.
2. Enable it in LSPosed and tick the scope: System Framework, Security, Battery and performance, Updater, Joyose, Themes, Guard Provider, Settings, System UI, Phone, Messages and Launcher.
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

XSecurity bu özellikleri LSPosed hook'larıyla **orijinal** uygulamalarda açar. Sistem APK'sını değiştirmek gerekmez ve her özelliğin kendi anahtarı vardır. Uyumluluk ROM ve uygulama sürümlerine bağlıdır.

### Özellikler
#### 🎮 Oyunlar
- 60 FPS kilidi kalkar: oyunlar Ayarlar'da seçtiğiniz yenileme hızında (120/144 Hz) çalışır
- Wild Boost, Dengeli / Performans geçişi ve performans modu
- Süper çözünürlük ve kare ekleme, oyun HDR, dinamik performans, dokunma ayarları, Mi Time
- Her oyunda nişangah, her oyunda makro, oyunun üstünde bildirim çıkmaz
- Oyun ağ araçları: Wi-Fi düşük gecikme ve ağ optimizasyonu
- Joyose GPU ayarlayıcı; isteğe bağlı Joyose bulut güncellemesini kapatma
- İsteğe bağlı: Oyun Turbo ana anahtarı açık kalır; oyun oturumunu sürekli açık tutmaz

#### 🔋 Pil ve şarj
- Ekran ve aramalar dahil uygulama pil sıralaması, gerçek ekran süresi
- Pil koruması sayfasında sağlık %, sıcaklık, döngü, gerçek ve tasarım kapasitesi
- Akıllı şarj, süper kablosuz şarj, gece ve akıllı şarj koruması seçenekleri
- Pil ömrü özellikleri, düşük sıcaklıkta uzun pil ömrü, ek güç özellikleri
- İsteğe bağlı: düşük pil uyarısını susturma
- Pil tasarrufunda yenileme hızını ve parlaklığı korumak için ayrı seçenekler; termal sınırlar korunur

#### 🌐 Ağ
- Veri paketi bitince mobil veri kapanmaz (yalnızca uyarı)
- Hotspot günlük sınırında kesilmez (yalnızca uyarı)
- Bu ay kullanılan veri bildirim panelinde ve Kontrol merkezinde
- İsteğe bağlı: uygun sistem uygulamalarına Xiaomi'nin Wi-Fi/arka plan ağ kontrolleri; kurallar Güvenlik → Veri kullanımı ekranından seçilir
- İsteğe bağlı: Çin ROM'da global ROM'un orijinal Vo LTE / Vo NR / Vo WiFi durum çubuğu simgeleri; gerçek IMS kaydına göre görünür (Sistem Arayüzü kapsamı)
- İsteğe bağlı: SIM kartı ayarlarında Xiaomi'nin kendi **VoLTE kullan** anahtarı ve Wi-Fi araması **Ağ tercihleri**; yeniden başlatınca kaybolmaz (Telefon kapsamı)

#### 📱 Uygulama bilgisi
- Paket adı ve Aç / Yol / Varsayılan aç düğmeleri
- Her uygulamada Devre dışı ve APK paylaş
- Sürüm kodu, UID, veri klasörü, APK dosyası, en düşük ve hedef SDK (dokununca kopyalanır)
- Ek uygulama ayrıntılarında Play Store’da aç; mağaza uygulaması yoksa Play web sayfası açılır
- Üstte zincir simgeli Varsayılan aç düğmesi; alttaki tekrarlanan satır kaldırıldı

- Ayrı **Uygulama bilgisi düğmeleri** anahtarı Aç / Yol / Varsayılan aç satırını yönetir; ana seçenek paket ayrıntılarını ve menü işlemlerini korur. Güncellemede eski düğme tercihi devralınır.
- İsteğe bağlı **Uygulama simgesine dokunarak aç**, üst düğmelerden bağımsızdır. Düğmeler kapatıldığında özgün Varsayılan aç satırı korunur.

#### Mesajlar ve Başlatıcı (isteğe bağlı)
- Xiaomi Mesajlar: tanıtım öğelerini gizleme, dolandırıcılık/yurt dışı risk etiketlerini ayrı yönetme, isteğe bağlı yeni arayüz seçimi ve açıkça doğrulama kodu olarak işaretlenmiş 4–8 haneli kodları kopyalama. Mesajlar 18’in bildirim oluşturucusu ve broadcast PendingIntent yolu desteklenir. Risk uyarısı seçenekleri görünen uyarıları azaltır ve varsayılan kapalıdır. Kodlar hassas işaretlenir, günlüğe veya dosyaya yazılmaz.
- Xiaomi Başlatıcı: sistem özelliklerini veya paketin gerçek yükleme kaynağını değiştirmeden ön başlatma kontrollerini kapatma ve kaldırma penceresindeki rapor seçeneğini gizleme.
- Mesajlar (`com.android.mms`) / Başlatıcı (`com.miui.home`) kapsamı ve ilgili uygulamanın yeniden başlatılması gerekir. Yalnızca eşleşen OEM metotları desteklenir; bulunmayan hedefler günlüğe yazılır ve özgün davranış korunur. Google Mesajlar, diğer başlatıcılar ve yalnızca native kod kullanan OEM yolları kapsam dışındadır. Yeni seçenekler varsayılan kapalıdır; hedef ROM üzerinde çalışma testi henüz gerekir.
- Yerel kod kullanan Başlatıcılarda bu iki seçenek için desteklenmediğini açıklayan bilgi gösterilir. Telefondaki HyperOS 4 Başlatıcı 8.01.02.8593 bu Java hook’larını çalıştırmaz; başka ROM’a ait yerel yama dahil edilmez.
- İsteğe bağlı **Pil tasarrufunu açılışta koru**, Güvenlik’in BootReceiver içindeki ek kapatma denetimini mevcut **Şarj olduğunda kapat** tercihine bağlar. Tasarrufu kendiliğinden açmaz; elle değişiklikleri ve zamanlamaları engellemez. Güvenlik kapsamı ve ilgili sürecin yeniden başlatılması gerekir; gerçek yeniden başlatma testi ayrıca yapılmalıdır.

#### ⚙️ Sistem
- Global sürümdeki tam Araç kutusu, yan çubukta 50 uygulama, 5-10 saniyelik izin geri sayımı yok
- Telefon ısınınca kapanmayan video araç kutusu (sinema modu, Dolby, MEMC, yapay zekâ görüntü, yeni video kutusu)
- Arama araç kutusu: görüntülü aramada hareketler, efektler ve güzellik
- İkinci alan ve çift uygulama, kaza algılama, araç tutması rahatlatma, depolama birleştirme (FBO)
- Karanlık mod: "Tek tek uygulamalar" altında tüm uygulamalar ve ad/paket adına göre arama düğmesi. Aramayı temizlemek uygulama anahtarlarını değiştirmeden listeyi geri getirir. Arama için LSPosed kapsamında Ayarlar (`com.android.settings`) da seçilmelidir.
- Uygulama mağazası: `market://` bağlantılarında GetApps'e zorlamak yerine seçici göster, GetApps detaylarını kayan pencerede aç
- Seçilen uygulamalar için arka planda pano okuma ve uygulama seçim ekranı
- İsteğe bağlı: Xiaomi Guard Provider’ın yüklü uygulama listesini yüklemesini engelleme
- İsteğe bağlı: uygulanan temanın varsayılana sıfırlanmasını önleme
- İsteğe bağlı: uygulama kilidinde karışık PIN klavyesi
- İsteğe bağlı: telefonun kilit ekranı için ayrı karışık PIN seçeneği; Sistem Arayüzü kapsamı gerekir. PIN doğrulaması ve acil arama/silme tuşları korunur

Yeni seçenekler varsayılan kapalıdır. Framework ekran/ekran yakalama ve imza doğrulama seçenekleri için **System Framework** kapsamı (modern libxposed'da `system`) ve yeniden başlatma gerekir. Kapsam, modülün kullandığı on iki sistem/uygulama hedefiyle sabittir; ilgisiz uygulamalar gizlenir. XSecurity açıldığında eksik hedefler istenir ve eski kapsam kayıtları kaldırılır. Derlemenin geçmesi her ROM'da çalıştığı anlamına gelmez; bulunamayan hedeflerde özgün davranış korunur ve günlüğe yazılır.

#### İmza doğrulamasını atlama ve kurulum seçenekleri
- Varsayılan kapalı 12 ayrı seçenek: sürüm düşürme, özet doğrulama, kaynak tablosu hizalama, imza karşılaştırma, tam eşleşme, yüklü imza uyumluluğu, sistem gizli API'leri, shared UID, doğrulama ajanı, üretici engel listesi, kalıcı sistem uygulamalarını güncelleme ve SystemUI UID koruması
- Shared UID için imza karşılaştırmayı atlama da gerekir. Üretici engel listesi hedefi Nothing OS gibi desteklenen ROM'lara özeldir.
- İmza kurtarma, yüklü sertifikayı veya APK içindeki sertifikayı kullanır. ROM'da bulunmayan hedefler LSPosed günlüğüne yazılır. Güncelleme, yüklü paketin ortak kullanıcı kimliğini korumalıdır; bu seçenekler paketi başka bir UID’ye taşımaz.
- Kalıcı uygulama güncelleme hook’ları eski ve yeni framework metot adlarını destekler; LSPosed günlüğüne sınırlı kurulum tanılaması yazar.
- İsteğe bağlı SystemUI UID koruması, güncellemeler için telefonda kayıtlı kimliği kullanır; mevcut UID sahipliğini veya APK dosyasını değiştirmez. Farklı kimlik bildiren güncellemeler kullanılırken modül etkin kalmalıdır. Android/API ve ROM uyumluluğunu sağlamaz.

#### Ekran görüntüsü ve ekran kaydı
- **Ekran görüntüsü engelini kaldır** seçeneği, desteklenen `FLAG_SECURE` yakalama kısıtlamalarını ve ilgili HyperOS ekran görüntüsü/kayıt kontrollerini esnetir.
- System Framework kapsamı ve yeniden başlatma gerekir. DRM korumalı içeriğin yakalanması garanti edilmez.
- Arka plan bölümü ve paket/sertifika seçilen geliştirici test modu, hook kodları dahil kaldırıldı.

#### 📦 Sistem güncelleyici
- "Otomatik güncellemeler açılsın mı?" diyaloğu çıkmaz
- İsteğe bağlı, tehlikeli: OTA doğrulamasını kaldırma (yalnızca VAB cihazlar)

#### 🎨 Uygulama
- HyperOS (Miuix) veya Material You görünümü, açık / koyu / sistem teması, duvar kâğıdı renkleri, bulanık üst çubuk
- English, Türkçe, Русский, Español, 简体中文
- Ana ekran simgesini gizle; gizlendikten sonra uygulamayı LSPosed'den açın


> **Yalnızca ileri düzey kullanıcılar:** kalıcı sistem uygulaması güncellemeleri ve SystemUI UID koruması, uyumsuz APK ile SystemUI çökmesine veya telefonun açılmamasına yol açabilir. Yalnızca cihazınızı kurtarmayı biliyorsanız etkinleştirin. Farklı kimlik bildiren SystemUI güncellemesi yüklüyken XSecurity ve UID koruması etkin kalmalıdır. Bir test kullanıcısı başarılı güncelleme bildirdi; diğer ROM’lar veya sonraki yeniden başlatmalar için garanti değildir.

### Gereksinimler
- HyperOS 4 Çin ROM'lu (Android 17) bir Xiaomi telefon
- Root (KernelSU, SukiSU veya Magisk) ve LSPosed (API 102)

Xiaomi 15 Ultra'da Güvenlik 13.5.8 ve 13.6.5 ile test edildi.

### Kurulum
1. XSecurity'yi kurun.
2. LSPosed'da etkinleştirin ve kapsamı işaretleyin: Sistem Çerçevesi, Güvenlik, Pil ve performans, Güncelleyici, Joyose, Temalar, Guard Provider, Ayarlar, Sistem Arayüzü, Telefon, Mesajlar ve Başlatıcı.
3. XSecurity'ye LSPosed iznini verin; yoksa uygulama "Etkin değil" gösterir.
4. Anahtarlarınızı seçip **Uygulamaları yeniden başlat**'a dokunun (root gerekir) ya da telefonu yeniden başlatın.

### Kaynak kod ve destek
- Kaynak: https://github.com/Leekin2/XSecurity (AGPL-3.0)
- Geri bildirim ve şikayet için Telegram: [@emcleek](https://t.me/emcleek)
- Destek grubu: [Xgoogle for HyperOS](https://t.me/Xgooogle)
