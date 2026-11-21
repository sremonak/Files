package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import com.sremonak.files.data.model.FileDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class DetailsUiState(
    val details: FileDetails? = null,
    val hashes: Map<String, String> = emptyMap(),
    val computing: String? = null,
    val isFavorite: Boolean = false,
    val loading: Boolean = true
)

class DetailsViewModel : ViewModel() {

    private val files = FilesApp.instance.files
    private val settings = FilesApp.instance.settings

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    private var currentFile: File? = null

    fun load(path: String) {
        currentFile = File(path)
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val details = files.getDetails(File(path))
            val favorites = settings.favorites.first()
            _state.update {
                it.copy(
                    details = details,
                    isFavorite = favorites.contains(path),
                    loading = false
                )
            }
        }
    }

    fun computeHash(algorithm: String) {
        val file = currentFile ?: return
        viewModelScope.launch {
            _state.update { it.copy(computing = algorithm) }
            val hash = files.computeHash(file, algorithm)
            _state.update { state ->
                state.copy(
                    computing = null,
                    hashes = state.hashes + (algorithm to hash)
                )
            }
        }
    }

    fun toggleFavorite() {
        val file = currentFile ?: return
        viewModelScope.launch {
            val nowFavorite = settings.toggleFavorite(file.absolutePath)
            _state.update { it.copy(isFavorite = nowFavorite) }
        }
    }

    fun recordRecent() {
        val file = currentFile ?: return
        viewModelScope.launch { settings.addRecent(file.absolutePath) }
    }

    fun rename(newName: String, onDone: (Boolean) -> Unit) {
        val file = currentFile ?: return
        viewModelScope.launch {
            val ok = files.rename(file, newName)
            if (ok) {
                currentFile = File(file.parent, newName)
                load(currentFile!!.absolutePath)
            }
            onDone(ok)
        }
    }

    fun deleteToTrash(onDone: () -> Unit) {
        val file = currentFile ?: return
        viewModelScope.launch {
            files.deleteToTrash(listOf(file))
            onDone()
        }
    }
}
