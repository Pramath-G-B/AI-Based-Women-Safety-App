package com.example.safeguard

data class SensorInput(
    val distressAudio: Boolean,
    val impactDetected: Boolean,
    val latitude: Double,
    val longitude: Double,
    val hour: Int,
    val timestamp: Long
)