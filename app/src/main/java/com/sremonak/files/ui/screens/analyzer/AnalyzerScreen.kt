package com.sremonak.files.ui.screens.analyzer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sremonak.files.R
import com.sremonak.files.data.model.DuplicateGroup
import com.sremonak.files.ui.components.EmptyState
import com.sremonak.files.ui.components.ConfirmDialog
import com.sremonak.files.ui.components.SectionHeader
import com.sremonak.files.util.FileUtils
import com.sremonak.files.viewmodel.AnalyzerViewModel
import com.sremonak.files.viewmodel.ScanPhase
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzerScreen(
    onOpenDetails: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AnalyzerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var keepMap by remember { mutableStateOf(mapOf<String, String>()) }
    var pendingGroup by remember { mutableStateOf<Pair<DuplicateGroup, File>?>(null) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    pendingGroup?.let { (group, keep) ->
        ConfirmDialog(title = "Move duplicates to trash?",
            message = "Keep ${keep.name} and move the other verified copies to trash?",
            confirmText = "Move to trash", onConfirm = { viewModel.deleteDuplicates(group, keep); pendingGroup = null },
            onDismiss = { pendingGroup = null })
    }
    pendingFile?.let { file ->
        ConfirmDialog(title = "Move to trash?", message = "Move ${file.name} to trash?",
            confirmText = "Move to trash", onConfirm = { viewModel.deleteFile(file); pendingFile = null },
            onDismiss = { pendingFile = null })
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analyzer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (state.phase) {
            ScanPhase.IDLE -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        icon = Icons.Filled.PieChart,
                        title = "Storage analyzer",
                        subtitle = "Find duplicate and large files to free up space"
                    )
                    Button(onClick = { viewModel.start() }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Start scan")
                    }
                }
            }
            ScanPhase.SCANNING -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning… ${FileUtils.humanCount(state.scannedCount)} files found")
                }
            }
            ScanPhase.HASHING -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Comparing file contents to find duplicates…")
                }
            }
            ScanPhase.DONE -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "${FileUtils.humanCount(state.totalFiles)} files • ${FileUtils.formatBytes(state.totalSize)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${state.duplicateGroups.size} duplicate groups • ${FileUtils.formatBytes(state.wastedBytes)} wasted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (state.duplicateGroups.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.duplicates_title)) }
                    items(state.duplicateGroups, key = { it.hash }) { group ->
                        DuplicateGroupCard(
                            group = group,
                            keepPath = keepMap[group.hash] ?: group.files.firstOrNull()?.absolutePath,
                            onKeepChange = { path ->
                                keepMap = keepMap + (group.hash to path)
                            },
                            onDeleteDuplicates = { keepPath ->
                                val keep = group.files.firstOrNull { it.absolutePath == keepPath }
                                    ?: group.files.firstOrNull()
                                if (keep != null) pendingGroup = group to keep
                            },
                            onOpenDetails = onOpenDetails
                        )
                    }
                }

                if (state.largestFiles.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.large_files_title)) }
                    items(state.largestFiles.take(20), key = { it.absolutePath }) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${FileUtils.formatBytes(file.length())} • ${file.absolutePath}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { pendingFile = file }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Move to trash")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    keepPath: String?,
    onKeepChange: (String) -> Unit,
    onDeleteDuplicates: (String?) -> Unit,
    onOpenDetails: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${group.files.size} copies • ${FileUtils.formatBytes(group.size)} each • ${FileUtils.formatBytes(group.wastedBytes)} wasted",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            group.files.forEach { file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = file.absolutePath == keepPath,
                        onClick = { onKeepChange(file.absolutePath) }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            file.absolutePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { onOpenDetails(file.absolutePath) }) {
                        Text("Details")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onDeleteDuplicates(keepPath) }) {
                Text("Delete duplicates (keep selected)")
            }
        }
    }
}
