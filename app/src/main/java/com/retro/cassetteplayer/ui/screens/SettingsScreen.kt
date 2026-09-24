package com.retro.cassetteplayer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import java.time.LocalDate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.AppLanguage
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.ui.theme.AppTheme
import com.retro.cassetteplayer.ui.theme.Ink
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import com.retro.cassetteplayer.ui.theme.ThemeMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.retro.cassetteplayer.data.AppSettings
import com.retro.cassetteplayer.data.CassetteModel
import com.retro.cassetteplayer.data.LabelColor
import com.retro.cassetteplayer.data.ReplayGainMode
import com.retro.cassetteplayer.ui.theme.InkRaised
import com.retro.cassetteplayer.ui.theme.OutlineColor

@Composable
fun SettingsScreen(
    equalizerEnabled: Boolean,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenChangelog: () -> Unit,
    onReloadLibrary: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onOpenStats: () -> Unit,
    lyricsIndexed: Int,
    totalSongs: Int,
    lyricsDownload: Pair<Int, Int>?,
    onToggleLyricsDownload: () -> Unit,
    onEnter: () -> Unit,
) {
    LaunchedEffect(Unit) { onEnter() }
    val crossfade by AppSettings.crossfadeSeconds.flow.collectAsState()
    val replayGain by AppSettings.replayGain.flow.collectAsState()
    val resumeOnConnect by AppSettings.resumeOnConnect.flow.collectAsState()
    val hiRes by AppSettings.hiResOutput.flow.collectAsState()
    val hiss by AppSettings.tapeHiss.flow.collectAsState()
    val wow by AppSettings.wowFlutter.flow.collectAsState()
    val clicks by AppSettings.keyClicks.flow.collectAsState()
    val model by AppSettings.cassetteModel.flow.collectAsState()
    val labelColor by AppSettings.labelColor.flow.collectAsState()
    val handwritten by AppSettings.handwrittenLabel.flow.collectAsState()
    val coverColors by AppSettings.coverColors.flow.collectAsState()
    val sideAB by AppSettings.sideAB.flow.collectAsState()
    val scrobbling by AppSettings.scrobbling.flow.collectAsState()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(onExportBackup) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportBackup) }
    val context = LocalContext.current
    val currentLanguage = remember { AppLanguage.current(context) }
    var choosingLanguage by remember { mutableStateOf(false) }
    var choosingTheme by remember { mutableStateOf(false) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }

    if (choosingLanguage) {
        LanguageDialog(
            current = currentLanguage,
            onSelect = { tag ->
                choosingLanguage = false
                context.findActivity()?.let { AppLanguage.apply(it, tag) }
            },
            onDismiss = { choosingLanguage = false },
        )
    }

    if (choosingTheme) {
        ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to stringResource(it.label) },
            selected = AppTheme.mode,
            onSelect = { mode ->
                choosingTheme = false
                AppTheme.setMode(context, mode)
            },
            onDismiss = { choosingTheme = false },
        )
    }

    when (dialog) {
        SettingsDialog.CROSSFADE -> ChoiceDialog(
            title = stringResource(R.string.settings_crossfade),
            options = CROSSFADE_OPTIONS.map { it to crossfadeLabel(it) },
            selected = crossfade,
            onSelect = { AppSettings.crossfadeSeconds.set(it); dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.REPLAY_GAIN -> ChoiceDialog(
            title = stringResource(R.string.settings_replay_gain),
            options = ReplayGainMode.entries.map { it to stringResource(it.label) },
            selected = replayGain,
            onSelect = { AppSettings.replayGain.set(it); dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.MODEL -> ChoiceDialog(
            title = stringResource(R.string.settings_cassette_model),
            options = CassetteModel.entries.map { it to stringResource(it.label) },
            selected = model,
            onSelect = { AppSettings.cassetteModel.set(it); dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.LABEL_COLOR -> ChoiceDialog(
            title = stringResource(R.string.settings_label_color),
            options = LabelColor.entries.map { it to stringResource(it.label) },
            selected = labelColor,
            onSelect = { AppSettings.labelColor.set(it); dialog = null },
            onDismiss = { dialog = null },
            swatch = { Color(it.argb) },
        )
        null -> Unit
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        ScreenTopBar(stringResource(R.string.settings_title), onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionTitle(stringResource(R.string.settings_section_you)) }
            item {
                SettingsRow(
                    icon = Icons.Rounded.BarChart,
                    title = stringResource(R.string.settings_stats),
                    summary = stringResource(R.string.settings_stats_summary),
                    onClick = onOpenStats,
                    showChevron = true,
                )
            }
            item { SectionTitle(stringResource(R.string.settings_section_audio)) }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Equalizer,
                    title = stringResource(R.string.settings_equalizer),
                    summary = stringResource(if (equalizerEnabled) R.string.settings_on else R.string.settings_off),
                    onClick = onOpenEqualizer,
                    showChevron = true,
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.SwapHoriz,
                    title = stringResource(R.string.settings_crossfade),
                    summary = stringResource(R.string.settings_crossfade_summary, crossfadeLabel(crossfade)),
                    onClick = { dialog = SettingsDialog.CROSSFADE },
                )
            }
            item {
                SettingsRow(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    title = stringResource(R.string.settings_replay_gain),
                    summary = stringResource(R.string.settings_replay_gain_summary, stringResource(replayGain.label)),
                    onClick = { dialog = SettingsDialog.REPLAY_GAIN },
                )
            }
            item {
                SwitchRow(Icons.Rounded.Headphones, stringResource(R.string.settings_resume_connect), stringResource(R.string.settings_resume_connect_summary), resumeOnConnect) {
                    AppSettings.resumeOnConnect.set(it)
                }
            }
            item {
                SwitchRow(Icons.Rounded.HighQuality, stringResource(R.string.settings_hires), stringResource(R.string.settings_hires_summary), hiRes) {
                    AppSettings.hiResOutput.set(it)
                }
            }
            item { SectionTitle(stringResource(R.string.settings_section_tape)) }
            item {
                SwitchRow(Icons.Rounded.GraphicEq, stringResource(R.string.settings_hiss), stringResource(R.string.settings_hiss_summary), hiss) {
                    AppSettings.tapeHiss.set(it)
                }
            }
            item {
                SwitchRow(Icons.Rounded.Waves, stringResource(R.string.settings_wow), stringResource(R.string.settings_wow_summary), wow) {
                    AppSettings.wowFlutter.set(it)
                }
            }
            item {
                SwitchRow(Icons.Rounded.TouchApp, stringResource(R.string.settings_clicks), stringResource(R.string.settings_clicks_summary), clicks) {
                    AppSettings.keyClicks.set(it)
                }
            }
            item { SectionTitle(stringResource(R.string.settings_section_appearance)) }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Contrast,
                    title = stringResource(R.string.settings_theme),
                    summary = stringResource(AppTheme.mode.label),
                    onClick = { choosingTheme = true },
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Album,
                    title = stringResource(R.string.settings_cassette_model),
                    summary = stringResource(model.label),
                    onClick = { dialog = SettingsDialog.MODEL },
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_label_color),
                    summary = stringResource(labelColor.label),
                    onClick = { dialog = SettingsDialog.LABEL_COLOR },
                    swatch = Color(labelColor.argb),
                )
            }
            item {
                SwitchRow(Icons.Rounded.Draw, stringResource(R.string.settings_handwritten), stringResource(R.string.settings_handwritten_summary), handwritten) {
                    AppSettings.handwrittenLabel.set(it)
                }
            }
            item {
                SwitchRow(Icons.Rounded.ColorLens, stringResource(R.string.settings_cover_colors), stringResource(R.string.settings_cover_colors_summary), coverColors) {
                    AppSettings.coverColors.set(it)
                }
            }
            item {
                SwitchRow(Icons.Rounded.FlipCameraAndroid, stringResource(R.string.settings_side_ab), stringResource(R.string.settings_side_ab_summary), sideAB) {
                    AppSettings.sideAB.set(it)
                }
            }
            item { SectionTitle(stringResource(R.string.settings_section_integrations)) }
            item {
                SwitchRow(Icons.Rounded.Podcasts, stringResource(R.string.settings_scrobbling), stringResource(R.string.settings_scrobbling_summary), scrobbling) {
                    AppSettings.scrobbling.set(it)
                }
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Lyrics,
                    title = stringResource(R.string.settings_lyrics_index),
                    summary = if (lyricsDownload != null) {
                        stringResource(R.string.settings_lyrics_index_running, lyricsDownload.first, lyricsDownload.second)
                    } else {
                        stringResource(R.string.settings_lyrics_index_summary, lyricsIndexed, totalSongs)
                    },
                    onClick = onToggleLyricsDownload,
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.DirectionsCar,
                    title = stringResource(R.string.settings_auto),
                    summary = stringResource(R.string.settings_auto_summary),
                    onClick = null,
                )
            }
            item { SectionTitle(stringResource(R.string.settings_section_general)) }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_language),
                    summary = stringResource(
                        AppLanguage.options.firstOrNull { it.tag == currentLanguage }?.label
                            ?: R.string.language_system
                    ),
                    onClick = { choosingLanguage = true },
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Refresh,
                    title = stringResource(R.string.action_reload_library),
                    summary = stringResource(R.string.settings_reload_summary),
                    onClick = onReloadLibrary,
                )
            }
            item { SectionTitle(stringResource(R.string.settings_section_backup)) }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Upload,
                    title = stringResource(R.string.settings_backup_export),
                    summary = stringResource(R.string.settings_backup_export_summary),
                    onClick = { exportLauncher.launch("retro-cassette-backup-${LocalDate.now()}.json") },
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Download,
                    title = stringResource(R.string.settings_backup_import),
                    summary = stringResource(R.string.settings_backup_import_summary),
                    // Some file managers don't tag .json files, so accept any file and validate it
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
                )
            }
            item { SectionTitle(stringResource(R.string.settings_section_about)) }
            item {
                SettingsRow(
                    icon = Icons.Outlined.NewReleases,
                    title = stringResource(R.string.settings_whats_new),
                    summary = stringResource(R.string.settings_whats_new_summary),
                    onClick = onOpenChangelog,
                    showChevron = true,
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_version),
                    summary = versionName,
                    onClick = null,
                )
            }
        }
    }
}

