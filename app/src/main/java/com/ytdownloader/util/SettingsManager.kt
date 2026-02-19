package com.ytdownloader.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        // Keys
        private val YTDLP_VERSION_KEY = stringPreferencesKey("yt_dlp_version")
        private val DEFAULT_QUALITY_KEY = stringPreferencesKey("default_quality")
        private val DEFAULT_FORMAT_KEY = stringPreferencesKey("default_format")
        private val DOWNLOAD_LOCATION_KEY = stringPreferencesKey("download_location")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val AUTO_UPDATE_YTDLP_KEY = booleanPreferencesKey("auto_update_yt_dlp")
        private val LAST_UPDATE_CHECK_KEY = longPreferencesKey("last_update_check")
    }

    // YT-DLP Version
    val ytDlpVersion: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[YTDLP_VERSION_KEY] }

    suspend fun setYTDLPVersion(version: String) {
        context.dataStore.edit { preferences ->
            preferences[YTDLP_VERSION_KEY] = version
        }
    }

    // Default Quality
    val defaultQuality: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[DEFAULT_QUALITY_KEY] ?: "best" }

    suspend fun setDefaultQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_QUALITY_KEY] = quality
        }
    }

    // Default Format (video, audio, both)
    val defaultFormat: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[DEFAULT_FORMAT_KEY] ?: "video" }

    suspend fun setDefaultFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_FORMAT_KEY] = format
        }
    }

    // Download Location
    val downloadLocation: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[DOWNLOAD_LOCATION_KEY] }

    suspend fun setDownloadLocation(location: String) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_LOCATION_KEY] = location
        }
    }

    // Notifications Enabled
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATIONS_ENABLED_KEY] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    // Auto Update YT-DLP
    val autoUpdateYTDLP: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_UPDATE_YTDLP_KEY] ?: false }

    suspend fun setAutoUpdateYTDLP(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_UPDATE_YTDLP_KEY] = enabled
        }
    }

    // Last Update Check
    val lastUpdateCheck: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_UPDATE_CHECK_KEY] ?: 0L }

    suspend fun setLastUpdateCheck(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_UPDATE_CHECK_KEY] = timestamp
        }
    }

    // Clear all settings
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
