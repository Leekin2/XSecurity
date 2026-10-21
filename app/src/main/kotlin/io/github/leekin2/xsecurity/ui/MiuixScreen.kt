// HyperOS-style components from compose-miuix-ui/miuix (Apache-2.0).
package io.github.leekin2.xsecurity.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.leekin2.xsecurity.BuildConfig
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun MiuixScreen(state: UiState, actions: Actions) {
    val t = textsFor(state.language)
    val bar = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val surface = MiuixTheme.colorScheme.surface
    val blur = state.look.blur
    BackHandler(state.settingsOpen) { actions.onSettings(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = if (state.settingsOpen) t.tabSettings else "XSecurity",
            subtitle = if (state.settingsOpen) "XSecurity" else t.subtitle,
            scrollBehavior = bar,
            color = if (blur) Color.Transparent else surface,
            modifier = if (blur) Modifier.textureBlur(backdrop, RectangleShape, 25f,
                colors = BlurColors(blendColors = listOf(BlendColorEntry(surface.copy(alpha = 0.8f))))) else Modifier,
            navigationIcon = {
                if (state.settingsOpen) IconButton(onClick = { actions.onSettings(false) }, modifier = Modifier.padding(start = 12.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, t.tabHome)
                }
            },
            actions = {
                if (!state.settingsOpen) IconButton(onClick = { actions.onSettings(true) }, modifier = Modifier.padding(end = 12.dp)) {
                    Icon(Icons.Rounded.Tune, t.tabSettings)
                }
            }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxHeight()
                .then(if (blur) Modifier.layerBackdrop(backdrop).background(surface) else Modifier)
                .overScrollVertical().nestedScroll(bar.nestedScrollConnection),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp),
            overscrollEffect = null
        ) {
            if (state.settingsOpen) {
                item { SettingsPage(state, t, actions) }
            } else {
                item { StatusHeader(state, t) }
                items(SECTIONS, key = { it.id }) { section -> SectionCard(section, state, t, actions) }
                item { RestartButton(state, t, actions) }
            }
        }
    }
}

/** Shield badge, state and how many switches are on. */
@Composable
private fun StatusHeader(state: UiState, t: Texts) {
    val colors = MiuixTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val (accent, icon, title) = when (state.connected) {
        null -> Triple(colors.primary, Icons.Rounded.HourglassTop, t.connecting)
        false -> Triple(if (dark) Color(0xFFFF6B6B) else Color(0xFFD32F2F), Icons.Rounded.HighlightOff, t.inactive)
        true -> Triple(if (dark) Color(0xFF4ADE80) else Color(0xFF1B8A3C), Icons.Rounded.CheckCircle, t.active)
    }
    val total = SECTIONS.sumOf { it.items.size }
    val on = SECTIONS.sumOf { s -> s.items.count { state.values[it.key] == true } }
    Card(Modifier.padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 8.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Shield, null, Modifier.size(30.dp), tint = accent)
                Icon(icon, null, Modifier.size(18.dp).align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp), tint = accent)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = accent)
                Text(if (state.connected == true) t.activeSummary else if (state.connected == false) t.inactiveSummary else t.version(BuildConfig.VERSION_NAME),
                    fontSize = 13.sp, color = colors.onSurfaceVariantSummary, modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t.onCount(on, total), accent)
                    Chip(t.version(BuildConfig.VERSION_NAME), colors.onSurfaceVariantSummary)
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp))
}

/** A folding card: colored badge, title, "n / m on" and an arrow; the switches open under it. */
@Composable
private fun SectionCard(section: Section, state: UiState, t: Texts, actions: Actions) {
    var open by rememberSaveable(section.id) { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "section")
    val colors = MiuixTheme.colorScheme
    val on = section.items.count { state.values[it.key] == true }
    Card(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(section.color), contentAlignment = Alignment.Center) {
                Icon(section.icon, null, Modifier.size(24.dp), tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t.sections[section.id] ?: section.id, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(t.onCount(on, section.items.size), fontSize = 13.sp, color = colors.onSurfaceVariantSummary)
            }
            Icon(Icons.Rounded.ExpandMore, null, Modifier.rotate(turn), tint = colors.onSurfaceVariantActions)
        }
        AnimatedVisibility(open, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
            Column(Modifier.padding(bottom = 6.dp)) {
                section.items.forEach { item ->
                    val line = t.features[item.key]
                    SwitchPreference(
                        checked = state.values[item.key] == true,
                        onCheckedChange = { actions.onChange(item.key, it) },
                        title = line?.title ?: item.key, summary = line?.summary,
                        startAction = { RowIcon(item.icon, section.color) },
                        enabled = state.connected != false
                    )
                }
            }
        }
    }
}

