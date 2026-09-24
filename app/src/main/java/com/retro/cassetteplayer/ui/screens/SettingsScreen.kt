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

@Composable
fun SettingsScreen(
    equalizerEnabled: Boolean,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenChangelog: () -> Unit,
    onReloadLibrary: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
) {
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        ScreenTopBar(stringResource(R.string.settings_title), onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
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
            item { SectionTitle(stringResource(R.string.settings_section_appearance)) }
            item {
                SettingsRow(
                    icon = Icons.Rounded.Contrast,
                    title = stringResource(R.string.settings_theme),
                    summary = stringResource(AppTheme.mode.label),
                    onClick = { choosingTheme = true },
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
        if (showChevron) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

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
