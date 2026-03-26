package simulation

import core.AlertManager
import core.ModeManager
import core.RiskEngine
import models.SensorInput
import models.Alert
import services.LocationRiskService
import services.FirebaseService

fun main() {

    val engine = RiskEngine()
    val alertManager = AlertManager()

    val scenarios = listOf(
        Triple(false, false, listOf("hospital")),       // safe area
        Triple(true, false, listOf("restaurant")),      // normal place
        Triple(false, true, listOf("bus_station")),     // crowded/public
        Triple(true, true, listOf("night_club"))        // high-risk area
    )

    for ((audio, impact, placeTypes) in scenarios) {

    val locationRisk = LocationRiskService.getRisk(placeTypes)

    val hour = if (placeTypes.contains("night_club")) 1 else 14

    val risk = engine.compute(
        audio,
        impact,
        locationRisk,
        hour
    )

    val mode = ModeManager.getMode(risk)

    val trigger = alertManager.shouldTrigger(
        risk,
        mode,
        audio,
        impact
    )

if (trigger) {
    val triggerType = alertManager.getTriggerType(audio, impact)
    println("----------")
    println("Place Types: $placeTypes")
    println("Location Risk: $locationRisk")
    println("Risk: $risk")
    println("Mode: $mode")
    println("Trigger: $trigger")
    println("Trigger Type: $triggerType")
}
if (trigger) {

    val triggerType = alertManager.getTriggerType(audio, impact)

    val alert = Alert(
        lat = 12.9716,
        lng = 77.5946,
        riskScore = risk,
        triggerType = triggerType,
        timestamp = System.currentTimeMillis(),
        mode = mode.name,

    )

    val payload = FirebaseService.prepareAlertPayload(alert)

    println("Sending to Firebase:")
    println(payload)
}
    }
}