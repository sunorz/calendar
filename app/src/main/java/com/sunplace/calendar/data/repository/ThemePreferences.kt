package com.sunplace.calendar.data.repository

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.sunplace.calendar.data.model.ThemeMode
import com.sunplace.calendar.data.model.TodayPattern
import com.sunplace.calendar.util.TranslationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class ThemePreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ELDERLY_MODE = "key_elderly_mode"
        private const val KEY_SHOW_GUANGXI_SANYUESAN = "key_show_guangxi_sanyuesan"
        private const val KEY_TODAY_PATTERN = "key_today_pattern"
        const val KEY_APP_LANGUAGE = "key_app_language"

        fun getSystemLanguage(): String {
            val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                Resources.getSystem().configuration.locale
            }
            return if (sysLocale.language.equals("en", ignoreCase = true)) "en" else "zh"
        }

        fun applyAppLanguage(context: Context, lang: String) {
            val targetLocale = if (lang.equals("en", ignoreCase = true)) Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE
            Locale.setDefault(targetLocale)
            TranslationHelper.appLanguageOverride = lang

            val appCtx = context.applicationContext ?: context
            val resources = appCtx.resources
            val config = Configuration(resources.configuration)
            config.setLocale(targetLocale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(targetLocale))
            }
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeManager = context.getSystemService(LocaleManager::class.java)
                    val localeTag = if (lang.equals("en", ignoreCase = true)) "en" else "zh-CN"
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(localeTag)
                } catch (_: Exception) {}
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadSavedAppLanguage(): String {
        return if (prefs.contains(KEY_APP_LANGUAGE)) {
            prefs.getString(KEY_APP_LANGUAGE, "zh") ?: "zh"
        } else {
            getSystemLanguage()
        }
    }

    private val _themeMode = MutableStateFlow(loadSavedTheme())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isElderlyMode = MutableStateFlow(loadSavedElderlyMode())
    val isElderlyMode: StateFlow<Boolean> = _isElderlyMode.asStateFlow()

    private val _showGuangxiSanyuesan = MutableStateFlow(loadSavedShowGuangxiSanyuesan())
    val showGuangxiSanyuesan: StateFlow<Boolean> = _showGuangxiSanyuesan.asStateFlow()

    private val _todayPattern = MutableStateFlow(loadSavedTodayPattern())
    val todayPattern: StateFlow<TodayPattern> = _todayPattern.asStateFlow()

    private val _appLanguage = MutableStateFlow(loadSavedAppLanguage())
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun isCustomLanguageSaved(): Boolean = prefs.contains(KEY_APP_LANGUAGE)

    private fun loadSavedTodayPattern(): TodayPattern {
        val id = prefs.getString(KEY_TODAY_PATTERN, TodayPattern.DEFAULT.id)
        return TodayPattern.fromId(id)
    }

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

    private fun loadSavedShowGuangxiSanyuesan(): Boolean {
        return prefs.getBoolean(KEY_SHOW_GUANGXI_SANYUESAN, true)
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

    fun setShowGuangxiSanyuesan(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_GUANGXI_SANYUESAN, enabled).apply()
        _showGuangxiSanyuesan.value = enabled
    }

    fun toggleShowGuangxiSanyuesan() {
        setShowGuangxiSanyuesan(!_showGuangxiSanyuesan.value)
    }

    fun setTodayPattern(pattern: TodayPattern) {
        prefs.edit().putString(KEY_TODAY_PATTERN, pattern.id).apply()
        _todayPattern.value = pattern
    }

    fun setAppLanguage(lang: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
        _appLanguage.value = lang
        applyAppLanguage(context, lang)
    }

    fun applySavedLanguage() {
        val currentLang = loadSavedAppLanguage()
        _appLanguage.value = currentLang
        applyAppLanguage(context, currentLang)
    }

    fun onSystemConfigurationChanged() {
        if (!prefs.contains(KEY_APP_LANGUAGE)) {
            val sysLang = getSystemLanguage()
            if (_appLanguage.value != sysLang) {
                _appLanguage.value = sysLang
                applyAppLanguage(context, sysLang)
            }
        }
    }
}
