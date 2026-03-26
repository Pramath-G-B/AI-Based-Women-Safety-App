package com.example.safeguard

import com.example.safeguard.core.AlertManager
import com.example.safeguard.core.Mode
import com.example.safeguard.core.ModeManager
import com.example.safeguard.core.RiskEngine
import com.example.safeguard.services.LocationRiskService

data class SafetyEvaluation(
    val riskScore: Int,
    val mode: Mode,
    val shouldTrigger: Boolean,
    val triggerType: String
)

class SafetyProcessor {

    private val riskEngine = RiskEngine()
    private val alertManager = AlertManager()

    fun evaluate(sensorInput: SensorInput, placeTypes: List<String>): SafetyEvaluation {
        val locationRisk = LocationRiskService.getRisk(placeTypes)

        val riskScore = riskEngine.compute(
            audioLevel = sensorInput.audioLevel,
            impactLevel = sensorInput.impactLevel,
            locationRisk = locationRisk,
            hour = sensorInput.hour
        )

        val mode = ModeManager.getMode(riskScore)

        val shouldTrigger = alertManager.shouldTrigger(
            risk = riskScore,
            mode = mode,
            audioLevel = sensorInput.audioLevel,
            impactLevel = sensorInput.impactLevel
        )

        val triggerType = alertManager.getTriggerType(
            audioLevel = sensorInput.audioLevel,
            impactLevel = sensorInput.impactLevel
        )

        return SafetyEvaluation(
            riskScore = riskScore,
            mode = mode,
            shouldTrigger = shouldTrigger,
            triggerType = triggerType
        )
    }
}