@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = TextPrimary,
            )
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TapeOrange,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: (() -> Unit)?,
    showChevron: Boolean = false,
    swatch: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 20.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            if (summary.isNotEmpty()) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        if (swatch != null) {
            Box(
                Modifier
                    .size(22.dp)
                    .background(swatch, CircleShape)
            )
        }
        if (showChevron) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 20.dp, end = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TapeOrange,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = InkRaised,
                uncheckedBorderColor = OutlineColor,
            ),
        )
    }
}

private enum class SettingsDialog { CROSSFADE, REPLAY_GAIN, MODEL, LABEL_COLOR }

private val CROSSFADE_OPTIONS = listOf(0, 2, 4, 6, 8, 12)

@Composable
private fun crossfadeLabel(seconds: Int): String =
    if (seconds == 0) stringResource(R.string.crossfade_off) else stringResource(R.string.crossfade_seconds, seconds)

@Composable
private fun LanguageDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ChoiceDialog(
        title = stringResource(R.string.settings_language),
        options = AppLanguage.options.map { it.tag to stringResource(it.label) },
        selected = current,
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

/** Single-choice dialog with radio buttons. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    swatch: ((T) -> Color)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = InkSurface,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = TapeOrange),
                        )
                        if (swatch != null) {
                            Box(
                                Modifier
                                    .padding(end = 10.dp)
                                    .size(18.dp)
                                    .background(swatch(value), CircleShape)
                            )
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (value == selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = TextPrimary,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = TextPrimary) }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
