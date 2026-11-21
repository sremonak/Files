package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import com.sremonak.files.data.model.TrashEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecycleUiState(
    val entries: List<TrashEntry> = emptyList(),
    val totalSize: Long = 0,
    val loading: Boolean = true
)

class RecycleViewModel : ViewModel() {

    private val files = FilesApp.instance.files

    private val _state = MutableStateFlow(RecycleUiState())
    val state: StateFlow<RecycleUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val entries = files.listTrash()
            val size = files.trashSize()
            _state.update { it.copy(entries = entries, totalSize = size, loading = false) }
        }
    }

    fun restore(entry: TrashEntry) {
        viewModelScope.launch {
            files.restore(entry)
            load()
        }
    }

    fun deleteForever(entry: TrashEntry) {
        viewModelScope.launch {
            files.deleteTrashEntry(entry)
            load()
        }
    }

    fun emptyAll() {
        viewModelScope.launch {
            files.emptyTrash()
            load()
        }
    }
}
