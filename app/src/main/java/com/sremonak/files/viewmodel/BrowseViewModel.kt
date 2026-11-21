package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.util.BatchRename
import com.sremonak.files.data.model.FileInfo
import com.sremonak.files.data.model.SortOption
import com.sremonak.files.data.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class BrowseUiState(
    val dirPath: String = "",
    val items: List<FileInfo> = emptyList(),
    val selection: Set<String> = emptySet(),
    val sortOption: SortOption = SortOption.NAME,
    val ascending: Boolean = true,
    val showHidden: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val gridColumns: Int = 3,
    val loading: Boolean = true,
    val message: String? = null,
    val categoryFilter: FileCategory? = null,
    val transferProgress: Pair<Int, Int>? = null
) {
    val isSelectionMode: Boolean get() = selection.isNotEmpty()
}

class BrowseViewModel : ViewModel() {

    private val files = FilesApp.instance.files
    private val settings = FilesApp.instance.settings

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    private var allItems: List<FileInfo> = emptyList()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            settings.sortOption.collect { option ->
                _state.update { it.copy(sortOption = option) }
                applySort()
            }
        }
        viewModelScope.launch {
            settings.sortAscending.collect { asc ->
                _state.update { it.copy(ascending = asc) }
                applySort()
            }
        }
        viewModelScope.launch {
            settings.showHidden.collect { hidden ->
                _state.update { it.copy(showHidden = hidden) }
                applySort()
            }
        }
        viewModelScope.launch {
            settings.gridColumns.collect { cols ->
                _state.update { it.copy(gridColumns = cols) }
            }
        }
    }

    fun load(path: String, categoryFilter: FileCategory? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, selection = emptySet(), categoryFilter = categoryFilter) }
            val favorites = settings.favorites.first()
            val raw: List<File> = if (categoryFilter != null) {
                files.searchFilesByCategory(categoryFilter)
            } else {
                val dir = File(path.ifBlank { defaultHome() })
                _state.update { it.copy(dirPath = dir.absolutePath) }
                files.listFiles(dir)
            }
            allItems = withContext(Dispatchers.IO) { raw.map { files.toInfo(it, favorites) } }
            applySort()
            _state.update { it.copy(loading = false) }
        }
    }

    private suspend fun defaultHome(): String {
        val custom = settings.homeDir.first()
        return if (custom.isNotBlank() && File(custom).exists()) custom else files.internalRoot().absolutePath
    }

    fun reload() {
        val current = _state.value
        load(current.dirPath, current.categoryFilter)
    }

    private fun applySort() {
        val s = _state.value
        val visible = allItems.filter { s.showHidden || !it.isHidden }
        val secondary: Comparator<FileInfo> = when (s.sortOption) {
            SortOption.NAME -> compareBy { it.name.lowercase() }
            SortOption.SIZE -> compareBy { it.size }
            SortOption.DATE_MODIFIED -> compareBy { it.lastModified }
            SortOption.DATE_CREATED -> compareBy { it.createdAt }
            SortOption.TYPE -> compareBy { it.category.name }
        }
        val ordered = if (s.ascending) secondary else secondary.reversed()
        val sorted = visible.sortedWith(compareByDescending<FileInfo> { it.isDirectory }.then(ordered))
        _state.update { it.copy(items = sorted) }
    }

    // ---------- Selection ----------
    fun toggleSelect(path: String) {
        _state.update { state ->
            val next = if (path in state.selection) state.selection - path else state.selection + path
            state.copy(selection = next)
        }
    }

    fun selectAll() {
        _state.update { it.copy(selection = it.items.map { info -> info.path }.toSet()) }
    }

    fun clearSelection() {
        _state.update { it.copy(selection = emptySet()) }
    }

    fun selectedFiles(): List<File> =
        _state.value.selection.map { File(it) }.filter { f -> f.exists() }

    // ---------- View options ----------
    fun setSort(option: SortOption, ascending: Boolean) {
        viewModelScope.launch { settings.setSort(option, ascending) }
    }

    fun setViewMode(mode: ViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }

    fun setShowHidden(show: Boolean) {
        viewModelScope.launch { settings.setShowHidden(show) }
    }

    // ---------- Mutations ----------
    fun createFolder(name: String) {
        viewModelScope.launch {
            val ok = files.createFolder(File(_state.value.dirPath), name)
            _state.update { it.copy(message = if (ok) "Folder created" else "Could not create folder") }
            reload()
        }
    }

    fun createTextFile(name: String) {
        viewModelScope.launch {
            val ok = files.createTextFile(File(_state.value.dirPath), name)
            _state.update { it.copy(message = if (ok) "File created" else "Could not create file") }
            reload()
        }
    }

    fun rename(path: String, newName: String) {
        viewModelScope.launch {
            val source = File(path)
            val ok = files.rename(source, newName)
            if (ok) source.parentFile?.let { settings.relocatePaths(path, File(it, newName.trim()).absolutePath) }
            _state.update { it.copy(message = if (ok) "Renamed" else "Rename failed (name may exist)") }
            reload()
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val targets = selectedFiles()
            val count = files.deleteToTrash(targets)
            _state.update { it.copy(selection = emptySet(), message = "$count item(s) moved to trash") }
            reload()
        }
    }

    fun copySelected() {
        files.setClipboard(selectedFiles(), move = false)
        _state.update { it.copy(message = "Copied ${selectedFiles().size} item(s)") }
        clearSelection()
    }

    fun moveSelected() {
        files.setClipboard(selectedFiles(), move = true)
        _state.update { it.copy(message = "Cut ${selectedFiles().size} item(s)") }
        clearSelection()
    }

    fun paste() {
        val clip = files.clipboard
        if (!clip.isActive || _state.value.transferProgress != null) return
        val destination = File(_state.value.dirPath)
        _state.update { it.copy(transferProgress = 0 to clip.files.size) }
        viewModelScope.launch {
            try {
                val report = files.transferBatch(clip.files, destination, clip.move) { completed, total ->
                    _state.update { it.copy(transferProgress = completed to total) }
                }
                if (clip.move) report.succeeded.forEach { (source, target) ->
                    settings.relocatePaths(source.absolutePath, target.absolutePath)
                }
                if (report.failed.isEmpty()) files.clearClipboard() else files.setClipboard(report.failed, clip.move)
                _state.update {
                    it.copy(message = "Pasted ${report.succeeded.size} of ${clip.files.size} item(s)" +
                        if (report.failed.isNotEmpty()) "; ${report.failed.size} failed and remain in clipboard" else "")
                }
                reload()
            } finally {
                _state.update { it.copy(transferProgress = null) }
            }
        }
    }

    fun batchRename(prefix: String) {
        val selected = selectedFiles()
        viewModelScope.launch {
            val plan = try { withContext(Dispatchers.IO) { BatchRename.plan(selected, prefix) } }
            catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Cannot rename selection") }
                return@launch
            }
            var succeeded = 0
            for ((source, target) in plan) {
                if (source.absolutePath == target.absolutePath) succeeded++
                else if (files.rename(source, target.name)) {
                    settings.relocatePaths(source.absolutePath, target.absolutePath)
                    succeeded++
                }
            }
            _state.update { it.copy(message = "Renamed $succeeded of ${plan.size} files") }
            reload()
        }
    }

    fun zipSelected() {
        viewModelScope.launch {
            val selected = selectedFiles()
            val destination = if (_state.value.categoryFilter != null) selected.firstOrNull()?.parentFile else File(_state.value.dirPath)
            val zip = if (destination != null) files.zipFiles(selected, destination) else null
            _state.update {
                it.copy(
                    selection = emptySet(),
                    message = if (zip != null) "Created ${zip.name}" else "Compression failed"
                )
            }
            reload()
        }
    }

    fun extract(file: File) {
        viewModelScope.launch {
            val out = files.unzip(file, file.parentFile ?: File(_state.value.dirPath))
            _state.update { it.copy(message = if (out != null) "Extracted to ${out.name}" else "Extraction failed") }
            reload()
        }
    }

    fun toggleFavorite(file: File) {
        viewModelScope.launch {
            val nowFavorite = settings.toggleFavorite(file.absolutePath)
            allItems = allItems.map {
                if (it.path == file.absolutePath) it.copy(isFavorite = nowFavorite) else it
            }
            applySort()
        }
    }

    fun recordRecent(file: File) {
        viewModelScope.launch { settings.addRecent(file.absolutePath) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
