package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import com.sremonak.files.data.model.FileInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<FileInfo> = emptyList(),
    val searching: Boolean = false,
    val searched: Boolean = false
)

class SearchViewModel : ViewModel() {

    private val files = FilesApp.instance.files
    private val settings = FilesApp.instance.settings

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = query, searching = false, searched = false, results = emptyList()) }
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(350)
                search()
            }
        }
    }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) {
            _state.update { it.copy(results = emptyList(), searched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true, searched = true) }
            val favorites = settings.favorites.first()
            val found = files.searchFiles(query)
            val infos = found.map { files.toInfo(it, favorites) }
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.name.lowercase() })
            _state.update { it.copy(results = infos, searching = false) }
        }
    }

    fun clear() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", results = emptyList(), searched = false, searching = false) }
    }
}
