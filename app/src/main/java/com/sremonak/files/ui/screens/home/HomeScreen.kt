package com.sremonak.files.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sremonak.files.R
import com.sremonak.files.data.model.CategoryStats
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.ui.components.FileListItem
import com.sremonak.files.ui.components.SectionHeader
import com.sremonak.files.ui.components.StorageDonut
import com.sremonak.files.util.FileUtils
import com.sremonak.files.viewmodel.HomeViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenBrowse: (String) -> Unit,
    onOpenCategory: (FileCategory) -> Unit,
    onOpenDetails: (String) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenAnalyzer: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_home)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = greeting(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your files, beautifully organized",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Storage overview
            item {
                val storage = state.storage
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (storage != null) {
                            StorageDonut(
                                usedFraction = storage.usedFraction,
                                usedBytes = storage.usedBytes,
                                totalBytes = storage.totalBytes
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Internal storage", style = MaterialTheme.typography.titleMedium)
                            if (storage != null) {
                                Text(
                                    "${FileUtils.formatBytes(storage.usedBytes)} used • ${FileUtils.formatBytes(storage.availableBytes)} free",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (state.scanning) {
                                Text(
                                    "Scanning… ${FileUtils.humanCount(state.scannedCount)} files",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    "${FileUtils.humanCount(state.totalFiles)} files • ${FileUtils.formatBytes(state.totalSize)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Categories
            item {
                SectionHeader(stringResource(R.string.categories_title))
                val categories = listOf(
                    FileCategory.IMAGE, FileCategory.VIDEO, FileCategory.AUDIO,
                    FileCategory.DOCUMENT, FileCategory.PDF, FileCategory.APK,
                    FileCategory.ARCHIVE, FileCategory.CODE
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            stats = state.categories[category],
                            onClick = { onOpenCategory(category) }
                        )
                    }
                }
            }

            // Quick actions
            item {
                SectionHeader(stringResource(R.string.quick_actions_title))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAction(
                        icon = Icons.Filled.Download,
                        label = "Downloads",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenBrowse(com.sremonak.files.FilesApp.instance.files.downloadsDir().absolutePath) }
                    )
                    QuickAction(
                        icon = Icons.Filled.Delete,
                        label = stringResource(R.string.trash_title),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTrash
                    )
                    QuickAction(
                        icon = Icons.Filled.PieChart,
                        label = stringResource(R.string.analyzer_title),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAnalyzer
                    )
                    QuickAction(
                        icon = Icons.Filled.FolderOpen,
                        label = stringResource(R.string.nav_browse),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenBrowse("") }
                    )
                }
            }

            // Recent files
            if (state.recents.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.recent_title)) }
                items(state.recents.take(5)) { info ->
                    FileListItem(
                        info = info,
                        selected = false,
                        selectionMode = false,
                        onClick = { onOpenDetails(info.path) },
                        onLongClick = { onOpenDetails(info.path) }
                    )
                }
            }

            // Favorites
            if (state.favorites.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.favorites_title)) }
                items(state.favorites.take(5)) { info ->
                    FileListItem(
                        info = info,
                        selected = false,
                        selectionMode = false,
                        onClick = { onOpenDetails(info.path) },
                        onLongClick = { onOpenDetails(info.path) }
                    )
                }
            }
        }
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun CategoryCard(
    category: FileCategory,
    stats: CategoryStats?,
    onClick: () -> Unit
) {
    val (icon, label) = when (category) {
        FileCategory.IMAGE -> Icons.Filled.Image to "Images"
        FileCategory.VIDEO -> Icons.Filled.Videocam to "Videos"
        FileCategory.AUDIO -> Icons.Filled.Audiotrack to "Audio"
        FileCategory.DOCUMENT -> Icons.Filled.Description to "Documents"
        FileCategory.PDF -> Icons.Filled.PictureAsPdf to "PDFs"
        FileCategory.APK -> Icons.Filled.Android to "APKs"
        FileCategory.ARCHIVE -> Icons.Filled.Archive to "Archives"
        FileCategory.CODE -> Icons.Filled.Code to "Code"
        FileCategory.FONT -> Icons.Filled.Description to "Fonts"
        FileCategory.OTHER -> Icons.Filled.Description to "Other"
    }
    Card(
        modifier = Modifier
            .width(132.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                "${FileUtils.humanCount(stats?.count ?: 0)} files",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                FileUtils.formatBytes(stats?.totalSize ?: 0),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
