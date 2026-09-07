package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.network.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "deck_settings")

data class DeckPreferences(
    val backendHost: String = ServerConfig.DEFAULT_HOST,
    val backendPort: Int = ServerConfig.DEFAULT_PORT,
    val fontScale: Float = 1.0f,
    val animationsEnabled: Boolean = true,
    val autoExportToStorage: Boolean = true,
    val darkThemeOnly: Boolean = true
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val BACKEND_HOST = stringPreferencesKey("backend_host")
        val BACKEND_PORT = intPreferencesKey("backend_port")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val AUTO_EXPORT = booleanPreferencesKey("auto_export")
        val DARK_THEME_ONLY = booleanPreferencesKey("dark_theme_only")
    }

    val preferencesFlow: Flow<DeckPreferences> = context.dataStore.data.map { prefs ->
        DeckPreferences(
            backendHost = prefs[Keys.BACKEND_HOST] ?: ServerConfig.DEFAULT_HOST,
            backendPort = prefs[Keys.BACKEND_PORT] ?: ServerConfig.DEFAULT_PORT,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
            autoExportToStorage = prefs[Keys.AUTO_EXPORT] ?: true,
            darkThemeOnly = prefs[Keys.DARK_THEME_ONLY] ?: true
        )
    }

    suspend fun updateServerConfig(host: String, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKEND_HOST] = host
            prefs[Keys.BACKEND_PORT] = port
        }
    }

    suspend fun updateFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SCALE] = scale
        }
    }

    suspend fun updateAnimations(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ANIMATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateAutoExport(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_EXPORT] = enabled
        }
    }
}
