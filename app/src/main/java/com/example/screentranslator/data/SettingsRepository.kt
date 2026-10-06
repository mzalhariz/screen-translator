package com.example.screentranslator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Persisted user preferences. */
data class AppSettings(
    val targetLang: String = "es",
    val sourceLang: String = "auto",
    val enabled: Boolean = true,
)

/**
 * Thin wrapper over Jetpack DataStore. A single DataStore instance backs the whole app via the
 * top-level [dataStore] delegate, so the service and the UI always read the same values.
 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            targetLang = prefs[Keys.TARGET_LANG] ?: "es",
            sourceLang = prefs[Keys.SOURCE_LANG] ?: "auto",
            enabled = prefs[Keys.ENABLED] ?: true,
        )
    }

    suspend fun setTargetLang(code: String) {
        context.dataStore.edit { it[Keys.TARGET_LANG] = code }
    }

    suspend fun setSourceLang(code: String) {
        context.dataStore.edit { it[Keys.SOURCE_LANG] = code }
    }

    suspend fun setEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.ENABLED] = value }
    }

    private object Keys {
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val ENABLED = booleanPreferencesKey("overlay_enabled")
    }
}
