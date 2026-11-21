package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import com.sremonak.files.data.model.SortOption
import com.sremonak.files.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel : ViewModel() {

    private val settings = FilesApp.instance.settings
    private val files = FilesApp.instance.files

    val themeMode: Flow<ThemeMode> = settings.themeMode
    val dynamicColor: Flow<Boolean> = settings.dynamicColor
    val accentColorArgb: Flow<Int> = settings.accentColorArgb
    val gridColumns: Flow<Int> = settings.gridColumns
    val language: Flow<String> = settings.language
    val sortOption: Flow<SortOption> = settings.sortOption
    val sortAscending: Flow<Boolean> = settings.sortAscending
    val showHidden: Flow<Boolean> = settings.showHidden
    val homeDir: Flow<String> = settings.homeDir
    val confirmDelete: Flow<Boolean> = settings.confirmDelete
    val haptics: Flow<Boolean> = settings.haptics
    val lockEnabled: Flow<Boolean> = settings.lockEnabled
    val biometricEnabled: Flow<Boolean> = settings.biometricEnabled
    val trashAutoDays: Flow<Int> = settings.trashAutoDays

    private val _trashSize = MutableStateFlow(0L)
    val trashSize: StateFlow<Long> = _trashSize.asStateFlow()

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    init {
        refreshSizes()
    }

    fun refreshSizes() {
        viewModelScope.launch {
            _trashSize.value = files.trashSize()
            _cacheSize.value = cacheDirSize()
        }
    }

    private suspend fun cacheDirSize(): Long = withContext(Dispatchers.IO) {
        val cache = FilesApp.instance.cacheDir
        var total = 0L
        try {
            cache.walkTopDown().forEach { f -> if (f.isFile) total += f.length() }
        } catch (_: Exception) { }
        total
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FilesApp.instance.cacheDir.deleteRecursively()
                FilesApp.instance.cacheDir.mkdirs()
            } catch (_: Exception) { }
            refreshSizes()
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settings.setDynamicColor(enabled) }
    fun setAccentColor(argb: Int) = viewModelScope.launch { settings.setAccentColor(argb) }
    fun setGridColumns(columns: Int) = viewModelScope.launch { settings.setGridColumns(columns) }
    fun setLanguage(lang: String) = viewModelScope.launch { settings.setLanguage(lang) }
    fun setSort(option: SortOption, ascending: Boolean) =
        viewModelScope.launch { settings.setSort(option, ascending) }
    fun setShowHidden(show: Boolean) = viewModelScope.launch { settings.setShowHidden(show) }
    fun setHomeDir(path: String) = viewModelScope.launch { settings.setHomeDir(path) }
    fun setConfirmDelete(confirm: Boolean) = viewModelScope.launch { settings.setConfirmDelete(confirm) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { settings.setHaptics(enabled) }
    fun setBiometric(enabled: Boolean) = viewModelScope.launch { settings.setBiometric(enabled) }
    fun setTrashAutoDays(days: Int) = viewModelScope.launch { settings.setTrashAutoDays(days) }
    fun setPin(pin: String) = viewModelScope.launch { settings.setPin(pin) }
    fun clearLock() = viewModelScope.launch { settings.clearLock() }
    fun clearRecents() = viewModelScope.launch { settings.clearRecents() }
}
