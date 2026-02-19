package com.ytdownloader.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ytdownloader.MainActivity
import com.ytdownloader.R
import com.ytdownloader.data.DownloadItem
import com.ytdownloader.data.DownloadStatus
import com.ytdownloader.repository.DownloadRepository
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern

class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"
        private const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1
        
        const val ACTION_START_DOWNLOAD = "action_start_download"
        const val ACTION_CANCEL_DOWNLOAD = "action_cancel_download"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentDownloadJob: Job? = null
    private lateinit var repository: DownloadRepository
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onCreate() {
        super.onCreate()
        repository = DownloadRepository.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    startDownload(downloadId)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownload()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(downloadId: Long) {
        currentDownloadJob?.cancel()
        
        currentDownloadJob = serviceScope.launch {
            try {
                val download = repository.getDownloadById(downloadId) ?: return@launch
                
                startForeground(NOTIFICATION_ID, createNotification("Starting download…", 0))
                
                repository.updateStatus(downloadId, DownloadStatus.DOWNLOADING)
                
                val outputDir = File(getExternalFilesDir(null), "downloads").apply { mkdirs() }
                val safeTitle = download.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val outputPath = File(outputDir, "$safeTitle.%(ext)s").absolutePath
                
                val cmd = buildDownloadCommand(download, outputPath)
                
                val process = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start()
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    if (!isActive) {
                        process.destroy()
                        repository.updateStatus(downloadId, DownloadStatus.CANCELLED)
                        return@launch
                    }
                    
                    line?.let { parseProgress(it, downloadId) }
                }
                
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    // Find downloaded file
                    val downloadedFile = outputDir.listFiles()?.find { 
                        it.name.contains(safeTitle) 
                    }
                    
                    repository.updateCompletedDownload(
                        downloadId,
                        downloadedFile?.absolutePath ?: outputPath
                    )
                    
                    updateNotification("Download complete: ${download.title}", 100, true)
                } else {
                    repository.updateStatus(downloadId, DownloadStatus.FAILED)
                    updateNotification("Download failed: ${download.title}", 0, true)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Download error: ${e.message}")
                repository.updateStatus(downloadId, DownloadStatus.FAILED)
                updateNotification("Download failed", 0, true)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun buildDownloadCommand(download: DownloadItem, outputPath: String): List<String> {
        val cmd = mutableListOf<String>()
        
        // Find yt-dlp executable
        val ytdlpFile = File(filesDir, "yt-dlp/yt-dlp")
        if (ytdlpFile.exists()) {
            cmd.add(ytdlpFile.absolutePath)
        } else {
            cmd.add("yt-dlp") // Fallback to system path
        }
        
        cmd.add("-o")
        cmd.add(outputPath)
        
        // Format selection based on quality
        when (download.format) {
            "audio" -> {
                cmd.add("-f")
                cmd.add("bestaudio")
                cmd.add("-x")
                cmd.add("--audio-format")
                cmd.add("mp3")
            }
            "video" -> {
                cmd.add("-f")
                cmd.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
            }
            else -> {
                cmd.add("-f")
                cmd.add("best")
            }
        }
        
        cmd.add("--no-playlist")
        cmd.add("--newline")
        cmd.add("--progress")
        cmd.add(download.url)
        
        return cmd
    }

    private suspend fun parseProgress(line: String, downloadId: Long) {
        // Parse yt-dlp progress output
        // Example: [download]  45.3% of  100.50MiB at  2.50MiB/s ETA 00:30
        val pattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)%")
        val matcher = pattern.matcher(line)
        
        if (matcher.find()) {
            val progress = matcher.group(1)?.toFloatOrNull()?.toInt() ?: 0
            repository.updateProgress(downloadId, progress)
            updateNotification("Downloading…", progress, false)
        }
    }

    private fun cancelDownload() {
        currentDownloadJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download notifications"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String, progress: Int): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("YT Downloader")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(content: String, progress: Int, complete: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("YT Downloader")
            .setContentText(content)
            .setSmallIcon(if (complete) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(!complete)
            .setAutoCancel(complete)
        
        if (!complete) {
            builder.setProgress(100, progress, progress == 0)
        }
        
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
