package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import com.sremonak.files.data.model.DuplicateGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class ScanPhase { IDLE, SCANNING, HASHING, DONE }

data class AnalyzerUiState(
    val phase: ScanPhase = ScanPhase.IDLE,
    val scannedCount: Long = 0,
    val totalFiles: Long = 0,
    val totalSize: Long = 0,
    val largestFiles: List<File> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val wastedBytes: Long = 0
)

class AnalyzerViewModel : ViewModel() {

    private val files = FilesApp.instance.files

    private val _state = MutableStateFlow(AnalyzerUiState())
    val state: StateFlow<AnalyzerUiState> = _state.asStateFlow()

    fun start() {
        viewModelScope.launch {
            _state.update { it.copy(phase = ScanPhase.SCANNING, scannedCount = 0) }
            val result = files.scanStorage(files.internalRoot()) { scanned ->
                _state.update { it.copy(scannedCount = scanned) }
            }
            _state.update {
                it.copy(
                    phase = ScanPhase.HASHING,
                    totalFiles = result.totalFiles,
                    totalSize = result.totalSize,
                    largestFiles = result.largestFiles
                )
            }
            val groups = files.findDuplicates(result.filesBySize)
            _state.update {
                it.copy(
                    phase = ScanPhase.DONE,
                    duplicateGroups = groups,
                    wastedBytes = groups.sumOf { g -> g.wastedBytes }
                )
            }
        }
    }

    fun deleteDuplicates(group: DuplicateGroup, keep: File) {
        viewModelScope.launch {
            val toDelete = group.files.filterNot { it == keep }
            files.deleteToTrash(toDelete)
            _state.update { state ->
                val remaining = state.duplicateGroups.filterNot { it.hash == group.hash }
                state.copy(
                    duplicateGroups = remaining,
                    wastedBytes = remaining.sumOf { it.wastedBytes }
                )
            }
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch {
            files.deleteToTrash(listOf(file))
            _state.update { it.copy(largestFiles = it.largestFiles.filterNot { f -> f == file }) }
        }
    }
}
