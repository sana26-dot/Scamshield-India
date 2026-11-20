package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "scamshield_prefs")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME = stringPreferencesKey("theme_preference")
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[Keys.LANGUAGE])
    }

    val themeFlow: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        ThemePreference.fromKey(prefs[Keys.THEME])
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme.key
        }
    }
}
