package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import com.sremonak.files.data.model.FileInfo
import com.sremonak.files.data.model.FileCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val searched: Boolean = false,
    val category: FileCategory? = null,
    val minimumSize: Long = 0,
    val recentDays: Int = 0
)

class SearchViewModel : ViewModel() {

    private val files = FilesApp.instance.files
    private val settings = FilesApp.instance.settings

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        search()
    }

    fun setCategory(category: FileCategory?) {
        _state.update { it.copy(category = category) }
        search()
    }

    fun setMinimumSize(bytes: Long) {
        _state.update { it.copy(minimumSize = bytes) }
        search()
    }

    fun setRecentDays(days: Int) {
        _state.update { it.copy(recentDays = days) }
        search()
    }

    fun search() {
        searchJob?.cancel()
        val request = _state.value
        val query = request.query.trim()
        if (query.isEmpty()) {
            _state.update { it.copy(results = emptyList(), searched = false, searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(searching = true, searched = true) }
            val favorites = settings.favorites.first()
            val showHidden = settings.showHidden.first()
            val after = if (request.recentDays > 0) System.currentTimeMillis() - request.recentDays * 86400_000L else 0
            val found = files.searchFiles(query, category = request.category, minSize = request.minimumSize,
                modifiedAfter = after, includeHidden = showHidden)
            val infos = withContext(Dispatchers.IO) {
                found.map { files.toInfo(it, favorites) }
                    .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.name.lowercase() })
            }
            _state.update { it.copy(results = infos, searching = false) }
        }
    }

    fun clear() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", results = emptyList(), searched = false, searching = false) }
    }
}
