package com.ytdownloader.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadItem?

    @Query("SELECT * FROM downloads WHERE url = :url ORDER BY createdAt DESC LIMIT 1")
    suspend fun getDownloadByUrl(url: String): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem): Long

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Delete
    suspend fun deleteDownload(download: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun deleteAllDownloads()

    @Query("SELECT COUNT(*) FROM downloads WHERE status = :status")
    suspend fun getDownloadCountByStatus(status: DownloadStatus): Int

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus)

    @Query("UPDATE downloads SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int)

    @Query("UPDATE downloads SET filePath = :filePath, status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateCompletedDownload(
        id: Long,
        filePath: String,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        completedAt: Long = System.currentTimeMillis()
    )
}
