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
import com.ytdownloader.util.YTDLPManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository.getInstance(application)
    private val ytdlpManager = YTDLPManager(application)

    /* ---------------- UI STATE ---------------- */

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /* ---------------- VIDEO INFO ---------------- */

    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    private val _videoQualities = MutableStateFlow<List<QualityOption>>(emptyList())
    val videoQualities: StateFlow<List<QualityOption>> = _videoQualities.asStateFlow()

    private val _audioQualities = MutableStateFlow<List<QualityOption>>(emptyList())
    val audioQualities: StateFlow<List<QualityOption>> = _audioQualities.asStateFlow()

    private val _selectedVideoQuality = MutableStateFlow<QualityOption?>(null)
    val selectedVideoQuality: StateFlow<QualityOption?> = _selectedVideoQuality.asStateFlow()

    private val _selectedAudioQuality = MutableStateFlow<QualityOption?>(null)
    val selectedAudioQuality: StateFlow<QualityOption?> = _selectedAudioQuality.asStateFlow()

    private val _downloadFormat = MutableStateFlow(DownloadFormat.VIDEO)
    val downloadFormat: StateFlow<DownloadFormat> = _downloadFormat.asStateFlow()

    /* ---------------- DOWNLOAD LIST ---------------- */

    val allDownloads: StateFlow<List<DownloadItem>> =
        repository.getAllDownloads()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /* ---------------- HANDLE SHARE / INTENT ---------------- */

    fun handleIntent(intent: Intent?) {
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(url = sharedText)
        }
    }

    /* ---------------- URL ---------------- */

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(url = url)
    }

    /* ---------------- FETCH VIDEO INFO ---------------- */

    fun fetchVideoInfo(url: String) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter a valid URL")
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

                    val (videos, audios) = ytdlpManager.getQualityOptions(info)
                    _videoQualities.value = videos
                    _audioQualities.value = audios

                    _selectedVideoQuality.value = videos.firstOrNull()
                    _selectedAudioQuality.value = audios.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showVideoInfo = true
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Failed to load video info"
                    )
                }
        }
    }

    /* ---------------- FORMAT / QUALITY ---------------- */

    fun onVideoQualitySelected(option: QualityOption) {
        _selectedVideoQuality.value = option
    }

    fun onAudioQualitySelected(option: QualityOption) {
        _selectedAudioQuality.value = option
    }

    fun onFormatChange(format: DownloadFormat) {
        _downloadFormat.value = format
    }

    /* ---------------- START DOWNLOAD ---------------- */

    fun startDownload() {
        val info = _videoInfo.value ?: return
        val url = _uiState.value.url

        viewModelScope.launch {
            val quality = when (_downloadFormat.value) {
                DownloadFormat.VIDEO ->
                    _selectedVideoQuality.value?.displayName ?: "best"
                DownloadFormat.AUDIO ->
                    _selectedAudioQuality.value?.displayName ?: "best"
                DownloadFormat.BOTH -> "best"
            }

            val format = _downloadFormat.value.name.lowercase()

            val downloadId = repository.createDownload(
                url = url,
                title = info.title,
                thumbnailUrl = info.thumbnail,
                uploader = info.uploader,
                duration = info.durationString,
                quality = quality,
                format = format
            )

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

            delay(1500)
            _uiState.value = _uiState.value.copy(downloadStarted = false)
        }
    }

    /* ---------------- DOWNLOAD ACTIONS ---------------- */

    fun retryDownload(item: DownloadItem) {
        viewModelScope.launch {
            repository.updateStatus(item.id, DownloadStatus.PENDING)
            val intent = Intent(getApplication(), DownloadService::class.java).apply {
                action = DownloadService.ACTION_START_DOWNLOAD
                putExtra(DownloadService.EXTRA_DOWNLOAD_ID, item.id)
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun deleteDownload(item: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(item)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /* ---------------- MODELS ---------------- */

    data class MainUiState(
        val url: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val showVideoInfo: Boolean = false,
        val downloadStarted: Boolean = false
    )

    enum class DownloadFormat {
        VIDEO, AUDIO, BOTH
    }
}
