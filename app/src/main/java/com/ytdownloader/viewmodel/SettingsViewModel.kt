package com.ytdownloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ytdownloader.util.SettingsManager
import com.ytdownloader.util.YTDLPManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val ytdlpManager = YTDLPManager(application)
    private val settingsManager = SettingsManager(application)

    // UI State
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Settings flows
    val defaultQuality: StateFlow<String> = settingsManager.defaultQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "best")

    val defaultFormat: StateFlow<String> = settingsManager.defaultFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "video")

    val notificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoUpdateYTDLP: StateFlow<Boolean> = settingsManager.autoUpdateYTDLP
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadYTDLPInfo()
    }

    private fun loadYTDLPInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingVersion = true)
            
            val currentVersion = ytdlpManager.getCurrentVersion()
            val latestVersion = ytdlpManager.getLatestVersion()
            
            _uiState.value = _uiState.value.copy(
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                isUpdateAvailable = latestVersion != null && currentVersion != latestVersion,
                isLoadingVersion = false
            )
            
            // Save version to settings
            currentVersion?.let {
                settingsManager.setYTDLPVersion(it)
            }
        }
    }

    fun updateYTDLP() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdating = true,
                updateProgress = 0,
                updateError = null
            )
            
            ytdlpManager.updateYTDLP { progress ->
                _uiState.value = _uiState.value.copy(updateProgress = progress)
            }.onSuccess { version ->
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    currentVersion = version,
                    isUpdateAvailable = false,
                    updateMessage = "Updated to version $version"
                )
                settingsManager.setYTDLPVersion(version)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    updateError = error.message ?: "Update failed"
                )
            }
        }
    }

    fun checkForUpdate() {
        loadYTDLPInfo()
    }

    fun setDefaultQuality(quality: String) {
        viewModelScope.launch {
            settingsManager.setDefaultQuality(quality)
        }
    }

    fun setDefaultFormat(format: String) {
        viewModelScope.launch {
            settingsManager.setDefaultFormat(format)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotificationsEnabled(enabled)
        }
    }

    fun setAutoUpdateYTDLP(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAutoUpdateYTDLP(enabled)
        }
    }

    fun clearUpdateMessage() {
        _uiState.value = _uiState.value.copy(updateMessage = null)
    }

    fun clearUpdateError() {
        _uiState.value = _uiState.value.copy(updateError = null)
    }

    data class SettingsUiState(
        val currentVersion: String? = null,
        val latestVersion: String? = null,
        val isUpdateAvailable: Boolean = false,
        val isLoadingVersion: Boolean = false,
        val isUpdating: Boolean = false,
        val updateProgress: Int = 0,
        val updateMessage: String? = null,
        val updateError: String? = null
    )
}
