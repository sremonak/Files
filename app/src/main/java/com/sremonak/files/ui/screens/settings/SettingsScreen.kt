package com.sremonak.files.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sremonak.files.FilesApp
import com.sremonak.files.R
import com.sremonak.files.data.model.SortOption
import com.sremonak.files.ui.components.SectionHeader
import com.sremonak.files.ui.theme.AccentPresets
import com.sremonak.files.ui.theme.ThemeMode
import com.sremonak.files.util.FileUtils
import com.sremonak.files.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenTrash: () -> Unit,
    onOpenAnalyzer: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
    val accentArgb by viewModel.accentColorArgb.collectAsStateWithLifecycle(initialValue = 0xFF2563EB.toInt())
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle(initialValue = 3)
    val language by viewModel.language.collectAsStateWithLifecycle(initialValue = "system")
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle(initialValue = SortOption.NAME)
    val sortAscending by viewModel.sortAscending.collectAsStateWithLifecycle(initialValue = true)
    val showHidden by viewModel.showHidden.collectAsStateWithLifecycle(initialValue = false)
    val homeDir by viewModel.homeDir.collectAsStateWithLifecycle(initialValue = "")
    val confirmDelete by viewModel.confirmDelete.collectAsStateWithLifecycle(initialValue = true)
    val haptics by viewModel.haptics.collectAsStateWithLifecycle(initialValue = true)
    val lockEnabled by viewModel.lockEnabled.collectAsStateWithLifecycle(initialValue = false)
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle(initialValue = false)
    val trashAutoDays by viewModel.trashAutoDays.collectAsStateWithLifecycle(initialValue = 30)
    val trashSize by viewModel.trashSize.collectAsStateWithLifecycle()
    val cacheSize by viewModel.cacheSize.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var showHomeDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {
            // ---------- Appearance ----------
            item {
                SectionHeader(stringResource(R.string.settings_appearance))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text(stringResource(R.string.theme_system)) }
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text(stringResource(R.string.theme_light)) }
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        label = { Text(stringResource(R.string.theme_dark)) }
                    )
                }
                SwitchRow(
                    title = "Dynamic color",
                    subtitle = "Use wallpaper-based colors (Android 12+)",
                    checked = dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accent color", modifier = Modifier.weight(1f))
                    AccentPresets.forEach { preset ->
                        val selected = Color(accentArgb) == preset
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(preset)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setAccentColor(preset.toArgb()) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Grid columns: $gridColumns", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = gridColumns.toFloat(),
                        onValueChange = { viewModel.setGridColumns(it.toInt()) },
                        valueRange = 2f..5f,
                        steps = 2
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Language:", style = MaterialTheme.typography.bodyMedium)
                    FilterChip(
                        selected = language == "system",
                        onClick = { viewModel.setLanguage("system") },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = language == "en",
                        onClick = { viewModel.setLanguage("en") },
                        label = { Text("English") }
                    )
                    FilterChip(
                        selected = language == "bn",
                        onClick = { viewModel.setLanguage("bn") },
                        label = { Text("বাংলা") }
                    )
                }
            }

            // ---------- Behavior ----------
            item {
                SectionHeader(stringResource(R.string.settings_behavior))
                ClickRow(
                    title = "Default sort",
                    subtitle = "${sortLabel(sortOption)} • ${if (sortAscending) "Ascending" else "Descending"}",
                    onClick = { showSortDialog = true }
                )
                SwitchRow(
                    title = "Show hidden files",
                    subtitle = "Files starting with a dot",
                    checked = showHidden,
                    onCheckedChange = viewModel::setShowHidden
                )
                ClickRow(
                    title = "Home folder",
                    subtitle = homeDir.ifBlank { "Internal storage (default)" },
                    onClick = { showHomeDialog = true }
                )
                SwitchRow(
                    title = "Confirm before delete",
                    subtitle = "Ask before moving files to trash",
                    checked = confirmDelete,
                    onCheckedChange = viewModel::setConfirmDelete
                )
                SwitchRow(
                    title = "Haptic feedback",
                    subtitle = "Vibrate on long-press",
                    checked = haptics,
                    onCheckedChange = viewModel::setHaptics
                )
            }

            // ---------- Security ----------
            item {
                SectionHeader(stringResource(R.string.settings_security))
                ClickRow(
                    title = if (lockEnabled) "Change app lock PIN" else "Set app lock PIN",
                    subtitle = if (lockEnabled) "PIN lock is on" else "Protect the app with a PIN",
                    onClick = { showPinDialog = true }
                )
                if (lockEnabled) {
                    SwitchRow(
                        title = "Biometric unlock",
                        subtitle = "Fingerprint / face unlock",
                        checked = biometricEnabled,
                        onCheckedChange = viewModel::setBiometric
                    )
                    ClickRow(
                        title = "Remove app lock",
                        subtitle = "Disable PIN and biometric unlock",
                        onClick = { viewModel.clearLock() }
                    )
                }
            }

            // ---------- Storage ----------
            item {
                SectionHeader(stringResource(R.string.settings_storage))
                ClickRow(
                    title = stringResource(R.string.trash_title),
                    subtitle = "${FileUtils.formatBytes(trashSize)} • auto-empty after $trashAutoDays days",
                    onClick = onOpenTrash
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-empty trash:", style = MaterialTheme.typography.bodyMedium)
                    listOf(7, 30, 90).forEach { days ->
                        FilterChip(
                            selected = trashAutoDays == days,
                            onClick = { viewModel.setTrashAutoDays(days) },
                            label = { Text("$days days") }
                        )
                    }
                    FilterChip(
                        selected = trashAutoDays == 0,
                        onClick = { viewModel.setTrashAutoDays(0) },
                        label = { Text("Never") }
                    )
                }
                ClickRow(
                    title = "Clear cache",
                    subtitle = "${FileUtils.formatBytes(cacheSize)} of cached data",
                    onClick = { viewModel.clearCache() }
                )
                ClickRow(
                    title = "Clear recent files history",
                    subtitle = "Forget recently opened files",
                    onClick = { viewModel.clearRecents() }
                )
                ClickRow(
                    title = stringResource(R.string.analyzer_title),
                    subtitle = "Find duplicates and large files",
                    onClick = onOpenAnalyzer
                )
            }

            // ---------- About ----------
            item {
                SectionHeader(stringResource(R.string.settings_about))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text("Files Pro", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${stringResource(R.string.version_label)} 1.0.0 • Premium file manager",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showPinDialog) {
            PinDialog(
                onConfirm = { pin ->
                    viewModel.setPin(pin)
                    showPinDialog = false
                },
                onDismiss = { showPinDialog = false }
            )
        }
        if (showHomeDialog) {
            HomeDirDialog(
                current = homeDir,
                onPick = { path ->
                    viewModel.setHomeDir(path)
                    showHomeDialog = false
                },
                onDismiss = { showHomeDialog = false }
            )
        }
        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
                title = { Text("Default sort") },
                text = {
                    Column {
                        SortOption.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSort(option, sortAscending)
                                        showSortDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sortLabel(option), modifier = Modifier.weight(1f))
                                if (sortOption == option) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            }
                        }
                        SwitchRow(
                            title = "Ascending",
                            subtitle = "",
                            checked = sortAscending,
                            onCheckedChange = { viewModel.setSort(sortOption, it) }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSortDialog = false }) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ClickRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PinDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set app lock PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    label = { Text("PIN (4–8 digits)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter(Char::isDigit).take(8) },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length < 4 -> error = "PIN must be at least 4 digits"
                    pin != confirm -> error = "PINs do not match"
                    else -> onConfirm(pin)
                }
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun HomeDirDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val files = FilesApp.instance.files
    val presets = listOf(
        "Internal storage" to files.internalRoot().absolutePath,
        "Downloads" to files.downloadsDir().absolutePath,
        "Pictures" to files.picturesDir().absolutePath,
        "Documents" to files.documentsDir().absolutePath,
        "DCIM" to files.dcimDir().absolutePath
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Home folder") },
        text = {
            Column {
                presets.forEach { (label, path) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(path) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, modifier = Modifier.weight(1f))
                        if (current == path) Icon(Icons.Filled.Check, contentDescription = null)
                    }
                }
                TextButton(onClick = { onPick("") }) {
                    Text("Reset to default")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun sortLabel(option: SortOption): String = when (option) {
    SortOption.NAME -> "Name"
    SortOption.SIZE -> "Size"
    SortOption.DATE_MODIFIED -> "Date modified"
    SortOption.DATE_CREATED -> "Date created"
    SortOption.TYPE -> "Type"
}
