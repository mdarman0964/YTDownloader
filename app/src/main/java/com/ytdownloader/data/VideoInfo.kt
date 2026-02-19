package com.ytdownloader.data

data class VideoInfo(
    val id: String,
    val title: String,
    val description: String? = null,
    val thumbnail: String? = null,
    val uploader: String? = null,
    val duration: Int? = null,
    val durationString: String? = null,
    val viewCount: Long? = null,
    val uploadDate: String? = null,
    val webpageUrl: String,
    val formats: List<FormatInfo> = emptyList(),
    val thumbnails: List<ThumbnailInfo> = emptyList()
)

data class FormatInfo(
    val formatId: String,
    val ext: String,
    val quality: String? = null,
    val resolution: String? = null,
    val fps: Float? = null,
    val filesize: Long? = null,
    val filesizeApprox: Long? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val abr: Float? = null, // Audio bitrate
    val vbr: Float? = null, // Video bitrate
    val asr: Int? = null, // Audio sample rate
    val audioChannels: Int? = null,
    val formatNote: String? = null,
    val hasVideo: Boolean = true,
    val hasAudio: Boolean = true
) {
    val isVideoOnly: Boolean get() = hasVideo && !hasAudio
    val isAudioOnly: Boolean get() = !hasVideo && hasAudio
    val displayQuality: String get() = resolution ?: quality ?: formatNote ?: formatId
}

data class ThumbnailInfo(
    val url: String,
    val preference: Int? = null,
    val height: Int? = null,
    val width: Int? = null
)

// Quality options for UI
data class QualityOption(
    val formatId: String,
    val displayName: String,
    val resolution: String? = null,
    val isAudio: Boolean = false,
    val bitrate: String? = null
)
