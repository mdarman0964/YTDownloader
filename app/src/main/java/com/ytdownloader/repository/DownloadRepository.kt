package com.ytdownloader.repository

import android.content.Context
import com.ytdownloader.data.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

class DownloadRepository private constructor(context: Context) {

    private val downloadDao: DownloadDao = AppDatabase.getDatabase(context).downloadDao()

    companion object {
        @Volatile
        private var INSTANCE: DownloadRepository? = null

        fun getInstance(context: Context): DownloadRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DownloadRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }

    fun getAllDownloads(): Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>> = 
        downloadDao.getDownloadsByStatus(status)

    suspend fun getDownloadById(id: Long): DownloadItem? = downloadDao.getDownloadById(id)

    suspend fun getDownloadByUrl(url: String): DownloadItem? = downloadDao.getDownloadByUrl(url)

    suspend fun insertDownload(download: DownloadItem): Long = downloadDao.insertDownload(download)

    suspend fun updateDownload(download: DownloadItem) = downloadDao.updateDownload(download)

    suspend fun deleteDownload(download: DownloadItem) = downloadDao.deleteDownload(download)

    suspend fun deleteDownloadById(id: Long) = downloadDao.deleteDownloadById(id)

    suspend fun deleteAllDownloads() = downloadDao.deleteAllDownloads()

    suspend fun updateStatus(id: Long, status: DownloadStatus) = 
        downloadDao.updateStatus(id, status)

    suspend fun updateProgress(id: Long, progress: Int) = 
        downloadDao.updateProgress(id, progress)

    suspend fun updateCompletedDownload(id: Long, filePath: String) = 
        downloadDao.updateCompletedDownload(id, filePath, DownloadStatus.COMPLETED, System.currentTimeMillis())

    suspend fun createDownload(
        url: String,
        title: String,
        thumbnailUrl: String? = null,
        uploader: String? = null,
        duration: String? = null,
        quality: String,
        format: String
    ): Long {
        val download = DownloadItem(
            url = url,
            title = title,
            thumbnailUrl = thumbnailUrl,
            uploader = uploader,
            duration = duration,
            quality = quality,
            format = format,
            status = DownloadStatus.PENDING
        )
        return downloadDao.insertDownload(download)
    }
}
