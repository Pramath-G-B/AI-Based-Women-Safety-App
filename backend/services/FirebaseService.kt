package services

import models.Alert

object FirebaseService {

    fun prepareAlertPayload(alert: Alert): Map<String, Any> {
        return mapOf(
            "lat" to alert.lat,
            "lng" to alert.lng,
            "riskScore" to alert.riskScore,
            "triggerType" to alert.triggerType,
            "timestamp" to alert.timestamp
            "mode" to alert.mode
        )
    }
}