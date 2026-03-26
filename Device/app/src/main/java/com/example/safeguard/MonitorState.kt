package com.example.safeguard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveMonitorData(
    val audioLevel: Int = 0,
    val impactLevel: Int = 0,
    val risk: Int = 0,
    val mode: String = "NORMAL",
    val trigger: Boolean = false,
    val triggerType: String = "NONE",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

object MonitorState {
    private val _state = MutableStateFlow(LiveMonitorData())
    val state: StateFlow<LiveMonitorData> = _state.asStateFlow()

    fun update(newState: LiveMonitorData) {
        _state.value = newState
    }
}