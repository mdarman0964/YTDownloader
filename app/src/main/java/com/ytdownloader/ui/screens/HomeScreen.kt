package com.ytdownloader.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytdownloader.R
import com.ytdownloader.data.VideoInfo
import com.ytdownloader.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val videoInfo by viewModel.videoInfo.collectAsState()
    val videoQualities by viewModel.videoQualities.collectAsState()
    val audioQualities by viewModel.audioQualities.collectAsState()
    val selectedVideoQuality by viewModel.selectedVideoQuality.collectAsState()
    val selectedAudioQuality by viewModel.selectedAudioQuality.collectAsState()
    val downloadFormat by viewModel.downloadFormat.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Show toast when download starts
    LaunchedEffect(uiState.downloadStarted) {
        if (uiState.downloadStarted) {
            Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
        }
    }

    // Show error toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL Input Section
            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text(stringResource(R.string.enter_url)) },
                placeholder = { Text("https://youtube.com/watch?v=...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.fetchVideoInfo(uiState.url) }
                ),
                trailingIcon = {
                    Row {
                        // Paste button
                        IconButton(onClick = {
                            clipboardManager.getText()?.let {
                                viewModel.onUrlChange(it.text)
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                        // Clear button
                        if (uiState.url.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onUrlChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                }
            )

            // Fetch Info Button
            Button(
                onClick = { viewModel.fetchVideoInfo(uiState.url) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.url.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.fetch_info))
            }

            // Video Info Card
            AnimatedVisibility(
                visible = uiState.showVideoInfo && videoInfo != null,
                enter = fadeIn() + expandVertically()
            ) {
                videoInfo?.let { info ->
                    VideoInfoCard(
                        videoInfo = info,
                        videoQualities = videoQualities,
                        audioQualities = audioQualities,
                        selectedVideoQuality = selectedVideoQuality,
                        selectedAudioQuality = selectedAudioQuality,
                        downloadFormat = downloadFormat,
                        onVideoQualitySelected = viewModel::onVideoQualitySelected,
                        onAudioQualitySelected = viewModel::onAudioQualitySelected,
                        onFormatChange = viewModel::onFormatChange,
                        onDownload = viewModel::startDownload
                    )
                }
            }

            // Empty state
            if (!uiState.isLoading && !uiState.showVideoInfo && uiState.url.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Paste a YouTube URL to start",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoCard(
    videoInfo: VideoInfo,
    videoQualities: List<com.ytdownloader.data.QualityOption>,
    audioQualities: List<com.ytdownloader.data.QualityOption>,
    selectedVideoQuality: com.ytdownloader.data.QualityOption?,
    selectedAudioQuality: com.ytdownloader.data.QualityOption?,
    downloadFormat: MainViewModel.DownloadFormat,
    onVideoQualitySelected: (com.ytdownloader.data.QualityOption) -> Unit,
    onAudioQualitySelected: (com.ytdownloader.data.QualityOption) -> Unit,
    onFormatChange: (MainViewModel.DownloadFormat) -> Unit,
    onDownload: () -> Unit
) {
    var showQualityDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            videoInfo.thumbnail?.let { thumbnailUrl ->
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            // Title
            Text(
                text = videoInfo.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )

            // Info row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                videoInfo.uploader?.let {
                    InfoChip(Icons.Default.Person, it)
                }
                videoInfo.durationString?.let {
                    InfoChip(Icons.Default.Timer, it)
                }
            }

            Divider()

            // Format Selection
            Text(
                text = stringResource(R.string.format),
                style = MaterialTheme.typography.labelMedium
            )

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = downloadFormat == MainViewModel.DownloadFormat.VIDEO,
                    onClick = { onFormatChange(MainViewModel.DownloadFormat.VIDEO) },
                    shape = SegmentedButtonDefaults.itemShape(0, 3)
                ) {
                    Text("Video")
                }
                SegmentedButton(
                    selected = downloadFormat == MainViewModel.DownloadFormat.AUDIO,
                    onClick = { onFormatChange(MainViewModel.DownloadFormat.AUDIO) },
                    shape = SegmentedButtonDefaults.itemShape(1, 3)
                ) {
                    Text("Audio")
                }
                SegmentedButton(
                    selected = downloadFormat == MainViewModel.DownloadFormat.BOTH,
                    onClick = { onFormatChange(MainViewModel.DownloadFormat.BOTH) },
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) {
                    Text("Both")
                }
            }

            // Quality Selection Button
            OutlinedButton(
                onClick = { showQualityDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.select_quality))
            }

            // Selected quality display
            when (downloadFormat) {
                MainViewModel.DownloadFormat.VIDEO -> {
                    selectedVideoQuality?.let {
                        Text(
                            text = "Video: ${it.displayName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                MainViewModel.DownloadFormat.AUDIO -> {
                    selectedAudioQuality?.let {
                        Text(
                            text = "Audio: ${it.displayName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                MainViewModel.DownloadFormat.BOTH -> {
                    selectedVideoQuality?.let {
                        Text(
                            text = "Video: ${it.displayName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    selectedAudioQuality?.let {
                        Text(
                            text = "Audio: ${it.displayName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Download Button
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.download))
            }
        }
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        QualitySelectionDialog(
            videoQualities = videoQualities,
            audioQualities = audioQualities,
            selectedVideoQuality = selectedVideoQuality,
            selectedAudioQuality = selectedAudioQuality,
            downloadFormat = downloadFormat,
            onVideoQualitySelected = onVideoQualitySelected,
            onAudioQualitySelected = onAudioQualitySelected,
            onDismiss = { showQualityDialog = false }
        )
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun QualitySelectionDialog(
    videoQualities: List<com.ytdownloader.data.QualityOption>,
    audioQualities: List<com.ytdownloader.data.QualityOption>,
    selectedVideoQuality: com.ytdownloader.data.QualityOption?,
    selectedAudioQuality: com.ytdownloader.data.QualityOption?,
    downloadFormat: MainViewModel.DownloadFormat,
    onVideoQualitySelected: (com.ytdownloader.data.QualityOption) -> Unit,
    onAudioQualitySelected: (com.ytdownloader.data.QualityOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_quality)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Quality
                if (downloadFormat != MainViewModel.DownloadFormat.AUDIO) {
                    Text(
                        text = stringResource(R.string.video_quality),
                        style = MaterialTheme.typography.labelMedium
                    )
                    videoQualities.take(5).forEach { quality ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVideoQuality?.formatId == quality.formatId,
                                onClick = { onVideoQualitySelected(quality) }
                            )
                            Text(
                                text = quality.displayName,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                // Audio Quality
                if (downloadFormat != MainViewModel.DownloadFormat.VIDEO) {
                    Text(
                        text = stringResource(R.string.audio_quality),
                        style = MaterialTheme.typography.labelMedium
                    )
                    audioQualities.take(5).forEach { quality ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAudioQuality?.formatId == quality.formatId,
                                onClick = { onAudioQualitySelected(quality) }
                            )
                            Text(
                                text = quality.displayName,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
