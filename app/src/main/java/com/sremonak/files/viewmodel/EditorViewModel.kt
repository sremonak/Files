package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EditorUiState(
    val fileName: String = "",
    val content: String = "",
    val savedContent: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val saveError: String? = null,
    val savedTick: Int = 0
) {
    val isDirty: Boolean get() = content != savedContent
}

class EditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var file: File? = null

    fun load(path: String) {
        file = File(path)
        viewModelScope.launch {
            _state.update { it.copy(loading = true, fileName = file?.name ?: "") }
            val result = withContext(Dispatchers.IO) {
                val f = file ?: return@withContext "File not found"
                if (!f.exists()) return@withContext "File not found"
                if (f.length() > 2_000_000) return@withContext "File is too large to edit (max 2 MB)"
                try {
                    val text = f.readText(Charsets.UTF_8)
                    _state.update { it.copy(content = text, savedContent = text, loading = false) }
                    null
                } catch (e: Exception) {
                    "Could not read file: ${e.message}"
                }
            }
            if (result != null) {
                _state.update { it.copy(error = result, loading = false) }
            }
        }
    }

    fun clearSaveError() { _state.update { it.copy(saveError = null) } }

    fun update(text: String) {
        _state.update { it.copy(content = text) }
    }

    fun save() {
        val f = file ?: return
        val text = _state.value.content
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val parent = f.parentFile ?: return@withContext false
                    val temp = File.createTempFile(".filespro-", ".tmp", parent)
                    try {
                        temp.outputStream().use { output ->
                            output.write(text.toByteArray(Charsets.UTF_8))
                            output.fd.sync()
                        }
                        temp.renameTo(f)
                    } finally {
                        if (temp.exists()) temp.delete()
                    }
                } catch (_: Exception) {
                    false
                }
            }
            if (ok) {
                _state.update { it.copy(savedContent = text, savedTick = it.savedTick + 1, saveError = null) }
            } else {
                _state.update { it.copy(saveError = "Save failed; original file was kept") }
            }
        }
    }
}
