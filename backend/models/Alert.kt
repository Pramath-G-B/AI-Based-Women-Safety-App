package models

data class Alert(
    val lat: Double,
    val lng: Double,
    val riskScore: Int,
    val triggerType: String,
    val timestamp: Long
)