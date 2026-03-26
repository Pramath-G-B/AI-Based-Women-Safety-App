package com.example.safeguard

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAlertHelper {

    fun sendManualSosAlert(
        context: Context,
        lat: Double,
        lng: Double,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val userId = UserSessionManager.getUserId(context)

        val alertData = hashMapOf(
            "userId" to userId,
            "lat" to lat,
            "lng" to lng,
            "risk" to 100,
            "trigger" to true,
            "triggerType" to "SOS_BUTTON",
            "mode" to "MANUAL",
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("alerts")
            .add(alertData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}