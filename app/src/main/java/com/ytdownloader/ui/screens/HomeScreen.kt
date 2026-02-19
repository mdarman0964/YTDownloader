package com.ytdownloader.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytdownloader.R
import com.ytdownloader.data.VideoInfo
import com.ytdownloader.data.QualityOption
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

    LaunchedEffect(uiState.downloadStarted) {
        if (uiState.downloadStarted) {
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YTDownloader") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("YouTube URL") },
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
                        IconButton(onClick = {
                            clipboardManager.getText()?.let {
                                viewModel.onUrlChange(it.text)
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, null)
                        }
                        if (uiState.url.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onUrlChange("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    }
                }
            )

            Button(
                onClick = { viewModel.fetchVideoInfo(uiState.url) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.url.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Fetch Info")
            }

            if (videoInfo != null) {
                VideoInfoCard(
                    videoInfo = videoInfo!!,
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
    }
}

@Composable
fun VideoInfoCard(
    videoInfo: VideoInfo,
    videoQualities: List<QualityOption>,
    audioQualities: List<QualityOption>,
    selectedVideoQuality: QualityOption?,
    selectedAudioQuality: QualityOption?,
    downloadFormat: MainViewModel.DownloadFormat,
    onVideoQualitySelected: (QualityOption) -> Unit,
    onAudioQualitySelected: (QualityOption) -> Unit,
    onFormatChange: (MainViewModel.DownloadFormat) -> Unit,
    onDownload: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            videoInfo.thumbnail?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            Text(videoInfo.title, style = MaterialTheme.typography.titleMedium)

            Divider()

            Text("Format")

            Column {
                FormatRadio("Video", downloadFormat == MainViewModel.DownloadFormat.VIDEO) {
                    onFormatChange(MainViewModel.DownloadFormat.VIDEO)
                }
                FormatRadio("Audio", downloadFormat == MainViewModel.DownloadFormat.AUDIO) {
                    onFormatChange(MainViewModel.DownloadFormat.AUDIO)
                }
                FormatRadio("Both", downloadFormat == MainViewModel.DownloadFormat.BOTH) {
                    onFormatChange(MainViewModel.DownloadFormat.BOTH)
                }
            }

            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Quality")
            }

            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Quality") },
            text = {
                Column {
                    if (downloadFormat != MainViewModel.DownloadFormat.AUDIO) {
                        videoQualities.forEach {
                            QualityRadio(it, selectedVideoQuality == it) {
                                onVideoQualitySelected(it)
                            }
                        }
                    }
                    if (downloadFormat != MainViewModel.DownloadFormat.VIDEO) {
                        audioQualities.forEach {
                            QualityRadio(it, selectedAudioQuality == it) {
                                onAudioQualitySelected(it)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun FormatRadio(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}

@Composable
fun QualityRadio(option: QualityOption, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(option.displayName)
    }
}
