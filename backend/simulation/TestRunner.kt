package simulation

import core.AlertManager
import core.ModeManager
import core.RiskEngine
import models.SensorInput

fun main() {

    val engine = RiskEngine()
    val alertManager = AlertManager()

    val scenarios = listOf(
        SensorInput(false, false, 10, 14),  // safe
        SensorInput(true, false, 20, 22),   // audio
        SensorInput(false, true, 20, 2),    // impact at night
        SensorInput(true, true, 30, 1)      // dangerous
    )

    for (input in scenarios) {

        val risk = engine.compute(
            input.distressAudio,
            input.impactDetected,
            input.locationRisk,
            input.hour
        )

        val mode = ModeManager.getMode(risk)

        val trigger = alertManager.shouldTrigger(
            risk,
            mode,
            input.distressAudio,
            input.impactDetected
        )

        println("----------")
        println("Input: $input")
        println("Risk: $risk")
        println("Mode: $mode")
        println("Trigger: $trigger")
    }
}