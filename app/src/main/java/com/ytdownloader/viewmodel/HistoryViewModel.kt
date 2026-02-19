package com.ytdownloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ytdownloader.data.DownloadItem
import com.ytdownloader.data.DownloadStatus
import com.ytdownloader.repository.DownloadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository.getInstance(application)

    // All downloads
    val allDownloads: StateFlow<List<DownloadItem>> = repository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered downloads
    private val _filter = MutableStateFlow<DownloadFilter>(DownloadFilter.ALL)
    val filter: StateFlow<DownloadFilter> = _filter.asStateFlow()

    val filteredDownloads: StateFlow<List<DownloadItem>> = combine(
        allDownloads,
        filter
    ) { downloads, filter ->
        when (filter) {
            DownloadFilter.ALL -> downloads
            DownloadFilter.COMPLETED -> downloads.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.DOWNLOADING -> downloads.filter { 
                it.status == DownloadStatus.DOWNLOADING || 
                it.status == DownloadStatus.PENDING ||
                it.status == DownloadStatus.QUEUED
            }
            DownloadFilter.FAILED -> downloads.filter { it.status == DownloadStatus.FAILED }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stats
    val downloadStats: StateFlow<DownloadStats> = allDownloads.map { downloads ->
        DownloadStats(
            total = downloads.size,
            completed = downloads.count { it.status == DownloadStatus.COMPLETED },
            failed = downloads.count { it.status == DownloadStatus.FAILED },
            inProgress = downloads.count { 
                it.status == DownloadStatus.DOWNLOADING || 
                it.status == DownloadStatus.PENDING 
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadStats())

    fun setFilter(filter: DownloadFilter) {
        _filter.value = filter
    }

    fun deleteDownload(download: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(download)
        }
    }

    fun deleteDownloadById(id: Long) {
        viewModelScope.launch {
            repository.deleteDownloadById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllDownloads()
        }
    }

    fun retryDownload(download: DownloadItem) {
        viewModelScope.launch {
            repository.updateStatus(download.id, DownloadStatus.PENDING)
            // Start download service
            val intent = android.content.Intent(getApplication(), com.ytdownloader.service.DownloadService::class.java).apply {
                action = com.ytdownloader.service.DownloadService.ACTION_START_DOWNLOAD
                putExtra(com.ytdownloader.service.DownloadService.EXTRA_DOWNLOAD_ID, download.id)
            }
            getApplication<Application>().startService(intent)
        }
    }

    enum class DownloadFilter {
        ALL, COMPLETED, DOWNLOADING, FAILED
    }

    data class DownloadStats(
        val total: Int = 0,
        val completed: Int = 0,
        val failed: Int = 0,
        val inProgress: Int = 0
    )
}
