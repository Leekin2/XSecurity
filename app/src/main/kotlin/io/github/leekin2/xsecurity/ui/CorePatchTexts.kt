package io.github.leekin2.xsecurity.ui

import io.github.leekin2.xsecurity.Features as F

internal fun corePatchLines(language: AppLanguage): Map<String, Line> {
    val lines = when (language) {
        AppLanguage.TR -> listOf(
            Line("Sürüm düşürmeye izin ver", "Daha düşük sürüm kodlu APK kurulabilir; uygulama verileri eski sürümle uyumsuz olabilir"),
            Line("Özet doğrulamayı devre dışı bırak", "APK/kurulum bütünlük denetimlerini gevşetir. ⚠ Değiştirilmiş APK'lara karşı korumayı azaltır"),
            Line("resources.arsc hizalama kısıtlamasını atla", "Sıkıştırılmış veya 4 bayt sınırına hizalanmamış kaynak tablosu için kurulum kısıtlamasını kaldırır"),
            Line("İmzaları karşılaştırmayı devre dışı bırak", "Farklı imzalı güncellemeler için kurulumdaki imza/anahtar uyumluluğunu esnetir. ⚠ Paket taklidine karşı koruma azalır"),
            Line("Tam imza eşleşmesini atla", "Paket kurulumu sırasında tam imza eşleşmesi aranmaz; bölünmüş APK'lar için"),
            Line("Yüklü imzaları kullan", "Aynı paketin mevcut imza tabanlı izin uyumluluğunu korur; imza karşılaştırma seçeneği de gerekir. ⚠ İleri düzey kurulum davranışı"),
            Line("Sistem uygulamaları için gizli API'lere izin ver", "Platform imzası taşımayan sistem/güncellenmiş sistem uygulamalarının gizli API erişimini açar"),
            Line("Paylaşılan kullanıcı doğrulamasını atla", "Farklı imzalı shared UID paketlerine izin verir; imza karşılaştırma seçeneği de gerekir. ⚠ Açılış/izin sorunları oluşturabilir"),
            Line("Paket Doğrulama Ajanını Devre Dışı Bırak", "Kurulumun paket doğrulama ajanını atlar (ör. Play Protect). ⚠ Zararlı APK denetimi azalır"),
            Line("Engeli atla", "Nothing OS gibi desteklenen üretici engelleme listelerini atlar; HyperOS'ta bu sınıf bulunmayabilir"),
            Line("Kalıcı sistem uygulamalarını güncellemeye izin ver", "SystemUI gibi kalıcı uygulamaların güncelleme engelini kurulum sırasında kaldırır. System Framework kapsamı ve yeniden başlatma gerekir. ⚠ İleri düzey: uyumsuz APK, SystemUI çökmesine veya telefonun açılmamasına yol açabilir. Yalnızca cihazını kurtarmayı bilen kullanıcılar etkinleştirmeli"),
            Line("SystemUI güncellemelerinde mevcut UID’yi koru", "Farklı sharedUserId bildiren SystemUI güncellemelerinde telefonda kayıtlı kimliği korur. System Framework ve yeniden başlatma gerekir. Bu güncellemeler kullanılırken XSecurity ve bu seçenek etkin kalmalıdır; Android/ROM uyumluluğunu sağlamaz. ⚠ İleri düzey: uyumsuz APK veya korumanın kapatılması SystemUI çökmesine ya da telefonun açılmamasına yol açabilir. Yalnızca cihazını kurtarmayı bilen kullanıcılar etkinleştirmeli"),
        )
        AppLanguage.EN -> listOf(
            Line("Allow downgrades", "Allows APKs with lower version codes; older versions may be incompatible with existing app data"),
            Line("Disable digest verification", "Relaxes APK/install integrity checks. ⚠ Reduces protection against modified APKs"),
            Line("Bypass resources.arsc alignment", "Allows compressed or non-4-byte-aligned resource tables during installation"),
            Line("Disable signature comparison", "Relaxes signer/key compatibility checks for differently signed updates. ⚠ Reduces protection against package impersonation"),
            Line("Bypass exact signature matching", "Skips exact signer matching in the install flow, including split APK checks"),
            Line("Use installed signatures", "Preserves an existing same-name package's signature-permission compatibility; also requires signature comparison bypass. ⚠ Advanced install behaviour"),
            Line("Allow hidden APIs for system apps", "Enables hidden API access for system/updated-system apps without the platform signature"),
            Line("Bypass shared-user verification", "Allows different-signature shared-UID packages; also requires signer compatibility bypass. ⚠ May cause boot or permission problems"),
            Line("Disable package verification agent", "Skips the installation verification agent (such as Play Protect). ⚠ Reduces malicious-APK checking"),
            Line("Bypass vendor blocklist", "Bypasses supported vendor blocklists such as Nothing OS; this class may not exist on HyperOS"),
            Line("Allow persistent system app updates", "Removes the update restriction for persistent apps such as SystemUI during installation. Requires System Framework scope and reboot. ⚠ Advanced users only: an incompatible APK may crash SystemUI or prevent the phone from booting. Enable only if you know how to recover your device"),
            Line("Keep installed UID for SystemUI updates", "Retains the phone’s existing identity when a SystemUI update declares a different sharedUserId. Requires System Framework and reboot. XSecurity and this option must remain enabled with these updates; this does not provide Android/ROM compatibility. ⚠ Advanced users only: an incompatible APK or disabling this protection may crash SystemUI or prevent booting. Enable only if you know how to recover your device"),
        )
        AppLanguage.RU -> listOf(
            Line("Разрешить понижение версии", "Разрешает APK с меньшим кодом версии; старое приложение может не понимать текущие данные"),
            Line("Отключить проверку хэшей", "Ослабляет проверки целостности APK при установке. ⚠ Снижает защиту от изменённых APK"),
            Line("Обойти выравнивание resources.arsc", "Разрешает сжатую таблицу ресурсов или выравнивание не по 4 байтам"),
            Line("Отключить сравнение подписей", "Ослабляет совместимость подписей/ключей при обновлении. ⚠ Снижает защиту от подмены пакета"),
            Line("Обойти точное совпадение подписей", "Пропускает точное сравнение подписей при установке, включая split APK"),
            Line("Использовать установленные подписи", "Сохраняет совместимость разрешений одноимённого пакета; нужен обход сравнения подписей. ⚠ Расширенное поведение установки"),
            Line("Скрытые API для системных приложений", "Открывает скрытые API системным/обновлённым системным приложениям без подписи платформы"),
            Line("Обойти проверку общего UID", "Разрешает разные подписи для общего UID; нужен обход сравнения подписей. ⚠ Возможны ошибки загрузки и разрешений"),
            Line("Отключить агент проверки пакетов", "Пропускает агент проверки установки, например Play Protect. ⚠ Снижает проверку вредоносных APK"),
            Line("Обойти блокировку производителя", "Обходит поддерживаемые списки блокировки, например Nothing OS; на HyperOS класса может не быть"),
            Line("Разрешить обновление постоянных системных приложений", "Снимает запрет обновления постоянных приложений, например SystemUI, во время установки. Нужны System Framework и перезагрузка. ⚠ Для опытных пользователей: несовместимый APK может вызвать сбой SystemUI или помешать загрузке телефона. Включайте, только если умеете восстановить устройство"),
            Line("Сохранять UID при обновлении SystemUI", "Сохраняет установленную идентичность SystemUI при другом sharedUserId в обновлении. Нужны System Framework и перезагрузка. XSecurity и эта опция должны оставаться включёнными; совместимость Android/ROM не обеспечивается. ⚠ Для опытных пользователей: несовместимый APK или отключение защиты могут вызвать сбой SystemUI или загрузки. Включайте, только если умеете восстановить устройство"),
        )
        AppLanguage.ES -> listOf(
            Line("Permitir versiones anteriores", "Permite APK con código de versión inferior; los datos actuales pueden ser incompatibles"),
            Line("Desactivar verificación de resúmenes", "Relaja la integridad del APK durante la instalación. ⚠ Reduce la protección frente a APK modificados"),
            Line("Omitir alineación de resources.arsc", "Permite tablas de recursos comprimidas o sin alineación de 4 bytes"),
            Line("Desactivar comparación de firmas", "Relaja la compatibilidad de firmas/claves al actualizar. ⚠ Reduce la protección contra suplantación de paquetes"),
            Line("Omitir coincidencia exacta de firmas", "Omite la coincidencia exacta al instalar, incluidas comprobaciones de APK divididos"),
            Line("Usar firmas instaladas", "Conserva la compatibilidad de permisos del paquete con el mismo nombre; requiere omitir la comparación de firmas. ⚠ Comportamiento avanzado"),
            Line("Permitir API ocultas en apps del sistema", "Permite API ocultas a apps del sistema o sus actualizaciones sin firma de plataforma"),
            Line("Omitir verificación de UID compartido", "Permite firmas distintas con UID compartido; requiere omitir compatibilidad de firmas. ⚠ Puede causar fallos de arranque o permisos"),
            Line("Desactivar agente de verificación", "Omite el agente de instalación, como Play Protect. ⚠ Reduce el análisis de APK maliciosos"),
            Line("Omitir bloqueo del fabricante", "Omite listas compatibles como las de Nothing OS; la clase puede no existir en HyperOS"),
            Line("Permitir actualizar apps persistentes del sistema", "Elimina la restricción de actualización de apps persistentes como SystemUI durante la instalación. Requiere System Framework y reinicio. ⚠ Solo usuarios avanzados: un APK incompatible puede bloquear SystemUI o impedir el arranque. Actívalo solo si sabes recuperar tu dispositivo"),
            Line("Conservar UID al actualizar SystemUI", "Conserva la identidad instalada si una actualización de SystemUI declara otro sharedUserId. Requiere System Framework y reinicio. XSecurity y esta opción deben seguir activos; no proporciona compatibilidad con Android/ROM. ⚠ Solo usuarios avanzados: un APK incompatible o desactivar esta protección puede bloquear SystemUI o impedir el arranque. Actívalo solo si sabes recuperar tu dispositivo"),
        )
        AppLanguage.ZH -> listOf(
            Line("允许降级", "允许安装较低版本号 APK；旧版本可能不兼容现有应用数据"),
            Line("停用摘要验证", "放宽 APK 安装完整性检查。⚠ 降低对修改过的 APK 的保护"),
            Line("绕过 resources.arsc 对齐限制", "允许安装资源表被压缩或未按 4 字节对齐的 APK"),
            Line("停用签名比较", "放宽不同签名更新的签名及密钥兼容性检查。⚠ 降低对包名冒用的保护"),
            Line("绕过完整签名匹配", "在安装流程中跳过精确签名匹配，包括拆分 APK 检查"),
            Line("使用已安装的签名", "保留同包名已安装应用的签名权限兼容性；需同时开启签名比较绕过。⚠ 高级安装行为"),
            Line("允许系统应用使用隐藏 API", "允许无平台签名的系统应用及系统应用更新访问隐藏 API"),
            Line("绕过共享用户验证", "允许不同签名的共享 UID 应用；需同时开启签名兼容性绕过。⚠ 可能导致启动或权限问题"),
            Line("停用软件包验证代理", "跳过安装验证代理（例如 Play Protect）。⚠ 减少恶意 APK 检查"),
            Line("绕过厂商拦截", "绕过受支持厂商（如 Nothing OS）的拦截名单；HyperOS 可能不存在相应类"),
            Line("允许更新常驻系统应用", "仅在安装过程中解除 SystemUI 等常驻应用的更新限制。需要勾选 System Framework 并重启手机。⚠ 仅限高级用户：不兼容的 APK 可能导致 SystemUI 崩溃或手机无法启动。请仅在了解如何恢复设备时启用"),
            Line("SystemUI 更新时保留现有 UID", "SystemUI 更新声明不同 sharedUserId 时保留手机已安装的身份。需要系统框架作用域和重启；使用这些更新时须保持 XSecurity 和此选项启用，不提供跨 Android/ROM 兼容性。⚠ 仅限高级用户：不兼容的 APK 或关闭此保护可能导致 SystemUI 崩溃或手机无法启动。请仅在了解如何恢复设备时启用"),
        )
    }
    return F.CORE_PATCH.zip(lines).toMap()
}

fun frameworkRestartHint(language: AppLanguage) = when (language) {
    AppLanguage.TR -> "System Framework kapsamını seçip telefonu yeniden başlatın"
    AppLanguage.EN -> "Select System Framework scope and reboot the phone"
    AppLanguage.RU -> "Выберите System Framework и перезагрузите телефон"
    AppLanguage.ES -> "Selecciona el ámbito System Framework y reinicia el teléfono"
    AppLanguage.ZH -> "请勾选 System Framework 作用域并重启手机"
}
