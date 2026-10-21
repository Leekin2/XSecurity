# XSecurity 1.4 — release notes (DRAFT)

> Draft. Review and edit before publishing. The "Developer's own additions"
> section is left for you to fill in.

## English

### New features
- **Hide home screen icon** — a switch in the App section (between Open LSPosed and About). Once hidden, open the app from LSPosed. Asks before hiding.
- **Dark mode: list all apps** — the "Individual apps" list shows every installed user app instead of just a few. *(System Framework scope + reboot.)*
- **App store redirects** — show a chooser for `market://` links instead of forcing GetApps, so you can pick another store such as Google Play; optionally open GetApps detail pages in a floating window. *(System Framework scope + reboot.)*
- **Gallery: Google Photos backup** — enables the Google backup-server option in the system gallery, as on the global ROM. *(Gallery scope; stock gallery.)*

### Fixes & improvements
- **Settings never reset on update** — a new install starts with every switch off; an existing install keeps all its choices across updates (defaults are frozen per install).
- **Shuffled app-lock PIN pad** — now actually shuffles: it runs after the keypad is laid out and re-shuffles on every unlock.
- **Battery usage ranking** — guards against physically impossible per-app mAh values on some ROMs (the list no longer shows nonsense; percentages were already correct).

### Notes
- New switches are off by default; turn on what you want.
- Framework features (dark mode, app store) need the **System Framework** scope ticked and a reboot.
- Licensed AGPL-3.0; some hooks are re-implementations of HyperCeiler rules (see NOTICE).

### Developer's own additions
<!-- Add here the features you implemented yourself. -->

---

## Türkçe

### Yeni özellikler
- **Ana ekran simgesini gizle** — App bölümünde (Open LSPosed ile About arasında) bir anahtar. Gizlenince uygulama LSPosed'den açılır. Gizlemeden önce sorar.
- **Karanlık mod: tüm uygulamaları listele** — "Tek tek uygulamalar" listesinde birkaç uygulama yerine yüklü tüm kullanıcı uygulamaları görünür. *(System Framework kapsamı + yeniden başlatma.)*
- **Uygulama mağazası yönlendirmeleri** — `market://` bağlantılarında GetApps'e zorlamak yerine seçici göster (Google Play gibi başka mağaza seçilebilir); isteğe bağlı GetApps detaylarını kayan pencerede aç. *(System Framework kapsamı + yeniden başlatma.)*
- **Galeri: Google Fotoğraflar yedekleme** — sistem galerisinde Google yedekleme sunucusu seçeneğini açar (global ROM'daki gibi). *(Galeri kapsamı; stok galeri.)*

### Düzeltmeler ve iyileştirmeler
- **Güncellemede ayarlar sıfırlanmaz** — yeni kurulumda her şey kapalı başlar; mevcut kurulum güncellemelerde tüm tercihlerini korur (varsayılanlar kuruluma göre dondurulur).
- **Karışık uygulama kilidi PIN klavyesi** — artık gerçekten karışıyor: tuşlar yerleştikten sonra ve her açılışta yeniden karışıyor.
- **Pil kullanım sıralaması** — bazı ROM'larda fiziksel olarak imkânsız uygulama başına mAh değerlerine karşı koruma (liste artık saçma göstermiyor; yüzdeler zaten doğruydu).

### Notlar
- Yeni anahtarlar varsayılan kapalı; istediğini aç.
- Framework özellikleri (karanlık mod, uygulama mağazası) için **System Framework** kapsamı işaretli olmalı ve yeniden başlatma gerekir.
- AGPL-3.0 lisanslı; bazı hook'lar HyperCeiler kurallarının yeniden uyarlamasıdır (bkz. NOTICE).

### Geliştiricinin kendi eklediği özellikler
<!-- Kendi eklediğin özellikleri buraya yaz. -->
