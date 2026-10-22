package io.github.leekin2.xsecurity.ui

import io.github.leekin2.xsecurity.Features as F

/** Descriptions distinguish master settings from active sessions and granted permissions. */
/** Key of the "choose apps" row under the background clipboard switch. */
const val PICK_CLIPBOARD = "clipboard_pick_apps"

internal fun extensionLines(language: AppLanguage): Map<String, Line> = when (language) {
    AppLanguage.TR -> mapOf(
        F.CLIPBOARD_WHITELIST to Line("Arka planda pano okuma (beyaz liste)", "Yalnızca seçtiğiniz uygulamalar arka planda panoyu okuyabilir; diğerleri normal kuralda kalır. System Framework kapsamı ve yeniden başlatma gerekir"),
        PICK_CLIPBOARD to Line("Uygulamaları seç", "Panoyu arka planda okuyabilecek uygulamalar"),
        F.APPLIST_UPLOAD_OFF to Line("Yüklü uygulama listesi yüklenmesini engelle", "Güvenlik bileşeni (Guard Provider) yüklü uygulama listenizi Xiaomi sunucusuna otomatik göndermez"),
        F.POWER_SAVE_REFRESH to Line("Pil tasarrufunda yenileme hızını koru", "Pil tasarrufunun 60 Hz sınırını kaldırır; seçtiğiniz hız ve termal sınırlar korunur. System Framework kapsamı ve yeniden başlatma gerekir; pil tüketimi artabilir"),
        F.POWER_SAVE_BRIGHTNESS to Line("Pil tasarrufunda parlaklığı koru", "Pil tasarrufunun parlaklık azaltmasını engeller. System Framework kapsamı ve yeniden başlatma gerekir; sıcaklık koruması değişmez"),
        F.GAME_KEEP_ENABLED to Line("Oyun Turbo anahtarını açık tut", "Oyun Turbo'nun ana aç/kapat ayarı açık kalır. Oyun oturumunu sürekli açık tutmaz ve oyun içi özel ayarları kilitlemez"),
        F.NET_APP_CONTROLS to Line("Uygulama ağ kontrollerini genişlet", "Güvenlik → Veri kullanımı ekranında uygun sistem uygulamalarına Wi-Fi ve arka plan ağ kontrolleri ekler. Kurallarınızı buradan siz seçersiniz"),
    )
    AppLanguage.EN -> mapOf(
        F.CLIPBOARD_WHITELIST to Line("Background clipboard read (allow list)", "Only the apps you pick can read the clipboard in the background; all others keep the normal rule. Requires System Framework scope and reboot"),
        PICK_CLIPBOARD to Line("Choose apps", "Apps that may read the clipboard in the background"),
        F.APPLIST_UPLOAD_OFF to Line("Block installed app list upload", "The security component (Guard Provider) no longer sends your installed app list to Xiaomi automatically"),
        F.POWER_SAVE_REFRESH to Line("Keep refresh rate in Battery Saver", "Removes Battery Saver's 60 Hz cap; respects your selected rate and thermal limits. Requires System Framework scope and reboot; may use more battery"),
        F.POWER_SAVE_BRIGHTNESS to Line("Keep brightness in Battery Saver", "Prevents Battery Saver dimming. Requires System Framework scope and reboot; thermal protection is unchanged"),
        F.GAME_KEEP_ENABLED to Line("Keep Game Turbo enabled", "Keeps the master switch on. Does not force an active game session or lock individual game settings"),
        F.NET_APP_CONTROLS to Line("Extend per-app network controls", "Adds Wi-Fi and background network controls for eligible system apps in Security → Data usage. You choose the rules there"),
    )
    AppLanguage.RU -> mapOf(
        F.CLIPBOARD_WHITELIST to Line("Чтение буфера обмена в фоне (список)", "Только выбранные вами приложения могут читать буфер обмена в фоне; остальные работают по обычному правилу. Нужны System Framework и перезагрузка"),
        PICK_CLIPBOARD to Line("Выбрать приложения", "Приложения, которым разрешено читать буфер обмена в фоне"),
        F.APPLIST_UPLOAD_OFF to Line("Не отправлять список установленных приложений", "Компонент безопасности (Guard Provider) больше не отправляет список ваших приложений на сервер Xiaomi"),
        F.POWER_SAVE_REFRESH to Line("Сохранять частоту при экономии заряда", "Убирает предел 60 Гц режима экономии, сохраняя выбранную частоту и тепловые ограничения. Нужны System Framework и перезагрузка; расход может вырасти"),
        F.POWER_SAVE_BRIGHTNESS to Line("Сохранять яркость при экономии заряда", "Убирает затемнение режима экономии. Нужны System Framework и перезагрузка; тепловая защита сохраняется"),
        F.GAME_KEEP_ENABLED to Line("Не отключать Game Turbo", "Главный переключатель остаётся включённым. Игровой сеанс не удерживается постоянно; настройки отдельных игр не блокируются"),
        F.NET_APP_CONTROLS to Line("Расширить сетевые настройки приложений", "Добавляет Wi-Fi и фоновую сеть для подходящих системных приложений в Безопасность → Трафик. Правила выбираете вы"),
    )
    AppLanguage.ES -> mapOf(
        F.CLIPBOARD_WHITELIST to Line("Leer portapapeles en segundo plano (lista)", "Solo las apps que elijas pueden leer el portapapeles en segundo plano; las demás siguen la regla normal. Requiere System Framework y reinicio"),
        PICK_CLIPBOARD to Line("Elegir apps", "Apps que pueden leer el portapapeles en segundo plano"),
        F.APPLIST_UPLOAD_OFF to Line("Impedir la subida de la lista de apps", "El componente de seguridad (Guard Provider) ya no envía automáticamente tu lista de apps a Xiaomi"),
        F.POWER_SAVE_REFRESH to Line("Mantener frecuencia con ahorro de batería", "Quita el límite de 60 Hz del ahorro; respeta la frecuencia elegida y los límites térmicos. Requiere System Framework y reinicio; puede gastar más batería"),
        F.POWER_SAVE_BRIGHTNESS to Line("Mantener brillo con ahorro de batería", "Evita la reducción de brillo del ahorro. Requiere System Framework y reinicio; conserva la protección térmica"),
        F.GAME_KEEP_ENABLED to Line("Mantener Game Turbo activado", "Mantiene encendido el interruptor principal. No fuerza una sesión de juego activa ni bloquea ajustes de cada juego"),
        F.NET_APP_CONTROLS to Line("Ampliar controles de red por aplicación", "Añade Wi-Fi y red en segundo plano para apps del sistema compatibles en Seguridad → Uso de datos. Tú eliges las reglas"),
    )
    AppLanguage.ZH -> mapOf(
        F.CLIPBOARD_WHITELIST to Line("后台读取剪贴板（白名单）", "仅你选择的应用可在后台读取剪贴板，其他应用保持默认规则。需勾选 System Framework 并重启"),
        PICK_CLIPBOARD to Line("选择应用", "允许在后台读取剪贴板的应用"),
        F.APPLIST_UPLOAD_OFF to Line("阻止上传已安装应用列表", "安全组件（Guard Provider）不再自动把你的已安装应用列表发送给小米服务器"),
        F.POWER_SAVE_REFRESH to Line("省电模式保持刷新率", "移除省电模式的 60 Hz 限制，保留所选刷新率及温控限制。需勾选 System Framework 并重启；可能增加耗电"),
        F.POWER_SAVE_BRIGHTNESS to Line("省电模式保持亮度", "阻止省电模式调暗屏幕。需勾选 System Framework 并重启；不改变温控保护"),
        F.GAME_KEEP_ENABLED to Line("保持游戏加速总开关开启", "保持总开关开启，不强制持续游戏会话，不锁定各游戏的设置"),
        F.NET_APP_CONTROLS to Line("扩展应用联网控制", "在手机管家→流量使用中为符合条件的系统应用提供 Wi-Fi 和后台联网控制，由你选择规则"),
    )
}
