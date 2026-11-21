package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import com.sremonak.files.data.model.CategoryStats
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.data.model.FileInfo
import com.sremonak.files.data.model.StorageStats
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

data class HomeUiState(
    val storage: StorageStats? = null,
    val categories: Map<FileCategory, CategoryStats> = emptyMap(),
    val totalFiles: Long = 0,
    val totalSize: Long = 0,
    val recents: List<FileInfo> = emptyList(),
    val favorites: List<FileInfo> = emptyList(),
    val scanning: Boolean = false,
    val scannedCount: Long = 0
)

class HomeViewModel : ViewModel() {

    private val files = FilesApp.instance.files
    private val settings = FilesApp.instance.settings

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(storage = files.storageStats()) }
            loadRecentsAndFavorites()
        }
        startScan()
    }

    private suspend fun loadRecentsAndFavorites() {
        val favPaths = settings.favorites.first()
        val recentPaths = settings.recents.first()
        val (favs, recents) = withContext(Dispatchers.IO) {
            favPaths.map { File(it) }.filter { it.exists() }.map { files.toInfo(it, favPaths) } to
                recentPaths.map { File(it) }.filter { it.exists() }.map { files.toInfo(it, favPaths) }
        }
        _state.update { it.copy(favorites = favs, recents = recents) }
    }

    private fun startScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.update { it.copy(scanning = true, scannedCount = 0) }
            val result = files.scanStorage(files.internalRoot()) { scanned ->
                _state.update { it.copy(scannedCount = scanned) }
            }
            _state.update {
                it.copy(
                    categories = result.categories,
                    totalFiles = result.totalFiles,
                    totalSize = result.totalSize,
                    scanning = false
                )
            }
        }
    }
}
