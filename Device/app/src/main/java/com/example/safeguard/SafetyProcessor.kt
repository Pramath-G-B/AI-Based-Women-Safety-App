package com.example.safeguard

class SafetyProcessor {

    fun process(sensorInput: SensorInput, placeTypes: List<String>): AlertData? {

        var riskScore = 0
        var triggerType = "none"

        // 🔹 Sensor-based risk
        if (sensorInput.distressAudio) {
            riskScore += 50
            triggerType = "distress_audio"
        }

        if (sensorInput.impactDetected) {
            riskScore += 40
            triggerType = "impact_detected"
        }

        // 🔹 Place-based risk
        if (placeTypes.contains("bar") || placeTypes.contains("night_club")) {
            riskScore += 20
        }

        if (placeTypes.contains("hospital")) {
            riskScore -= 20
        }

        // 🔹 Time-based risk
        if (sensorInput.hour >= 22 || sensorInput.hour <= 5) {
            riskScore += 10
        }

        // 🔹 Final decision
        return if (riskScore >= 50) {
            AlertData(
                lat = sensorInput.latitude,
                lng = sensorInput.longitude,
                riskScore = riskScore,
                triggerType = triggerType,
                mode = "on_device",
                timestamp = sensorInput.timestamp
            )
        } else {
            null
        }
    }
}