// HyperOS-style components from compose-miuix-ui/miuix (Apache-2.0).
package io.github.leekin2.xsecurity.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun isDark(look: Look): Boolean = when (look.theme) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun XTheme(look: Look, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isDark(look)
    LaunchedEffect(dark) {
        val activity = context as? Activity ?: return@LaunchedEffect
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    if (look.hyper) {
        val mode = if (look.monet) { if (dark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight }
            else { if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light }
        val controller = remember(mode) { ThemeController(mode) }
        MiuixTheme(controller = controller, content = content)
    } else {
        MaterialTheme(colorScheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context), content = content)
    }
}
