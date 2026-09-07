package com.sunplace.calendar.data.repository

import android.content.Context
import com.sunplace.calendar.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ELDERLY_MODE = "key_elderly_mode"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadSavedTheme())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isElderlyMode = MutableStateFlow(loadSavedElderlyMode())
    val isElderlyMode: StateFlow<Boolean> = _isElderlyMode.asStateFlow()

    private fun loadSavedTheme(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun loadSavedElderlyMode(): Boolean {
        return prefs.getBoolean(KEY_ELDERLY_MODE, false)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setElderlyMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ELDERLY_MODE, enabled).apply()
        _isElderlyMode.value = enabled
    }

    fun toggleElderlyMode() {
        setElderlyMode(!_isElderlyMode.value)
    }
}
