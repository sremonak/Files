package com.sremonak.files.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sremonak.files.data.model.SortOption
import com.sremonak.files.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "files_pro_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SORT_ASC = booleanPreferencesKey("sort_ascending")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val HOME_DIR = stringPreferencesKey("home_dir")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val HAPTICS = booleanPreferencesKey("haptics")
        val LANGUAGE = stringPreferencesKey("language")
        val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val TRASH_DAYS = intPreferencesKey("trash_auto_days")
        val FAVORITES = stringSetPreferencesKey("favorites")
        val RECENTS = stringPreferencesKey("recents")
    }

    // ---------- Appearance ----------
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }
    val accentColorArgb: Flow<Int> = context.dataStore.data.map { it[Keys.ACCENT_COLOR] ?: 0xFF2563EB.toInt() }
    val gridColumns: Flow<Int> = context.dataStore.data.map { it[Keys.GRID_COLUMNS] ?: 3 }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "system" }

    // ---------- Behavior ----------
    val sortOption: Flow<SortOption> = context.dataStore.data.map { prefs ->
        runCatching { SortOption.valueOf(prefs[Keys.SORT_OPTION] ?: "NAME") }.getOrDefault(SortOption.NAME)
    }
    val sortAscending: Flow<Boolean> = context.dataStore.data.map { it[Keys.SORT_ASC] ?: true }
    val showHidden: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_HIDDEN] ?: false }
    val homeDir: Flow<String> = context.dataStore.data.map { it[Keys.HOME_DIR] ?: "" }
    val confirmDelete: Flow<Boolean> = context.dataStore.data.map { it[Keys.CONFIRM_DELETE] ?: true }
    val haptics: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTICS] ?: true }

    // ---------- Security ----------
    val lockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOCK_ENABLED] ?: false }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BIOMETRIC] ?: false }

    // ---------- Storage ----------
    val trashAutoDays: Flow<Int> = context.dataStore.data.map { it[Keys.TRASH_DAYS] ?: 30 }

    // ---------- Favorites & recents ----------
    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[Keys.FAVORITES] ?: emptySet() }

    val recents: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENTS] ?: return@map emptyList()
        raw.split("\n").filter { it.isNotBlank() }
    }

    // ---------- Setters ----------
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setAccentColor(argb: Int) = edit { it[Keys.ACCENT_COLOR] = argb }
    suspend fun setGridColumns(columns: Int) = edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(2, 5) }
    suspend fun setLanguage(lang: String) = edit { it[Keys.LANGUAGE] = lang }
    suspend fun setSort(option: SortOption, ascending: Boolean) = edit {
        it[Keys.SORT_OPTION] = option.name
        it[Keys.SORT_ASC] = ascending
    }
    suspend fun setShowHidden(show: Boolean) = edit { it[Keys.SHOW_HIDDEN] = show }
    suspend fun setHomeDir(path: String) = edit { it[Keys.HOME_DIR] = path }
    suspend fun setConfirmDelete(confirm: Boolean) = edit { it[Keys.CONFIRM_DELETE] = confirm }
    suspend fun setHaptics(enabled: Boolean) = edit { it[Keys.HAPTICS] = enabled }
    suspend fun setBiometric(enabled: Boolean) = edit { it[Keys.BIOMETRIC] = enabled }
    suspend fun setTrashAutoDays(days: Int) = edit { it[Keys.TRASH_DAYS] = days }

    suspend fun toggleFavorite(path: String): Boolean {
        var nowFavorite = false
        edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            nowFavorite = path !in current
            prefs[Keys.FAVORITES] = if (nowFavorite) current + path else current - path
        }
        return nowFavorite
    }

    suspend fun addRecent(path: String) {
        edit { prefs ->
            val current = (prefs[Keys.RECENTS] ?: "").split("\n").filter { it.isNotBlank() }
            val updated = (listOf(path) + current.filterNot { it == path }).take(30)
            prefs[Keys.RECENTS] = updated.joinToString("\n")
        }
    }

    suspend fun clearRecents() = edit { it.remove(Keys.RECENTS) }

    // ---------- PIN lock ----------
    suspend fun setPin(pin: String) = edit {
        it[Keys.PIN_HASH] = hashPin(pin)
        it[Keys.LOCK_ENABLED] = true
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = context.dataStore.data.first()[Keys.PIN_HASH] ?: return false
        return stored == hashPin(pin)
    }

    suspend fun clearLock() = edit {
        it.remove(Keys.PIN_HASH)
        it[Keys.LOCK_ENABLED] = false
        it[Keys.BIOMETRIC] = false
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("filespro:$pin".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private suspend fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
