package com.example.safeguard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingSosState(
    val isPending: Boolean = false,
    val secondsLeft: Int = 0,
    val message: String = "",
    val phoneNumber: String = ""
)

object SosState {

    private val _state = MutableStateFlow(PendingSosState())
    val state: StateFlow<PendingSosState> = _state.asStateFlow()

    fun start(phoneNumber: String, message: String) {
        _state.value = PendingSosState(
            isPending = true,
            secondsLeft = 5,
            message = message,
            phoneNumber = phoneNumber
        )
    }

    fun updateSeconds(seconds: Int) {
        _state.value = _state.value.copy(secondsLeft = seconds)
    }

    fun cancel() {
        _state.value = PendingSosState()
    }

    fun clear() {
        _state.value = PendingSosState()
    }
}