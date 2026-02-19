package com.ytdownloader.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ytdownloader.R
import com.ytdownloader.data.DownloadItem
import com.ytdownloader.data.DownloadStatus
import com.ytdownloader.viewmodel.HistoryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val downloads by viewModel.filteredDownloads.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val stats by viewModel.downloadStats.collectAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.download_history)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.clear_history),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Card
            StatsCard(stats = stats)

            // Filter Chips
            FilterChips(
                selectedFilter = filter,
                onFilterSelected = viewModel::setFilter
            )

            // Downloads List
            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.no_history),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(downloads, key = { it.id }) { download ->
                        DownloadItemCard(
                            download = download,
                            onShare = { shareDownload(context, download) },
                            onOpen = { openDownload(context, download) },
                            onRetry = { viewModel.retryDownload(download) },
                            onDelete = { viewModel.deleteDownload(download) }
                        )
                    }
                }
            }
        }
    }

    // Clear History Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.confirm_clear_history)) },
            text = { Text(stringResource(R.string.action_cannot_be_undone)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

@Composable
fun StatsCard(stats: HistoryViewModel.DownloadStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Total", stats.total)
            StatItem("Completed", stats.completed, MaterialTheme.colorScheme.primary)
            StatItem("Failed", stats.failed, MaterialTheme.colorScheme.error)
            StatItem("In Progress", stats.inProgress, MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun StatItem(label: String, value: Int, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    selectedFilter: HistoryViewModel.DownloadFilter,
    onFilterSelected: (HistoryViewModel.DownloadFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == HistoryViewModel.DownloadFilter.ALL,
            onClick = { onFilterSelected(HistoryViewModel.DownloadFilter.ALL) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selectedFilter == HistoryViewModel.DownloadFilter.COMPLETED,
            onClick = { onFilterSelected(HistoryViewModel.DownloadFilter.COMPLETED) },
            label = { Text("Completed") }
        )
        FilterChip(
            selected = selectedFilter == HistoryViewModel.DownloadFilter.DOWNLOADING,
            onClick = { onFilterSelected(HistoryViewModel.DownloadFilter.DOWNLOADING) },
            label = { Text("Active") }
        )
        FilterChip(
            selected = selectedFilter == HistoryViewModel.DownloadFilter.FAILED,
            onClick = { onFilterSelected(HistoryViewModel.DownloadFilter.FAILED) },
            label = { Text("Failed") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadItemCard(
    download: DownloadItem,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = download.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 12.dp)
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = download.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Status
                StatusChip(status = download.status)

                // Progress bar for downloading
                if (download.status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        progress = download.progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${download.progress}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Date
                Text(
                    text = dateFormat.format(download.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Actions
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (download.status == DownloadStatus.COMPLETED) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.open)) },
                            onClick = {
                                onOpen()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share)) },
                            onClick = {
                                onShare()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                    }
                    if (download.status == DownloadStatus.FAILED) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.retry)) },
                            onClick = {
                                onRetry()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.outline
        DownloadStatus.FETCHING_INFO -> "Fetching Info" to MaterialTheme.colorScheme.primary
        DownloadStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.tertiary
        DownloadStatus.DOWNLOADING -> "Downloading" to MaterialTheme.colorScheme.primary
        DownloadStatus.PROCESSING -> "Processing" to MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun shareDownload(context: android.content.Context, download: DownloadItem) {
    download.filePath?.let { path ->
        val file = File(path)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = context.contentResolver.getType(uri) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        } else {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openDownload(context: android.content.Context, download: DownloadItem) {
    download.filePath?.let { path ->
        val file = File(path)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
        }
    }
}
