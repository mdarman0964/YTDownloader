package com.ytdownloader.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ytdownloader.data.DownloadItem
import com.ytdownloader.data.DownloadStatus
import com.ytdownloader.data.QualityOption
import com.ytdownloader.data.VideoInfo
import com.ytdownloader.repository.DownloadRepository
import com.ytdownloader.service.DownloadService
import com.ytdownloader.util.SettingsManager
import com.ytdownloader.util.YTDLPManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository.getInstance(application)
    private val ytdlpManager = YTDLPManager(application)
    private val settingsManager = SettingsManager(application)

    // UI State
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Shared URL from intent
    private val _sharedUrl = MutableStateFlow<String?>(null)
    val sharedUrl: StateFlow<String?> = _sharedUrl.asStateFlow()

    // Video info
    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    // Quality options
    private val _videoQualities = MutableStateFlow<List<QualityOption>>(emptyList())
    val videoQualities: StateFlow<List<QualityOption>> = _videoQualities.asStateFlow()

    private val _audioQualities = MutableStateFlow<List<QualityOption>>(emptyList())
    val audioQualities: StateFlow<List<QualityOption>> = _audioQualities.asStateFlow()

    // Selected qualities
    private val _selectedVideoQuality = MutableStateFlow<QualityOption?>(null)
    val selectedVideoQuality: StateFlow<QualityOption?> = _selectedVideoQuality.asStateFlow()

    private val _selectedAudioQuality = MutableStateFlow<QualityOption?>(null)
    val selectedAudioQuality: StateFlow<QualityOption?> = _selectedAudioQuality.asStateFlow()

    // Format type
    private val _downloadFormat = MutableStateFlow<DownloadFormat>(DownloadFormat.VIDEO)
    val downloadFormat: StateFlow<DownloadFormat> = _downloadFormat.asStateFlow()

    // Downloads
    val allDownloads: StateFlow<List<DownloadItem>> = repository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkYTDLPStatus()
    }

    fun handleIntent(intent: Intent?) {
        intent?.let {
            when (it.action) {
                Intent.ACTION_SEND -> {
                    if (it.type == "text/plain") {
                        val sharedText = it.getStringExtra(Intent.EXTRA_TEXT)
                        sharedText?.let { url ->
                            if (ytdlpManager.isSupportedUrl(url)) {
                                _sharedUrl.value = url
                                _uiState.value = _uiState.value.copy(
                                    url = url,
                                    showUrlInput = true
                                )
                                fetchVideoInfo(url)
                            }
                        }
                    }
                }
                Intent.ACTION_VIEW -> {
                    it.dataString?.let { url ->
                        if (ytdlpManager.isSupportedUrl(url)) {
                            _sharedUrl.value = url
                            _uiState.value = _uiState.value.copy(
                                url = url,
                                showUrlInput = true
                            )
                            fetchVideoInfo(url)
                        }
                    }
                }
            }
        }
    }

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(url = url)
    }

    fun onPasteUrl() {
        // Clipboard paste handled in UI
    }

    fun fetchVideoInfo(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a valid URL"
            )
            return
        }

        if (!ytdlpManager.isSupportedUrl(url)) {
            _uiState.value = _uiState.value.copy(
                error = "Unsupported URL. Please enter a YouTube URL."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                showVideoInfo = false
            )

            ytdlpManager.fetchVideoInfo(url)
                .onSuccess { info ->
                    _videoInfo.value = info
                    
                    val (videoOpts, audioOpts) = ytdlpManager.getQualityOptions(info)
                    _videoQualities.value = videoOpts
                    _audioQualities.value = audioOpts
                    
                    // Select best qualities by default
                    _selectedVideoQuality.value = videoOpts.firstOrNull()
                    _selectedAudioQuality.value = audioOpts.firstOrNull()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showVideoInfo = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to fetch video info"
                    )
                }
        }
    }

    fun onVideoQualitySelected(quality: QualityOption) {
        _selectedVideoQuality.value = quality
    }

    fun onAudioQualitySelected(quality: QualityOption) {
        _selectedAudioQuality.value = quality
    }

    fun onFormatChange(format: DownloadFormat) {
        _downloadFormat.value = format
    }

    fun startDownload() {
        val info = _videoInfo.value ?: return
        val url = _uiState.value.url
        
        viewModelScope.launch {
            val quality = when (_downloadFormat.value) {
                DownloadFormat.VIDEO -> _selectedVideoQuality.value?.displayName ?: "best"
                DownloadFormat.AUDIO -> _selectedAudioQuality.value?.displayName ?: "best"
                DownloadFormat.BOTH -> "best"
            }
            
            val format = when (_downloadFormat.value) {
                DownloadFormat.VIDEO -> "video"
                DownloadFormat.AUDIO -> "audio"
                DownloadFormat.BOTH -> "both"
            }
            
            val downloadId = repository.createDownload(
                url = url,
                title = info.title,
                thumbnailUrl = info.thumbnail,
                uploader = info.uploader,
                duration = info.durationString,
                quality = quality,
                format = format
            )
            
            // Start download service
            val intent = Intent(getApplication(), DownloadService::class.java).apply {
                action = DownloadService.ACTION_START_DOWNLOAD
                putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
            }
            getApplication<Application>().startService(intent)
            
            _uiState.value = _uiState.value.copy(
                downloadStarted = true,
                showVideoInfo = false,
                url = ""
            )
            _videoInfo.value = null
            
            // Reset download started flag after a delay
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(downloadStarted = false)
        }
    }

    fun retryDownload(download: DownloadItem) {
        viewModelScope.launch {
            repository.updateStatus(download.id, DownloadStatus.PENDING)
            
            val intent = Intent(getApplication(), DownloadService::class.java).apply {
                action = DownloadService.ACTION_START_DOWNLOAD
                putExtra(DownloadService.EXTRA_DOWNLOAD_ID, download.id)
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun deleteDownload(download: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(download)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSharedUrl() {
        _sharedUrl.value = null
    }

    private fun checkYTDLPStatus() {
        viewModelScope.launch {
            val installed = ytdlpManager.isYTDLPInstalled()
            val version = if (installed) ytdlpManager.getCurrentVersion() else null
            
            _uiState.value = _uiState.value.copy(
                ytDlpInstalled = installed,
                ytDlpVersion = version
            )
        }
    }

    data class MainUiState(
        val url: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val showVideoInfo: Boolean = false,
        val showUrlInput: Boolean = true,
        val downloadStarted: Boolean = false,
        val ytDlpInstalled: Boolean = false,
        val ytDlpVersion: String? = null
    )

    enum class DownloadFormat {
        VIDEO, AUDIO, BOTH
    }
}
