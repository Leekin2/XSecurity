package io.github.leekin2.xsecurity.ui

import io.github.leekin2.xsecurity.Features as F

/** Descriptions distinguish master settings from active sessions and granted permissions. */
/** Key of the "choose apps" row under the background clipboard switch. */
const val PICK_CLIPBOARD = "clipboard_pick_apps"

internal fun extensionLines(language: AppLanguage): Map<String, Line> = when (language) {
    AppLanguage.TR -> mapOf(
        F.SIM_MENUS to Line("SIM ayarları: VoLTE ve Wi-Fi araması menüleri", "SIM kartı ayarlarında Xiaomi’nin kendi “VoLTE kullan” anahtarını ve Wi-Fi araması ağ tercihini (Wi-Fi tercih edilir) gösterir; yeniden başlatınca kaybolmaz. LSPosed’de Telefon (com.android.phone) kapsamını seçip telefonu yeniden başlatın"),
        F.IMS_ICONS to Line("Global VoLTE / VoNR simgeleri", "Çin ROM’da durum çubuğunda global ROM’daki gibi orijinal Vo LTE / Vo NR simgesini gösterir. Simge yalnızca gerçek IMS kaydı varken görünür. LSPosed’de Sistem Arayüzü kapsamını seçip Sistem Arayüzünü yeniden başlatın"),
        F.CLIPBOARD_WHITELIST to Line("Arka planda pano okuma (beyaz liste)", "Yalnızca seçtiğiniz uygulamalar arka planda panoyu okuyabilir; diğerleri normal kuralda kalır. System Framework kapsamı ve yeniden başlatma gerekir"),
        PICK_CLIPBOARD to Line("Uygulamaları seç", "Panoyu arka planda okuyabilecek uygulamalar"),
        F.APPLIST_UPLOAD_OFF to Line("Yüklü uygulama listesi yüklenmesini engelle", "Güvenlik bileşeni (Guard Provider) yüklü uygulama listenizi Xiaomi sunucusuna otomatik göndermez"),
        F.POWER_SAVE_REFRESH to Line("Pil tasarrufunda yenileme hızını koru", "Pil tasarrufunun 60 Hz sınırını kaldırır; seçtiğiniz hız ve termal sınırlar korunur. System Framework kapsamı ve yeniden başlatma gerekir; pil tüketimi artabilir"),
        F.POWER_SAVE_BRIGHTNESS to Line("Pil tasarrufunda parlaklığı koru", "Pil tasarrufunun parlaklık azaltmasını engeller. System Framework kapsamı ve yeniden başlatma gerekir; sıcaklık koruması değişmez"),
        F.GAME_KEEP_ENABLED to Line("Oyun Turbo anahtarını açık tut", "Oyun Turbo'nun ana aç/kapat ayarı açık kalır. Oyun oturumunu sürekli açık tutmaz ve oyun içi özel ayarları kilitlemez"),
        F.NET_APP_CONTROLS to Line("Uygulama ağ kontrollerini genişlet", "Güvenlik → Veri kullanımı ekranında uygun sistem uygulamalarına Wi-Fi ve arka plan ağ kontrolleri ekler. Kurallarınızı buradan siz seçersiniz"),
    )
    AppLanguage.EN -> mapOf(
        F.SIM_MENUS to Line("SIM settings: VoLTE and Wi-Fi calling menus", "Shows Xiaomi’s own “Use VoLTE” switch and the Wi-Fi calling network preference on the SIM card settings page, and keeps them after a restart. Select Phone (com.android.phone) scope in LSPosed and reboot"),
        F.IMS_ICONS to Line("Global VoLTE / VoNR icons", "Shows the global ROM’s original Vo LTE / Vo NR status bar icon on a China ROM. It appears only while IMS is really registered. Select System UI scope in LSPosed and restart System UI"),
        F.CLIPBOARD_WHITELIST to Line("Background clipboard read (allow list)", "Only the apps you pick can read the clipboard in the background; all others keep the normal rule. Requires System Framework scope and reboot"),
        PICK_CLIPBOARD to Line("Choose apps", "Apps that may read the clipboard in the background"),
        F.APPLIST_UPLOAD_OFF to Line("Block installed app list upload", "The security component (Guard Provider) no longer sends your installed app list to Xiaomi automatically"),
        F.POWER_SAVE_REFRESH to Line("Keep refresh rate in Battery Saver", "Removes Battery Saver's 60 Hz cap; respects your selected rate and thermal limits. Requires System Framework scope and reboot; may use more battery"),
        F.POWER_SAVE_BRIGHTNESS to Line("Keep brightness in Battery Saver", "Prevents Battery Saver dimming. Requires System Framework scope and reboot; thermal protection is unchanged"),
        F.GAME_KEEP_ENABLED to Line("Keep Game Turbo enabled", "Keeps the master switch on. Does not force an active game session or lock individual game settings"),
        F.NET_APP_CONTROLS to Line("Extend per-app network controls", "Adds Wi-Fi and background network controls for eligible system apps in Security → Data usage. You choose the rules there"),
    )
    AppLanguage.RU -> mapOf(
        F.SIM_MENUS to Line("Настройки SIM: меню VoLTE и Wi-Fi звонков", "Показывает в настройках SIM собственный переключатель Xiaomi «Использовать VoLTE» и выбор сети для Wi-Fi звонков; сохраняются после перезагрузки. Выберите Телефон (com.android.phone) в LSPosed и перезагрузите"),
        F.IMS_ICONS to Line("Значки VoLTE / VoNR как в Global", "Показывает в строке состояния оригинальный значок Vo LTE / Vo NR глобальной прошивки на китайской. Значок виден только при реальной регистрации IMS. Выберите System UI в LSPosed и перезапустите System UI"),
        F.CLIPBOARD_WHITELIST to Line("Чтение буфера обмена в фоне (список)", "Только выбранные вами приложения могут читать буфер обмена в фоне; остальные работают по обычному правилу. Нужны System Framework и перезагрузка"),
        PICK_CLIPBOARD to Line("Выбрать приложения", "Приложения, которым разрешено читать буфер обмена в фоне"),
        F.APPLIST_UPLOAD_OFF to Line("Не отправлять список установленных приложений", "Компонент безопасности (Guard Provider) больше не отправляет список ваших приложений на сервер Xiaomi"),
        F.POWER_SAVE_REFRESH to Line("Сохранять частоту при экономии заряда", "Убирает предел 60 Гц режима экономии, сохраняя выбранную частоту и тепловые ограничения. Нужны System Framework и перезагрузка; расход может вырасти"),
        F.POWER_SAVE_BRIGHTNESS to Line("Сохранять яркость при экономии заряда", "Убирает затемнение режима экономии. Нужны System Framework и перезагрузка; тепловая защита сохраняется"),
        F.GAME_KEEP_ENABLED to Line("Не отключать Game Turbo", "Главный переключатель остаётся включённым. Игровой сеанс не удерживается постоянно; настройки отдельных игр не блокируются"),
        F.NET_APP_CONTROLS to Line("Расширить сетевые настройки приложений", "Добавляет Wi-Fi и фоновую сеть для подходящих системных приложений в Безопасность → Трафик. Правила выбираете вы"),
    )
    AppLanguage.ES -> mapOf(
        F.SIM_MENUS to Line("Ajustes SIM: menús de VoLTE y llamadas Wi-Fi", "Muestra en los ajustes de la SIM el interruptor «Usar VoLTE» de Xiaomi y la preferencia de red de llamadas Wi-Fi, y se mantienen tras reiniciar. Selecciona Teléfono (com.android.phone) en LSPosed y reinicia"),
        F.IMS_ICONS to Line("Iconos VoLTE / VoNR globales", "Muestra en una ROM china el icono original Vo LTE / Vo NR de la ROM global. Solo aparece con un registro IMS real. Selecciona System UI en LSPosed y reinicia System UI"),
        F.CLIPBOARD_WHITELIST to Line("Leer portapapeles en segundo plano (lista)", "Solo las apps que elijas pueden leer el portapapeles en segundo plano; las demás siguen la regla normal. Requiere System Framework y reinicio"),
        PICK_CLIPBOARD to Line("Elegir apps", "Apps que pueden leer el portapapeles en segundo plano"),
        F.APPLIST_UPLOAD_OFF to Line("Impedir la subida de la lista de apps", "El componente de seguridad (Guard Provider) ya no envía automáticamente tu lista de apps a Xiaomi"),
        F.POWER_SAVE_REFRESH to Line("Mantener frecuencia con ahorro de batería", "Quita el límite de 60 Hz del ahorro; respeta la frecuencia elegida y los límites térmicos. Requiere System Framework y reinicio; puede gastar más batería"),
        F.POWER_SAVE_BRIGHTNESS to Line("Mantener brillo con ahorro de batería", "Evita la reducción de brillo del ahorro. Requiere System Framework y reinicio; conserva la protección térmica"),
        F.GAME_KEEP_ENABLED to Line("Mantener Game Turbo activado", "Mantiene encendido el interruptor principal. No fuerza una sesión de juego activa ni bloquea ajustes de cada juego"),
        F.NET_APP_CONTROLS to Line("Ampliar controles de red por aplicación", "Añade Wi-Fi y red en segundo plano para apps del sistema compatibles en Seguridad → Uso de datos. Tú eliges las reglas"),
    )
    AppLanguage.ZH -> mapOf(
        F.SIM_MENUS to Line("SIM 卡设置：VoLTE 与 WLAN 通话菜单", "在 SIM 卡设置中显示小米原生的“使用 VoLTE”开关和 WLAN 通话网络偏好，重启后保留。需在 LSPosed 勾选电话（com.android.phone）并重启"),
        F.IMS_ICONS to Line("国际版 VoLTE / VoNR 图标", "在国行系统的状态栏显示国际版原生 Vo LTE / Vo NR 图标，仅在 IMS 实际注册时显示。需在 LSPosed 勾选系统界面并重启系统界面"),
        F.CLIPBOARD_WHITELIST to Line("后台读取剪贴板（白名单）", "仅你选择的应用可在后台读取剪贴板，其他应用保持默认规则。需勾选 System Framework 并重启"),
        PICK_CLIPBOARD to Line("选择应用", "允许在后台读取剪贴板的应用"),
        F.APPLIST_UPLOAD_OFF to Line("阻止上传已安装应用列表", "安全组件（Guard Provider）不再自动把你的已安装应用列表发送给小米服务器"),
        F.POWER_SAVE_REFRESH to Line("省电模式保持刷新率", "移除省电模式的 60 Hz 限制，保留所选刷新率及温控限制。需勾选 System Framework 并重启；可能增加耗电"),
        F.POWER_SAVE_BRIGHTNESS to Line("省电模式保持亮度", "阻止省电模式调暗屏幕。需勾选 System Framework 并重启；不改变温控保护"),
        F.GAME_KEEP_ENABLED to Line("保持游戏加速总开关开启", "保持总开关开启，不强制持续游戏会话，不锁定各游戏的设置"),
        F.NET_APP_CONTROLS to Line("扩展应用联网控制", "在手机管家→流量使用中为符合条件的系统应用提供 Wi-Fi 和后台联网控制，由你选择规则"),
    )
}
