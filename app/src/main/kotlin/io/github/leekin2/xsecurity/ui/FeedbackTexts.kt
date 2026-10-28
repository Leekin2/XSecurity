package io.github.leekin2.xsecurity.ui

import io.github.leekin2.xsecurity.Features as F

internal fun feedbackLines(language: AppLanguage): Map<String, Line> = when (language) {
    AppLanguage.TR -> mapOf(
        F.APP_INFO to Line("Uygulama bilgisi işlemleri", "Paket adı, ek ayrıntılar, Play Store, Devre dışı ve APK paylaşma. Üst düğmeler ve simgeye dokunarak açma aşağıdan ayrı yönetilir"),
        F.APP_INFO_BUTTONS to Line("Uygulama bilgisi düğmeleri", "Aç / Yol / Varsayılan aç düğmelerini gösterir. Uygulama bilgisi işlemleri de açık olmalı. Kapatıldığında özgün Varsayılan aç satırı korunur. Güvenlik uygulamasını yeniden başlatın"),
        F.APP_ICON_LAUNCH to Line("Uygulama simgesine dokunarak aç", "Uygulama bilgisi ekranındaki uygulama simgesine dokununca uygulamayı açar. Üst düğmelerden bağımsızdır. Güvenlik uygulamasını yeniden başlatın"),
        F.MMS_ADS to Line("Mesajlardaki reklamları kaldır", "Desteklenen Xiaomi Mesajlar sürümlerindeki tanıtım menülerini ve düğmelerini gizler. Mesajlar kapsamı ve uygulamanın yeniden başlatılması gerekir"),
        F.MMS_FRAUD to Line("Dolandırıcılık uyarısını gizle", "Mesajlardaki dolandırıcılık risk etiketini gizler. ⚠ Şüpheli gönderen uyarılarını göremeyebilirsiniz. Varsayılan kapalı"),
        F.MMS_OVERSEAS to Line("Yurt dışı risk uyarısını gizle", "Yurt dışı numaraları için risk etiketini gizler; dolandırıcılık seçeneğinden bağımsızdır. ⚠ Bu risk uyarısını göremezsiniz. Varsayılan kapalı"),
        F.MMS_OLD_DEVICE to Line("Yeni Mesajlar arayüzünü kullan", "Desteklenen Xiaomi Mesajlar sürümlerinde eski cihaz arayüzü seçimini kapatır. Deneyseldir; hesap veya cihaz kimliğini değiştirmez"),
        F.POWER_SAVE_BOOT to Line("Pil tasarrufunu açılışta koru", "Pil tasarrufundaki Şarj olduğunda kapat seçeneği kapalıysa, Güvenlik’in açılıştaki ek kapatma denetimini atlar. Tasarrufu kendiliğinden açmaz; elle kapatma ve zamanlama korunur. Güvenlik kapsamı ve yeniden başlatma gerekir"),
        F.MMS_COPY_CODE to Line("Doğrulama kodunu otomatik kopyala", "Xiaomi Mesajlar bildiriminin sunduğu 4–8 haneli sayısal kodu panoya kopyalar; tüm mesajı kopyalamaz. Pano hassas olarak işaretlenir. Kod günlüğe veya dosyaya yazılmaz"),
        F.HOME_PRESTART to Line("Uygulama ön yüklemesini kapat", "Desteklenen Xiaomi başlatıcılarında uygulama ön başlatma kontrollerini kapatır. Sistem özelliklerini değiştirmez; uygulamaların açılması yavaşlayabilir. Başlatıcı kapsamı ve yeniden başlatma gerekir"),
        F.HOME_REPORT to Line("Kaldırma penceresinde Bildir yazısını gizle", "Desteklenen Xiaomi başlatıcılarında uygulama kaldırma penceresindeki rapor seçeneğini gizler. Uygulamanın gerçek yükleme kaynağı değişmez. Başlatıcıyı yeniden başlatın"),
    )
    AppLanguage.EN -> mapOf(
        F.APP_INFO to Line("App info actions", "Package name, additional details, Play Store, Disable and Share APK. Header buttons and tapping the icon to launch are controlled separately below"),
        F.APP_INFO_BUTTONS to Line("App info buttons", "Show Open / Path / Open by default buttons. Also requires App info actions. Turning this off keeps the stock Open by default row. Restart Security after changing"),
        F.APP_ICON_LAUNCH to Line("Tap app icon to launch", "Tap the app icon on App info to launch the app, independently of the header buttons. Restart Security after changing"),
        F.MMS_ADS to Line("Remove message ads", "Hide promotional menus and buttons in supported Xiaomi Messages builds. Requires Messages scope and restarting the app"),
        F.MMS_FRAUD to Line("Hide fraud warning", "Hide the fraud-risk label in messages. ⚠ You may miss warnings about suspicious senders. Off by default"),
        F.MMS_OVERSEAS to Line("Hide overseas risk warning", "Hide the overseas-number risk label independently of the fraud option. ⚠ This warning will not be shown. Off by default"),
        F.MMS_OLD_DEVICE to Line("Use newer Messages interface", "Disable the legacy-device interface choice in supported Xiaomi Messages builds. Experimental; does not change account or device identity"),
        F.POWER_SAVE_BOOT to Line("Keep Battery Saver at boot", "Skip Security’s extra boot-time close check when Turn off when charged is disabled in Battery Saver. Does not turn Saver on by itself; manual changes and schedules remain. Requires Security scope and reboot"),
        F.MMS_COPY_CODE to Line("Auto-copy verification code", "Copy the 4–8 digit numeric code supplied by a Xiaomi Messages notification, never the entire message. Clipboard content is marked sensitive; codes are never logged or saved to files"),
        F.HOME_PRESTART to Line("Disable app preloading", "Disable app prestart checks in supported Xiaomi launchers without changing system properties. Apps may open more slowly. Requires Launcher scope and restarting it"),
        F.HOME_REPORT to Line("Hide Report in uninstall dialog", "Hide the report option in supported Xiaomi launcher uninstall dialogs. The real app install source is unchanged. Restart Launcher"),
    )
    AppLanguage.RU -> mapOf(
        F.APP_INFO to Line("Действия в «О приложении»", "Имя пакета, сведения, Play Маркет, отключение и отправка APK. Кнопки и запуск касанием значка настраиваются отдельно ниже"),
        F.APP_INFO_BUTTONS to Line("Кнопки в «О приложении»", "Показывать Открыть / Путь / По умолчанию. Требуются действия в «О приложении». При отключении сохраняется штатная строка. Перезапустите Безопасность"),
        F.APP_ICON_LAUNCH to Line("Запуск касанием значка", "Касание значка в «О приложении» запускает приложение независимо от верхних кнопок. Перезапустите Безопасность"),
        F.MMS_ADS to Line("Убрать рекламу в сообщениях", "Скрывает рекламные меню и кнопки в поддерживаемых версиях Xiaomi Сообщения. Нужны область Сообщения и перезапуск"),
        F.MMS_FRAUD to Line("Скрыть предупреждение о мошенничестве", "Скрывает метку риска мошенничества. ⚠ Можно пропустить предупреждения о подозрительных отправителях. По умолчанию выключено"),
        F.MMS_OVERSEAS to Line("Скрыть риск зарубежных номеров", "Скрывает метку зарубежного номера независимо от мошенничества. ⚠ Это предупреждение не показывается. По умолчанию выключено"),
        F.MMS_OLD_DEVICE to Line("Новый интерфейс Сообщений", "Отключает выбор интерфейса для старых устройств в поддерживаемых версиях Xiaomi Сообщения. Экспериментально; аккаунт и идентификатор устройства не меняются"),
        F.POWER_SAVE_BOOT to Line("Сохранять энергосбережение при запуске", "Пропускает дополнительную проверку отключения при загрузке, если выключено отключение при зарядке. Не включает режим самостоятельно; ручное управление и расписание сохраняются. Нужны область Безопасности и перезагрузка"),
        F.MMS_COPY_CODE to Line("Автокопирование кода подтверждения", "Копирует только цифровой код из 4–8 цифр, предоставленный уведомлением Xiaomi Сообщения. Буфер отмечен как конфиденциальный; коды не записываются в журнал или файлы"),
        F.HOME_PRESTART to Line("Отключить предзапуск приложений", "Отключает проверки предзапуска в поддерживаемых лаунчерах Xiaomi без изменения системных свойств. Запуск может замедлиться. Нужны область лаунчера и перезапуск"),
        F.HOME_REPORT to Line("Скрыть «Сообщить» при удалении", "Скрывает пункт жалобы в поддерживаемом диалоге удаления лаунчера Xiaomi. Реальный источник установки не меняется. Перезапустите лаунчер"),
    )
    AppLanguage.ES -> mapOf(
        F.APP_INFO to Line("Acciones en Info de la app", "Paquete, detalles, Play Store, Desactivar y Compartir APK. Los botones y el inicio al tocar el icono se controlan por separado abajo"),
        F.APP_INFO_BUTTONS to Line("Botones en Info de la app", "Muestra Abrir / Ruta / Abrir por defecto. Requiere Acciones en Info de la app. Al apagarlo se conserva la fila original. Reinicia Seguridad"),
        F.APP_ICON_LAUNCH to Line("Tocar el icono para abrir", "Toca el icono en Info de la app para abrirla, independientemente de los botones superiores. Reinicia Seguridad"),
        F.MMS_ADS to Line("Quitar anuncios de mensajes", "Oculta menús y botones promocionales en versiones compatibles de Mensajes Xiaomi. Requiere su ámbito y reiniciar la app"),
        F.MMS_FRAUD to Line("Ocultar aviso de fraude", "Oculta la etiqueta de riesgo de fraude. ⚠ Puedes perder avisos de remitentes sospechosos. Desactivado por defecto"),
        F.MMS_OVERSEAS to Line("Ocultar riesgo de números extranjeros", "Oculta la etiqueta de números extranjeros independientemente de fraude. ⚠ No se mostrará este aviso. Desactivado por defecto"),
        F.MMS_OLD_DEVICE to Line("Usar interfaz nueva de Mensajes", "Desactiva la selección de interfaz antigua en versiones compatibles de Mensajes Xiaomi. Experimental; no cambia la cuenta ni la identidad del dispositivo"),
        F.POWER_SAVE_BOOT to Line("Conservar ahorro al arrancar", "Omite la comprobación adicional al arrancar si Desactivar al cargar está apagado. No activa el ahorro por sí solo; conserva los cambios manuales y horarios. Requiere ámbito Seguridad y reiniciar"),
        F.MMS_COPY_CODE to Line("Copiar código de verificación", "Copia solo el código numérico de 4–8 dígitos de la notificación de Mensajes Xiaomi. Marca el portapapeles como sensible; no guarda códigos en registros ni archivos"),
        F.HOME_PRESTART to Line("Desactivar precarga de apps", "Desactiva las comprobaciones de preinicio en lanzadores Xiaomi compatibles sin cambiar propiedades del sistema. El inicio puede ser más lento. Requiere su ámbito y reiniciarlo"),
        F.HOME_REPORT to Line("Ocultar Denunciar al desinstalar", "Oculta la opción de denuncia en diálogos de desinstalación compatibles del lanzador Xiaomi. No cambia el origen real de instalación. Reinicia el lanzador"),
    )
    AppLanguage.ZH -> mapOf(
        F.APP_INFO to Line("应用信息操作", "包名、详细信息、Play 商店、停用与分享 APK。下方可分别控制顶部按钮及点击图标启动"),
        F.APP_INFO_BUTTONS to Line("应用信息按钮", "显示打开 / 路径 / 默认打开按钮。需同时启用应用信息操作。关闭后保留系统默认打开条目。修改后请重启手机管家"),
        F.APP_ICON_LAUNCH to Line("点击应用图标启动", "在应用信息页面点击图标启动应用，与顶部按钮无关。修改后请重启手机管家"),
        F.MMS_ADS to Line("移除短信广告", "隐藏受支持小米短信版本中的推广菜单和按钮。需要短信作用域并重启应用"),
        F.MMS_FRAUD to Line("隐藏诈骗风险提示", "隐藏短信诈骗风险标签。⚠ 可能错过可疑发件人的警告。默认关闭"),
        F.MMS_OVERSEAS to Line("隐藏境外号码风险提示", "仅隐藏境外号码风险标签，与诈骗选项独立。⚠ 将不再显示此警告。默认关闭"),
        F.MMS_OLD_DEVICE to Line("使用新版短信界面", "在受支持的小米短信版本中停用旧设备界面选择。实验性功能，不改变账号或设备身份"),
        F.POWER_SAVE_BOOT to Line("开机保留省电模式", "关闭充电时退出省电模式后，跳过手机管家的额外开机退出检查。不会自行开启省电；保留手动操作和定时规则。需要手机管家作用域并重启"),
        F.MMS_COPY_CODE to Line("自动复制验证码", "仅复制小米短信通知提供的 4–8 位数字验证码，不复制整条短信。剪贴板标记为敏感内容；不将验证码写入日志或文件"),
        F.HOME_PRESTART to Line("停用应用预启动", "在受支持的小米桌面中停用应用预启动检查，不修改系统属性。应用启动可能变慢。需要桌面作用域并重启桌面"),
        F.HOME_REPORT to Line("隐藏卸载窗口举报文字", "隐藏受支持小米桌面卸载窗口中的举报选项。不改变应用的真实安装来源。请重启桌面"),
    )
}

internal fun nativeLauncherHint(language: AppLanguage): String = when (language) {
    AppLanguage.TR -> "Bu Başlatıcı sürümü yerel kod kullanıyor. Bu iki seçenek için uyumlu yerel hook henüz yok; Java hook’ları uygulanamaz."
    AppLanguage.EN -> "This Launcher uses native code. These two options need a compatible native hook; Java hooks cannot apply."
    AppLanguage.RU -> "Эта версия лаунчера использует нативный код. Для этих двух опций нужен совместимый нативный хук; Java-хуки неприменимы."
    AppLanguage.ES -> "Este lanzador usa código nativo. Estas dos opciones necesitan un hook nativo compatible; los hooks Java no se aplican."
    AppLanguage.ZH -> "此桌面版本使用原生代码。这两个选项需要兼容的原生钩子，无法使用 Java 钩子。"
}
