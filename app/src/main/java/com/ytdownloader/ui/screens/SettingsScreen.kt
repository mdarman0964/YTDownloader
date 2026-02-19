package com.ytdownloader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ytdownloader.R
import com.ytdownloader.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val defaultFormat by viewModel.defaultFormat.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val autoUpdateYTDLP by viewModel.autoUpdateYTDLP.collectAsState()
    val context = LocalContext.current

    // Show messages
    LaunchedEffect(uiState.updateMessage) {
        uiState.updateMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearUpdateMessage()
        }
    }

    LaunchedEffect(uiState.updateError) {
        uiState.updateError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearUpdateError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
            // YT-DLP Section
            SettingsSection(title = stringResource(R.string.yt_dlp_settings)) {
                // Version Info
                ListItem(
                    headlineContent = { Text(stringResource(R.string.yt_dlp_version)) },
                    supportingContent = {
                        when {
                            uiState.isLoadingVersion -> Text(stringResource(R.string.checking_version))
                            uiState.currentVersion != null -> {
                                Column {
                                    Text("Current: ${uiState.currentVersion}")
                                    if (uiState.isUpdateAvailable) {
                                        Text(
                                            text = "Update available: ${uiState.latestVersion}",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.up_to_date),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            else -> Text("Not installed")
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.checkForUpdate() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Check for update")
                        }
                    }
                )

                // Update Button
                if (uiState.isUpdateAvailable || uiState.currentVersion == null) {
                    Button(
                        onClick = { viewModel.updateYTDLP() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isUpdating
                    ) {
                        if (uiState.isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${uiState.updateProgress}%")
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.update_yt_dlp))
                        }
                    }

                    // Progress bar
                    if (uiState.isUpdating) {
                        LinearProgressIndicator(
                            progress = uiState.updateProgress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Auto Update Toggle
                ListItem(
                    headlineContent = { Text("Auto-update YT-DLP") },
                    supportingContent = { Text("Check for updates on app start") },
                    trailingContent = {
                        Switch(
                            checked = autoUpdateYTDLP,
                            onCheckedChange = { viewModel.setAutoUpdateYTDLP(it) }
                        )
                    }
                )
            }

            // Download Settings
            SettingsSection(title = "Download Settings") {
                // Default Quality
                var showQualityDialog by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.default_quality)) },
                    supportingContent = { Text(defaultQuality) },
                    trailingContent = {
                        IconButton(onClick = { showQualityDialog = true }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                )

                if (showQualityDialog) {
                    QualityDialog(
                        currentQuality = defaultQuality,
                        onQualitySelected = {
                            viewModel.setDefaultQuality(it)
                            showQualityDialog = false
                        },
                        onDismiss = { showQualityDialog = false }
                    )
                }

                // Default Format
                var showFormatDialog by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.format)) },
                    supportingContent = { 
                        Text(defaultFormat.replaceFirstChar { it.uppercase() }) 
                    },
                    trailingContent = {
                        IconButton(onClick = { showFormatDialog = true }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                )

                if (showFormatDialog) {
                    FormatDialog(
                        currentFormat = defaultFormat,
                        onFormatSelected = {
                            viewModel.setDefaultFormat(it)
                            showFormatDialog = false
                        },
                        onDismiss = { showFormatDialog = false }
                    )
                }
            }

            // Notifications
            SettingsSection(title = "Notifications") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notifications)) },
                    supportingContent = { Text("Show download notifications") },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                        )
                    }
                )
            }

            // About
            SettingsSection(title = stringResource(R.string.about)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.version)) },
                    supportingContent = { Text("1.0.0") }
                )
                ListItem(
                    headlineContent = { Text("YT-DLP") },
                    supportingContent = { Text("A feature-rich command-line audio/video downloader") }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun QualityDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf("best", "1080p", "720p", "480p", "360p", "audio")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.default_quality)) },
        text = {
            Column {
                qualities.forEach { quality ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentQuality == quality,
                            onClick = { onQualitySelected(quality) }
                        )
                        Text(
                            text = quality.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun FormatDialog(
    currentFormat: String,
    onFormatSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val formats = listOf("video", "audio", "both")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.format)) },
        text = {
            Column {
                formats.forEach { format ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFormat == format,
                            onClick = { onFormatSelected(format) }
                        )
                        Text(
                            text = format.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
