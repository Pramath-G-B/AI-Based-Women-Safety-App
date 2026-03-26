package com.example.safeguard

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAlertHelper {

    fun sendManualSosAlert(
        lat: Double,
        lng: Double,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val payload = hashMapOf(
            "lat" to lat,
            "lng" to lng,
            "riskScore" to 100,
            "triggerType" to "SOS_BUTTON",
            "mode" to "MANUAL",
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("alerts")
            .add(payload)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}