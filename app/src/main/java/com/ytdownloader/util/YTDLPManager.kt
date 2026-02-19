package com.ytdownloader.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.ytdownloader.data.FormatInfo
import com.ytdownloader.data.QualityOption
import com.ytdownloader.data.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL

class YTDLPManager(private val context: Context) {

    companion object {
        private const val TAG = "YTDLPManager"
        private const val YTDLP_VERSION_URL = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
        private const val YTDLP_DOWNLOAD_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
        
        // Supported sites
        val SUPPORTED_SITES = listOf(
            "youtube.com", "youtu.be", "m.youtube.com",
            "www.youtube.com", "music.youtube.com"
        )
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val appDir: File get() = context.filesDir
    private val ytdlpDir: File get() = File(appDir, "yt-dlp").apply { mkdirs() }
    val ytdlpExecutable: File get() = File(ytdlpDir, "yt-dlp")

    init {
        // Ensure yt-dlp is available
        if (!ytdlpExecutable.exists()) {
            // Will be downloaded on first use
        }
    }

    /**
     * Check if yt-dlp is installed
     */
    fun isYTDLPInstalled(): Boolean {
        return ytdlpExecutable.exists() && ytdlpExecutable.canExecute()
    }

    /**
     * Get current yt-dlp version
     */
    suspend fun getCurrentVersion(): String? = withContext(Dispatchers.IO) {
        try {
            if (!isYTDLPInstalled()) return@withContext null
            
            val process = ProcessBuilder(ytdlpExecutable.absolutePath, "--version")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version: ${e.message}")
            null
        }
    }

    /**
     * Get latest yt-dlp version from GitHub
     */
    suspend fun getLatestVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(YTDLP_VERSION_URL)
            val connection = url.openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val response = connection.getInputStream().bufferedReader().readText()
            val jsonElement = json.parseToJsonElement(response)
            jsonElement.jsonObject["tag_name"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.e(TAG, "Error checking latest version: ${e.message}")
            null
        }
    }

