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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.leekin2.xsecurity.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialScreen(state: UiState, actions: Actions) {
    val t = textsFor(state.language)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    BackHandler(state.settingsOpen) { actions.onSettings(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(if (state.settingsOpen) t.tabSettings else "XSecurity") },
                navigationIcon = {
                    if (state.settingsOpen) IconButton(onClick = { actions.onSettings(false) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, t.tabHome)
                    }
                },
                actions = {
                    if (!state.settingsOpen) IconButton(onClick = { actions.onSettings(true) }) { Icon(Icons.Rounded.Tune, t.tabSettings) }
                },
                scrollBehavior = scroll
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp)) {
            if (state.settingsOpen) {
                item { SettingsPage(state, t, actions) }
            } else {
                item { StatusHeader(state, t) }
                items(SECTIONS, key = { it.id }) { section -> SectionCard(section, state, t, actions) }
                item {
                    Button(onClick = actions.onRestart, enabled = state.connected == true && !state.restarting,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp)) {
                        Icon(Icons.Rounded.RestartAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text(t.restart)
                    }
                    Text(t.restartSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusHeader(state: UiState, t: Texts) {
    val scheme = MaterialTheme.colorScheme
    val (accent, icon, title) = when (state.connected) {
        null -> Triple(scheme.primary, Icons.Rounded.HourglassTop, t.connecting)
        false -> Triple(scheme.error, Icons.Rounded.HighlightOff, t.inactive)
        true -> Triple(Color(0xFF2E9D57), Icons.Rounded.CheckCircle, t.active)
    }
    val total = SECTIONS.sumOf { it.items.size }
    val on = SECTIONS.sumOf { s -> s.items.count { state.values[it.key] == true } }
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Shield, null, Modifier.size(30.dp), tint = accent)
                Icon(icon, null, Modifier.size(18.dp).align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp), tint = accent)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.SemiBold)
                Text(if (state.connected == true) t.activeSummary else if (state.connected == false) t.inactiveSummary else t.version(BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t.onCount(on, total), accent)
                    Chip(t.version(BuildConfig.VERSION_NAME), scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
private fun SectionCard(section: Section, state: UiState, t: Texts, actions: Actions) {
    var open by rememberSaveable(section.id) { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "section")
    val on = section.items.count { state.values[it.key] == true }
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(section.color), contentAlignment = Alignment.Center) {
                Icon(section.icon, null, Modifier.size(24.dp), tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t.sections[section.id] ?: section.id, style = MaterialTheme.typography.titleMedium)
                Text(t.onCount(on, section.items.size), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ExpandMore, null, Modifier.rotate(turn))
        }
        AnimatedVisibility(open, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
            Column {
                section.items.forEach { item ->
                    val line = t.features[item.key]
                    val checked = state.values[item.key] == true
                    ListItem(
                        headlineContent = { Text(line?.title ?: item.key) },
                        supportingContent = line?.summary?.let { { Text(it) } },
                        leadingContent = { Icon(item.icon, null, tint = section.color) },
                        trailingContent = { Switch(checked, { actions.onChange(item.key, it) }, enabled = state.connected != false) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(enabled = state.connected != false) { actions.onChange(item.key, !checked) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(state: UiState, t: Texts, actions: Actions) {
    Title(t.groupLook)
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        MenuRow(Icons.Rounded.Language, t.language, state.language.label, AppLanguage.entries.map { it.label }) {
            actions.onLanguage(AppLanguage.entries[it])
        }
        val themes = listOf(t.themeSystem, t.themeLight, t.themeDark)
        val themeIcon = when (state.look.theme) { 1 -> Icons.Rounded.LightMode; 2 -> Icons.Rounded.DarkMode; else -> Icons.Rounded.BrightnessAuto }
        MenuRow(themeIcon, t.theme, themes[state.look.theme], themes) { actions.onLook(state.look.copy(theme = it)) }
        SwitchRow(Icons.Rounded.Palette, t.hyperStyle, t.hyperStyleSummary, state.look.hyper) { actions.onLook(state.look.copy(hyper = it)) }
    }
    Title(t.groupApp)
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        ListItem(headlineContent = { Text(t.openLsposed) }, supportingContent = { Text(t.openLsposedSummary) },
            leadingContent = { Icon(Icons.Rounded.OpenInNew, null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = actions.onLsposed))
        ListItem(headlineContent = { Text(t.about) }, supportingContent = { Text(t.aboutSummary(BuildConfig.VERSION_NAME)) },
            leadingContent = { Icon(Icons.Rounded.Info, null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(summary) }, leadingContent = { Icon(icon, null) },
        trailingContent = { Switch(checked, onChange) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onChange(!checked) })
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, value: String, options: List<String>, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ListItem(headlineContent = { Text(title) }, supportingContent = { Text(value) }, leadingContent = { Icon(icon, null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.padding(start = 56.dp)) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onSelect(index) })
            }
        }
    }
}
