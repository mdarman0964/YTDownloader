package com.ytdownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ytdownloader.data.converter.DateConverter
import java.util.Date

@Entity(tableName = "downloads")
@TypeConverters(DateConverter::class)
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val uploader: String? = null,
    val duration: String? = null,
    val filePath: String? = null,
    val fileSize: Long = 0,
    val quality: String,
    val format: String, // "video", "audio", "both"
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Date = Date(),
    val completedAt: Date? = null
)

enum class DownloadStatus {
    PENDING,
    FETCHING_INFO,
    QUEUED,
    DOWNLOADING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