    /**
     * Download and install latest yt-dlp
     */
    suspend fun updateYTDLP(progress: (Int) -> Unit): Result<String> = withContext(Dispatchers.IO) {
        try {
            progress(10)
            
            // Download yt-dlp
            val url = URL(YTDLP_DOWNLOAD_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            progress(30)
            
            val contentLength = connection.contentLength
            val input = connection.getInputStream()
            
            // Write to temporary file first
            val tempFile = File(ytdlpDir, "yt-dlp.tmp")
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    if (contentLength > 0) {
                        val progressPercent = 30 + (totalBytesRead * 60 / contentLength)
                        progress(progressPercent.coerceIn(30, 90))
                    }
                }
            }
            
            progress(90)
            
            // Make executable and replace old version
            ytdlpExecutable.delete()
            tempFile.renameTo(ytdlpExecutable)
            ytdlpExecutable.setExecutable(true)
            
            progress(100)
            
            val version = getCurrentVersion()
            Result.success(version ?: "Updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating yt-dlp: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetch video information
     */
    suspend fun fetchVideoInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            if (!isYTDLPInstalled()) {
                // Try to download yt-dlp first
                updateYTDLP { }.getOrThrow()
            }

            val process = ProcessBuilder(
                ytdlpExecutable.absolutePath,
                "--dump-json",
                "--no-playlist",
                "--skip-download",
                url
            )
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line)
                }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                return@withContext Result.failure(Exception("Failed to fetch video info: $output"))
            }

            val videoData = json.parseToJsonElement(output.toString()).jsonObject

            // Parse formats
            val formats = mutableListOf<FormatInfo>()
            videoData["formats"]?.let { formatsJson ->
                formatsJson.toString().let { formatsStr ->
                    try {
                        val formatsList = json.decodeFromString<List<Map<String, Any?>>>(formatsStr)
                        formatsList.forEach { format ->
                            formats.add(parseFormatInfo(format))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing formats: ${e.message}")
                    }
                }
            }

            // Parse thumbnails
            val thumbnails = mutableListOf<com.ytdownloader.data.ThumbnailInfo>()
            videoData["thumbnails"]?.let { thumbsJson ->
                try {
                    val thumbsList = json.decodeFromString<List<Map<String, Any?>>>(thumbsJson.toString())
                    thumbsList.forEach { thumb ->
                        thumbnails.add(
                            com.ytdownloader.data.ThumbnailInfo(
                                url = thumb["url"] as? String ?: "",
                                preference = (thumb["preference"] as? Number)?.toInt(),
                                height = (thumb["height"] as? Number)?.toInt(),
                                width = (thumb["width"] as? Number)?.toInt()
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing thumbnails: ${e.message}")
                }
            }

            val videoInfo = VideoInfo(
                id = videoData["id"]?.jsonPrimitive?.content ?: "",
                title = videoData["title"]?.jsonPrimitive?.content ?: "Unknown Title",
                description = videoData["description"]?.jsonPrimitive?.content,
                thumbnail = videoData["thumbnail"]?.jsonPrimitive?.content
                    ?: thumbnails.maxByOrNull { it.preference ?: 0 }?.url,
                uploader = videoData["uploader"]?.jsonPrimitive?.content,
                duration = (videoData["duration"]?.jsonPrimitive?.content?.toFloatOrNull()?.toInt()),
                durationString = videoData["duration_string"]?.jsonPrimitive?.content,
                viewCount = videoData["view_count"]?.jsonPrimitive?.content?.toLongOrNull(),
                uploadDate = videoData["upload_date"]?.jsonPrimitive?.content,
                webpageUrl = videoData["webpage_url"]?.jsonPrimitive?.content ?: url,
                formats = formats.sortedByDescending { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0 },
                thumbnails = thumbnails
            )

            Result.success(videoInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching video info: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get quality options from video info
     */
    fun getQualityOptions(videoInfo: VideoInfo): Pair<List<QualityOption>, List<QualityOption>> {
        val videoFormats = videoInfo.formats.filter { it.hasVideo && !it.isAudioOnly }
            .groupBy { it.resolution }
            .map { (_, formats) -> formats.first() }
            .sortedByDescending { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0 }

        val audioFormats = videoInfo.formats.filter { it.hasAudio && !it.hasVideo }
            .groupBy { it.abr?.toInt() }
            .map { (_, formats) -> formats.first() }
            .sortedByDescending { it.abr?.toInt() ?: 0 }

        val videoOptions = videoFormats.map { format ->
            QualityOption(
                formatId = format.formatId,
                displayName = "${format.resolution ?: "Unknown"} (${format.ext})",
                resolution = format.resolution,
                isAudio = false
            )
        }

        val audioOptions = audioFormats.map { format ->
            QualityOption(
                formatId = format.formatId,
                displayName = "${format.abr?.toInt() ?: "Unknown"} kbps (${format.ext})",
                bitrate = "${format.abr?.toInt()} kbps",
                isAudio = true
            )
        }

        return Pair(videoOptions, audioOptions)
    }

    /**
     * Build download command
     */
    fun buildDownloadCommand(
        url: String,
        outputPath: String,
        videoFormatId: String? = null,
        audioFormatId: String? = null,
        audioOnly: Boolean = false
    ): List<String> {
        val cmd = mutableListOf(ytdlpExecutable.absolutePath)

        // Output template
        cmd.add("-o")
        cmd.add(outputPath)

        // Format selection
        when {
            audioOnly && audioFormatId != null -> {
                cmd.add("-f")
                cmd.add(audioFormatId)
            }
            videoFormatId != null && audioFormatId != null -> {
                cmd.add("-f")
                cmd.add("${videoFormatId}+${audioFormatId}")
            }
            videoFormatId != null -> {
                cmd.add("-f")
                cmd.add(videoFormatId)
            }
            audioOnly -> {
                cmd.add("-f")
                cmd.add("bestaudio")
            }
            else -> {
                cmd.add("-f")
                cmd.add("best")
            }
        }

        // Additional options
        cmd.add("--no-playlist")
        cmd.add("--newline")
        cmd.add("--progress")
        
        if (audioOnly) {
            cmd.add("-x")
            cmd.add("--audio-format")
            cmd.add("mp3")
        }

        cmd.add(url)
        return cmd
    }

    private fun parseFormatInfo(format: Map<String, Any?>): FormatInfo {
        val vcodec = format["vcodec"] as? String
        val acodec = format["acodec"] as? String
        
        return FormatInfo(
            formatId = format["format_id"] as? String ?: "",
            ext = format["ext"] as? String ?: "mp4",
            quality = format["quality"] as? String,
            resolution = format["resolution"] as? String,
            fps = (format["fps"] as? Number)?.toFloat(),
            filesize = (format["filesize"] as? Number)?.toLong()
                ?: (format["filesize_approx"] as? Number)?.toLong(),
            filesizeApprox = (format["filesize_approx"] as? Number)?.toLong(),
            vcodec = vcodec,
            acodec = acodec,
            abr = (format["abr"] as? Number)?.toFloat(),
            vbr = (format["vbr"] as? Number)?.toFloat(),
            asr = (format["asr"] as? Number)?.toInt(),
            audioChannels = (format["audio_channels"] as? Number)?.toInt(),
            formatNote = format["format_note"] as? String,
            hasVideo = vcodec != null && vcodec != "none",
            hasAudio = acodec != null && acodec != "none"
        )
    }

    /**
     * Check if URL is supported
     */
    fun isSupportedUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return SUPPORTED_SITES.any { lowerUrl.contains(it) }
    }
}
