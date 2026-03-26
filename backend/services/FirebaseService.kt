package services

import models.Alert

class FirebaseService {

    fun sendAlert(alert: Alert) {
        println("Sending alert to Firebase:")
        println(alert)
    }
}