@Composable
private fun RestartButton(state: UiState, t: Texts, actions: Actions) {
    Column(Modifier.padding(horizontal = 12.dp).padding(top = 14.dp)) {
        TextButton(text = t.restart, onClick = actions.onRestart, modifier = Modifier.fillMaxWidth(),
            enabled = state.connected == true && !state.restarting, colors = ButtonDefaults.textButtonColorsPrimary())
        Text(t.restartSummary, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
private fun SettingsPage(state: UiState, t: Texts, actions: Actions) {
    SmallTitle(t.groupLook, Modifier.padding(top = 6.dp))
    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 6.dp)) {
        PopupRow(Icons.Rounded.Language, t.language, state.language.label, AppLanguage.entries.map { it.label },
            AppLanguage.entries.indexOf(state.language)) { actions.onLanguage(AppLanguage.entries[it]) }
        val themes = listOf(t.themeSystem, t.themeLight, t.themeDark)
        val themeIcon = when (state.look.theme) { 1 -> Icons.Rounded.LightMode; 2 -> Icons.Rounded.DarkMode; else -> Icons.Rounded.BrightnessAuto }
        PopupRow(themeIcon, t.theme, themes[state.look.theme], themes, state.look.theme) { actions.onLook(state.look.copy(theme = it)) }
        SwitchPreference(checked = state.look.hyper, onCheckedChange = { actions.onLook(state.look.copy(hyper = it)) },
            title = t.hyperStyle, summary = t.hyperStyleSummary, startAction = { RowIcon(Icons.Rounded.Palette, null) })
        SwitchPreference(checked = state.look.monet, onCheckedChange = { actions.onLook(state.look.copy(monet = it)) },
            title = t.monet, summary = t.monetSummary, startAction = { RowIcon(Icons.Rounded.Wallpaper, null) })
        SwitchPreference(checked = state.look.blur, onCheckedChange = { actions.onLook(state.look.copy(blur = it)) },
            title = t.blur, summary = t.blurSummary, startAction = { RowIcon(Icons.Rounded.BlurOn, null) })
    }
    SmallTitle(t.groupApp, Modifier.padding(top = 6.dp))
    var confirmHide by remember { mutableStateOf(false) }
    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 6.dp)) {
        ArrowPreference(title = t.openLsposed, summary = t.openLsposedSummary,
            startAction = { RowIcon(Icons.Rounded.OpenInNew, null) }, onClick = actions.onLsposed)
        // Enabling asks first; disabling takes effect at once.
        SwitchPreference(checked = state.iconHidden,
            onCheckedChange = { if (it) confirmHide = true else actions.onHideIcon(false) },
            title = t.hideIcon, summary = t.hideIconSummary, startAction = { RowIcon(Icons.Rounded.VisibilityOff, null) })
        ArrowPreference(title = t.about, summary = t.aboutSummary(BuildConfig.VERSION_NAME),
            startAction = { RowIcon(Icons.Rounded.Info, null) }, onClick = {})
    }
    WindowDialog(show = confirmHide, title = t.hideIconTitle, onDismissRequest = { confirmHide = false }) {
        Column {
            Text(t.hideIconMessage, Modifier.fillMaxWidth(), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(Modifier.height(24.dp))
            Row {
                TextButton(t.cancel, { confirmHide = false }, Modifier.weight(1f))
                Spacer(Modifier.size(16.dp))
                TextButton(t.hide, { confirmHide = false; actions.onHideIcon(true) }, Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary())
            }
        }
    }
}

/** A row whose value is chosen from a small popup list. */
@Composable
private fun PopupRow(icon: ImageVector, title: String, value: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ArrowPreference(title = title, summary = value, startAction = { RowIcon(icon, null) }, onClick = { open = true })
        WindowListPopup(show = open, onDismissRequest = { open = false }, alignment = PopupPositionProvider.Align.End) {
            ListPopupColumn {
                options.forEachIndexed { index, option ->
                    DropdownImpl(text = option, optionSize = options.size, isSelected = index == selected, index = index,
                        onSelectedIndexChange = { open = false; onSelect(it) })
                }
            }
        }
    }
}

@Composable
private fun RowIcon(icon: ImageVector, tint: Color?) {
    Icon(icon, null, Modifier.padding(end = 16.dp).size(24.dp), tint = tint ?: MiuixTheme.colorScheme.onSurfaceVariantSummary)
}
