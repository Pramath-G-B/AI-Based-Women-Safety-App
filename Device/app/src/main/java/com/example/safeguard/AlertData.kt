package com.example.safeguard

data class AlertData(
    val lat: Double,
    val lng: Double,
    val riskScore: Int,
    val triggerType: String,
    val mode: String,
    val timestamp: Long
)