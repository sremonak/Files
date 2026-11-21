package com.sremonak.files.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sremonak.files.FilesApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockUiState(
    val pin: String = "",
    val error: Boolean = false,
    val checking: Boolean = false
)

class LockViewModel : ViewModel() {

    private val settings = FilesApp.instance.settings

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    fun appendDigit(digit: Char) {
        _state.update { state ->
            if (state.pin.length >= 8) state else state.copy(pin = state.pin + digit, error = false)
        }
    }

    fun backspace() {
        _state.update { state ->
            if (state.pin.isEmpty()) state else state.copy(pin = state.pin.dropLast(1))
        }
    }

    fun clear() {
        _state.update { it.copy(pin = "", error = false) }
    }

    fun submit(onSuccess: () -> Unit) {
        val pin = _state.value.pin
        if (pin.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true) }
            val ok = settings.verifyPin(pin)
            _state.update { it.copy(checking = false) }
            if (ok) {
                onSuccess()
            } else {
                _state.update { it.copy(error = true, pin = "") }
            }
        }
    }
}
