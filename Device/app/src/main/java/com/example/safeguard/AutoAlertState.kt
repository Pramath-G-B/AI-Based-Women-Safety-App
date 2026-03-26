package com.example.safeguard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AutoAlertUiState(
    val isVisible: Boolean = false,
    val secondsLeft: Int = 0,
    val triggerType: String = "",
    val cancelRequested: Boolean = false
)

object AutoAlertState {
    private val _state = MutableStateFlow(AutoAlertUiState())
    val state: StateFlow<AutoAlertUiState> = _state.asStateFlow()

    fun show(triggerType: String, secondsLeft: Int = 5) {
        _state.value = AutoAlertUiState(
            isVisible = true,
            secondsLeft = secondsLeft,
            triggerType = triggerType,
            cancelRequested = false
        )
    }

    fun updateSeconds(secondsLeft: Int) {
        _state.value = _state.value.copy(secondsLeft = secondsLeft)
    }

    fun requestCancel() {
        _state.value = _state.value.copy(
            cancelRequested = true,
            isVisible = false
        )
    }

    fun hide() {
        _state.value = AutoAlertUiState()
    }
}