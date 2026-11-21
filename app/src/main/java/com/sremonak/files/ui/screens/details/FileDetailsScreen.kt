package com.sremonak.files.ui.screens.details

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sremonak.files.R
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.data.model.FileDetails
import com.sremonak.files.ui.components.ConfirmDialog
import com.sremonak.files.ui.components.FileTypeIcon
import com.sremonak.files.ui.components.InfoRow
import com.sremonak.files.ui.components.SectionHeader
import com.sremonak.files.ui.components.TextInputDialog
import com.sremonak.files.util.FileUtils
import com.sremonak.files.util.IntentUtils
import com.sremonak.files.viewmodel.DetailsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsScreen(
    path: String,
    onOpenEditor: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHost = remember { SnackbarHostState() }

    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHost.showSnackbar(it); viewModel.clearMessage() }
    }
    LaunchedEffect(path) {
        viewModel.load(path)
        viewModel.recordRecent()
    }

    fun copyText(text: String, label: String) {
        clipboard.setText(AnnotatedString(text))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_rename)) },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = { showMenu = false; showRename = true }
                            )
                            val details = state.details
                            if (details != null && !details.isDirectory) {
                                if (details.category() == FileCategory.IMAGE) {
                                    DropdownMenuItem(
                                        text = { Text("Set as wallpaper") },
                                        leadingIcon = { Icon(Icons.Filled.Wallpaper, null) },
                                        onClick = {
                                            showMenu = false
                                            IntentUtils.setAsWallpaper(context, File(path))
                                        }
                                    )
                                }
                                if (details.category() == FileCategory.AUDIO) {
                                    DropdownMenuItem(
                                        text = { Text("Set as ringtone") },
                                        leadingIcon = { Icon(Icons.Filled.MusicNote, null) },
                                        onClick = {
                                            showMenu = false
                                            IntentUtils.setAsRingtone(context, File(path))
                                        }
                                    )
                                }
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { showMenu = false; showDeleteConfirm = true }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        val details = state.details
        when {
            state.loading || details == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FileTypeIcon(
                            info = com.sremonak.files.data.model.FileInfo(
                                file = File(path),
                                name = details.name,
                                isDirectory = details.isDirectory,
                                size = details.size,
                                lastModified = details.modifiedAt,
                                createdAt = details.createdAt,
                                mimeType = details.mimeType,
                                extension = details.extension,
                                category = details.category(),
                                isHidden = details.isHidden
                            ),
                            size = 96.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = details.name,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (details.isDirectory) "Folder" else "${FileUtils.formatBytes(details.size)} • ${details.mimeType ?: "Unknown type"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            TextButton(onClick = {
                                IntentUtils.openFile(context, File(path))
                                viewModel.recordRecent()
                            }) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(stringResource(R.string.action_open))
                            }
                            TextButton(onClick = { IntentUtils.shareFiles(context, listOf(File(path))) }) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(stringResource(R.string.action_share))
                            }
                            if (!details.isDirectory && FileUtils.isTextFile(File(path))) {
                                TextButton(onClick = { onOpenEditor(path) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(stringResource(R.string.action_edit))
                                }
                            }
                            TextButton(onClick = { copyText(details.path, "Path") }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Copy path")
                            }
                        }
                    }
                }

                item {
                    SectionHeader("Information")
                    InfoRow("Name", details.name, Icons.Filled.Info)
                    InfoRow("Path", details.path, Icons.Filled.FolderOpen, mono = true, onCopy = { copyText(details.path, "Path") })
                    InfoRow("Size", FileUtils.formatBytes(details.size), Icons.Filled.Storage)
                    InfoRow("Type", details.mimeType ?: "Unknown", Icons.Filled.Info)
                    InfoRow("Extension", if (details.extension.isEmpty()) "—" else ".${details.extension}", Icons.Filled.Info)
                    InfoRow("Storage volume", details.volumeLabel, Icons.Filled.Storage)
                    InfoRow("Hidden", if (details.isHidden) "Yes" else "No", Icons.Filled.Info)
                    if (details.isSymlink) {
                        InfoRow("Symbolic link", "Yes → ${details.linkTarget ?: ""}", Icons.Filled.Info, mono = true)
                    }
                    InfoRow("Permissions", details.permissions, Icons.Filled.Info, mono = true)
                    InfoRow("Owner", details.owner, Icons.Filled.Info)
                }

                item {
                    SectionHeader("Dates")
                    InfoRow("Created", FileUtils.formatDate(details.createdAt), Icons.Filled.CalendarMonth)
                    InfoRow("Modified", FileUtils.formatDate(details.modifiedAt), Icons.Filled.CalendarMonth)
                    InfoRow("Last accessed", FileUtils.formatDate(details.accessedAt), Icons.Filled.AccessTime)
                }

                if (details.media.width > 0 || details.media.durationMs > 0 || details.media.bitrateKbps > 0) {
                    item {
                        SectionHeader("Media")
                        if (details.media.width > 0) {
                            InfoRow("Dimensions", "${details.media.width} × ${details.media.height} px", Icons.Filled.Image)
                        }
                        if (details.media.durationMs > 0) {
                            InfoRow("Duration", FileUtils.formatDuration(details.media.durationMs), Icons.Filled.AccessTime)
                        }
                        if (details.media.bitrateKbps > 0) {
                            InfoRow("Bitrate", FileUtils.formatBitrate(details.media.bitrateKbps), Icons.Filled.Info)
                        }
                    }
                }

                if (details.exif.isNotEmpty()) {
                    item {
                        SectionHeader("EXIF data")
                        details.exif.forEach { (label, value) ->
                            InfoRow(label, value, Icons.Filled.Image)
                        }
                    }
                }

                if (!details.isDirectory) {
                    item {
                        SectionHeader("Checksums")
                        listOf("MD5", "SHA-1", "SHA-256").forEach { algorithm ->
                            val hash = state.hashes[algorithm]
                            val computing = state.computing == algorithm
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = algorithm,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = hash ?: "Not computed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = com.sremonak.files.ui.theme.MonoFontFamily
                                )
                            }
                            when {
                                computing -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                hash != null -> IconButton(onClick = { copyText(hash, algorithm) }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                                }
                                else -> TextButton(onClick = { viewModel.computeHash(algorithm) }) {
                                    Text("Compute")
                                }
                            }
                            }
                        }
                    }
                }
            }
        }

        if (showRename) {
            TextInputDialog(
                title = stringResource(R.string.action_rename),
                initialValue = state.details?.name ?: "",
                confirmText = stringResource(R.string.action_save),
                onConfirm = { newName ->
                    viewModel.rename(newName) { showRename = false }
                },
                onDismiss = { showRename = false }
            )
        }
        if (showDeleteConfirm) {
            ConfirmDialog(
                title = stringResource(R.string.action_delete),
                message = "Move this item to trash?",
                confirmText = stringResource(R.string.action_delete),
                onConfirm = { viewModel.deleteToTrash { ok -> showDeleteConfirm = false; if (ok) onBack() } },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

private fun FileDetails.category(): FileCategory = FileUtils.categoryFor(File(path))
