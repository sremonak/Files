package com.sremonak.files.ui.screens.recycle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.sremonak.files.data.model.TrashEntry
import com.sremonak.files.ui.components.ConfirmDialog
import com.sremonak.files.ui.components.EmptyState
import com.sremonak.files.util.FileUtils
import com.sremonak.files.viewmodel.RecycleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    viewModel: RecycleViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showEmptyConfirm by remember { mutableStateOf(false) }
    var deleteForeverTarget by remember { mutableStateOf<TrashEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.entries.isNotEmpty()) {
                        TextButton(onClick = { showEmptyConfirm = true }) {
                            Text(stringResource(R.string.action_empty))
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.entries.isEmpty() -> EmptyState(
                icon = Icons.Filled.Delete,
                title = stringResource(R.string.empty_trash),
                subtitle = "Deleted files stay here until you restore or empty the trash"
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text(
                    text = "${state.entries.size} items • ${FileUtils.formatBytes(state.totalSize)}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries, key = { it.id }) { entry ->
                        ListItem(
                            leadingContent = {
                                Icon(
                                    if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                            },
                            headlineContent = {
                                Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(
                                    "${FileUtils.formatBytes(entry.size)} • deleted ${FileUtils.formatDate(entry.deletedAt)}\n${entry.originalPath}",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                androidx.compose.foundation.layout.Row {
                                    IconButton(onClick = { viewModel.restore(entry) }) {
                                        Icon(Icons.Filled.RestoreFromTrash, contentDescription = stringResource(R.string.action_restore))
                                    }
                                    IconButton(onClick = { deleteForeverTarget = entry }) {
                                        Icon(Icons.Filled.DeleteForever, contentDescription = "Delete forever")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showEmptyConfirm) {
            ConfirmDialog(
                title = stringResource(R.string.action_empty),
                message = "Permanently delete all ${state.entries.size} item(s) in trash?",
                confirmText = stringResource(R.string.action_empty),
                onConfirm = { viewModel.emptyAll(); showEmptyConfirm = false },
                onDismiss = { showEmptyConfirm = false }
            )
        }
        deleteForeverTarget?.let { entry ->
            ConfirmDialog(
                title = "Delete forever?",
                message = "\"${entry.name}\" will be permanently deleted.",
                confirmText = stringResource(R.string.action_delete),
                onConfirm = { viewModel.deleteForever(entry); deleteForeverTarget = null },
                onDismiss = { deleteForeverTarget = null }
            )
        }
    }
}
