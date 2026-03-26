package models

data class SensorInput(
    val distressAudio: Boolean,
    val impactDetected: Boolean,
    val locationRisk: Int,
    val hour: Int
)