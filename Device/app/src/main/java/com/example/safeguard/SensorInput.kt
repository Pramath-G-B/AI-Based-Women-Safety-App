package com.example.safeguard

data class SensorInput(
    val audioLevel: Int,
    val impactLevel: Int,
    val latitude: Double,
    val longitude: Double,
    val hour: Int,
    val timestamp: Long
)