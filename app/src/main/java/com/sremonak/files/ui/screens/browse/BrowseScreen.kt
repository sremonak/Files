package com.sremonak.files.ui.screens.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sremonak.files.FilesApp
import com.sremonak.files.R
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.data.model.FileInfo
import com.sremonak.files.data.model.SortOption
import com.sremonak.files.data.model.ViewMode
import com.sremonak.files.ui.components.ConfirmDialog
import com.sremonak.files.ui.components.EmptyState
import com.sremonak.files.ui.components.FileGridItem
import com.sremonak.files.ui.components.FileListItem
import com.sremonak.files.ui.components.TextInputDialog
import com.sremonak.files.util.BatchRename
import com.sremonak.files.util.FileUtils
import com.sremonak.files.util.IntentUtils
import com.sremonak.files.viewmodel.BrowseViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    initialPath: String,
    categoryFilter: FileCategory? = null,
    onOpenDetails: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BrowseViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val files = FilesApp.instance.files
    val settings = FilesApp.instance.settings
    val confirmDelete by settings.confirmDelete.collectAsStateWithLifecycle(initialValue = true)
    val hapticsEnabled by settings.haptics.collectAsStateWithLifecycle(initialValue = true)
    val haptic = LocalHapticFeedback.current
    val snackbarHost = remember { SnackbarHostState() }

    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileInfo?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchRename by remember { mutableStateOf(false) }

    LaunchedEffect(initialPath, categoryFilter) { viewModel.load(initialPath, categoryFilter) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && state.dirPath.isNotBlank()) viewModel.reload()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            snackbarHost.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    val rootPath = files.internalRoot().absolutePath
    val canGoUp = categoryFilter == null && state.dirPath.isNotBlank() &&
        state.dirPath != rootPath && state.dirPath.startsWith(rootPath + File.separator)
    BackHandler(enabled = state.isSelectionMode || canGoUp) {
        if (state.isSelectionMode) viewModel.clearSelection()
        else File(state.dirPath).parentFile?.let { viewModel.load(it.absolutePath) }
    }

    val selectedInfos = remember(state.selection, state.items) {
        state.items.filter { it.path in state.selection }
    }
    val singleSelected = selectedInfos.singleOrNull()

    fun handleItemClick(info: FileInfo) {
        if (state.isSelectionMode) {
            viewModel.toggleSelect(info.path)
            return
        }
        if (info.isDirectory) viewModel.load(info.path)
        else onOpenDetails(info.path)
    }

    fun handleItemLongClick(info: FileInfo) {
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.toggleSelect(info.path)
    }

    fun requestDelete() {
        if (confirmDelete) showDeleteConfirm = true else viewModel.deleteSelected()
    }

    val title = when {
        state.isSelectionMode -> "${state.selection.size} selected"
        categoryFilter != null -> categoryLabel(categoryFilter)
        state.dirPath.isNotBlank() -> File(state.dirPath).name.ifBlank { "Internal storage" }
        else -> stringResource(R.string.browse_title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (categoryFilter != null || initialPath.isNotEmpty() || canGoUp) {
                        IconButton(onClick = {
                            val parent = File(state.dirPath).parentFile
                            if (canGoUp && parent != null) {
                                viewModel.load(parent.absolutePath)
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!state.isSelectionMode) {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        IconButton(onClick = {
                            viewModel.setViewMode(
                                if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                            )
                        }) {
                            Icon(
                                if (state.viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                                contentDescription = "Toggle view"
                            )
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                if (categoryFilter == null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_new_folder)) },
                                        leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                                        onClick = { showOverflowMenu = false; showNewFolder = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_new_file)) },
                                        leadingIcon = { Icon(Icons.Filled.NoteAdd, null) },
                                        onClick = { showOverflowMenu = false; showNewFile = true }
                                    )
                                    if (files.clipboard.isActive) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_paste)) },
                                            leadingIcon = { Icon(Icons.Filled.ContentPaste, null) },
                                            onClick = { showOverflowMenu = false; viewModel.paste() }
                                        )
                                    }
                                    HorizontalDivider()
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_select_all)) },
                                    leadingIcon = { Icon(Icons.Filled.SelectAll, null) },
                                    onClick = { showOverflowMenu = false; viewModel.selectAll() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Show hidden files") },
                                    leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                                    trailingIcon = {
                                        if (state.showHidden) Icon(Icons.Filled.Check, null)
                                    },
                                    onClick = { showOverflowMenu = false; viewModel.setShowHidden(!state.showHidden) }
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            if (categoryFilter == null && !state.isSelectionMode) {
                Box {
                    FloatingActionButton(onClick = { showFabMenu = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New")
                    }
                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_new_folder)) },
                            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                            onClick = { showFabMenu = false; showNewFolder = true }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_new_file)) },
                            leadingIcon = { Icon(Icons.Filled.NoteAdd, null) },
                            onClick = { showFabMenu = false; showNewFile = true }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (state.isSelectionMode) {
                Surface(tonalElevation = 6.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (singleSelected != null) {
                            SelectionAction(Icons.Filled.FolderOpen, stringResource(R.string.action_open)) {
                                IntentUtils.openFile(context, singleSelected.file)
                                viewModel.recordRecent(singleSelected.file)
                                viewModel.clearSelection()
                            }
                            SelectionAction(Icons.Filled.Info, stringResource(R.string.action_details)) {
                                onOpenDetails(singleSelected.path)
                            }
                            if (FileUtils.isTextFile(singleSelected.file)) {
                                SelectionAction(Icons.Filled.Edit, stringResource(R.string.action_edit)) {
                                    onOpenEditor(singleSelected.path)
                                }
                            }
                            SelectionAction(
                                if (singleSelected.isFavorite) Icons.Filled.Star else Icons.Filled.Star,
                                "Favorite"
                            ) {
                                viewModel.toggleFavorite(singleSelected.file)
                            }
                            SelectionAction(Icons.Filled.Edit, stringResource(R.string.action_rename)) {
                                renameTarget = singleSelected
                            }
                            if (singleSelected.category == FileCategory.ARCHIVE) {
                                SelectionAction(Icons.Filled.FileDownload, "Extract") {
                                    viewModel.extract(singleSelected.file)
                                    viewModel.clearSelection()
                                }
                            }
                        }
                        if (selectedInfos.size > 1 && selectedInfos.all { !it.isDirectory }) {
                            SelectionAction(Icons.Filled.Edit, stringResource(R.string.batch_rename)) {
                                showBatchRename = true
                            }
                        }
                        SelectionAction(Icons.Filled.Share, stringResource(R.string.action_share)) {
                            IntentUtils.shareFiles(context, viewModel.selectedFiles())
                        }
                        SelectionAction(Icons.Filled.ContentCopy, stringResource(R.string.action_copy)) {
                            viewModel.copySelected()
                        }
                        SelectionAction(Icons.Filled.DriveFileMove, stringResource(R.string.action_move)) {
                            viewModel.moveSelected()
                        }
                        SelectionAction(Icons.Filled.Archive, stringResource(R.string.action_zip)) {
                            viewModel.zipSelected()
                        }
                        SelectionAction(Icons.Filled.Delete, stringResource(R.string.action_delete)) {
                            requestDelete()
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (categoryFilter == null && state.dirPath.isNotBlank()) {
                PathBar(dirPath = state.dirPath, onNavigate = { viewModel.load(it) })
            }

            if (files.clipboard.isActive && categoryFilter == null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${files.clipboard.files.size} item(s) ${if (files.clipboard.move) "cut" else "copied"}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { viewModel.paste() }, enabled = state.transferProgress == null) {
                            Text(stringResource(R.string.action_paste))
                        }
                        IconButton(onClick = { files.clearClipboard() }, enabled = state.transferProgress == null) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            state.transferProgress?.let { (done, total) ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(stringResource(R.string.transfer_progress, done, total), style = MaterialTheme.typography.labelMedium)
                    LinearProgressIndicator(progress = if (total == 0) 0f else done.toFloat() / total, modifier = Modifier.fillMaxWidth())
                }
            }
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                state.items.isEmpty() -> EmptyState(
                    icon = Icons.Filled.FolderOpen,
                    title = stringResource(R.string.empty_folder)
                )
                state.viewMode == ViewMode.LIST -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)
                ) {
                    items(state.items, key = { it.path }) { info ->
                        FileListItem(
                            info = info,
                            selected = info.path in state.selection,
                            selectionMode = state.isSelectionMode,
                            onClick = { handleItemClick(info) },
                            onLongClick = { handleItemLongClick(info) }
                        )
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(state.gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)
                ) {
                    items(state.items, key = { it.path }) { info ->
                        FileGridItem(
                            info = info,
                            selected = info.path in state.selection,
                            selectionMode = state.isSelectionMode,
                            onClick = { handleItemClick(info) },
                            onLongClick = { handleItemLongClick(info) }
                        )
                    }
                }
            }
        }

        // Sort menu
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
        ) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortLabel(option)) },
                    trailingIcon = {
                        if (state.sortOption == option) Icon(Icons.Filled.Check, null)
                    },
                    onClick = {
                        showSortMenu = false
                        viewModel.setSort(option, state.ascending)
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (state.ascending) "Ascending" else "Descending") },
                onClick = {
                    showSortMenu = false
                    viewModel.setSort(state.sortOption, !state.ascending)
                }
            )
        }

        // Dialogs
        if (showBatchRename) {
            BatchRenameDialog(
                selected = selectedInfos.map { it.file },
                onConfirm = { viewModel.batchRename(it); showBatchRename = false },
                onDismiss = { showBatchRename = false }
            )
        }
        renameTarget?.let { target ->
            TextInputDialog(
                title = stringResource(R.string.action_rename),
                initialValue = target.name,
                confirmText = stringResource(R.string.action_save),
                onConfirm = { newName ->
                    viewModel.rename(target.path, newName)
                    renameTarget = null
                },
                onDismiss = { renameTarget = null }
            )
        }
        if (showNewFolder) {
            TextInputDialog(
                title = stringResource(R.string.action_new_folder),
                confirmText = stringResource(R.string.action_save),
                onConfirm = { viewModel.createFolder(it); showNewFolder = false },
                onDismiss = { showNewFolder = false }
            )
        }
        if (showNewFile) {
            TextInputDialog(
                title = stringResource(R.string.action_new_file),
                confirmText = stringResource(R.string.action_save),
                onConfirm = { viewModel.createTextFile(it); showNewFile = false },
                onDismiss = { showNewFile = false }
            )
        }
        if (showDeleteConfirm) {
            ConfirmDialog(
                title = stringResource(R.string.action_delete),
                message = "Move ${state.selection.size} item(s) to trash?",
                confirmText = stringResource(R.string.action_delete),
                onConfirm = { viewModel.deleteSelected(); showDeleteConfirm = false },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

@Composable
private fun BatchRenameDialog(selected: List<File>, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var prefix by remember { mutableStateOf("") }
    val plan = remember(selected, prefix) { runCatching { BatchRename.plan(selected, prefix) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.batch_rename)) },
        text = {
            Column {
                OutlinedTextField(value = prefix, onValueChange = { prefix = it }, singleLine = true,
                    label = { Text(stringResource(R.string.batch_prefix)) })
                if (prefix.isNotBlank()) {
                    val targets = plan.getOrNull()
                    if (targets != null) {
                        Text(stringResource(R.string.batch_preview), style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 12.dp))
                        targets.take(3).forEach { (_, target) -> Text(target.name, maxLines = 1) }
                        if (targets.size > 3) Text("… +${targets.size - 3}")
                    } else {
                        Text(plan.exceptionOrNull()?.message ?: "Invalid name", color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(prefix) }, enabled = plan.isSuccess) { Text(stringResource(R.string.action_rename)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PathBar(dirPath: String, onNavigate: (String) -> Unit) {
    val root = FilesApp.instance.files.internalRoot().absolutePath
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!dirPath.startsWith(root)) {
            Text(
                text = File(dirPath).name,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            return
        }
        TextButton(onClick = { onNavigate(root) }) { Text("Internal storage") }
        val rel = dirPath.removePrefix(root).trim('/')
        if (rel.isNotEmpty()) {
            val segments = rel.split('/')
            segments.forEachIndexed { index, segment ->
                Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val target = root + "/" + segments.take(index + 1).joinToString("/")
                TextButton(onClick = { onNavigate(target) }) { Text(segment) }
            }
        }
    }
}

private fun sortLabel(option: SortOption): String = when (option) {
    SortOption.NAME -> "Name"
    SortOption.SIZE -> "Size"
    SortOption.DATE_MODIFIED -> "Date modified"
    SortOption.DATE_CREATED -> "Date created"
    SortOption.TYPE -> "Type"
}

private fun categoryLabel(category: FileCategory): String = when (category) {
    FileCategory.IMAGE -> "Images"
    FileCategory.VIDEO -> "Videos"
    FileCategory.AUDIO -> "Audio"
    FileCategory.DOCUMENT -> "Documents"
    FileCategory.PDF -> "PDFs"
    FileCategory.APK -> "APKs"
    FileCategory.ARCHIVE -> "Archives"
    FileCategory.CODE -> "Code"
    FileCategory.FONT -> "Fonts"
    FileCategory.OTHER -> "Other"
}
