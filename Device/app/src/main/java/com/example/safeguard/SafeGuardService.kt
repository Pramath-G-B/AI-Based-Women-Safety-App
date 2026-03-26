package com.example.safeguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import java.util.*
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore

class SafeGuardService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private lateinit var processor: SafetyProcessor

    override fun onCreate() {
        super.onCreate()
        processor = SafetyProcessor()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("SafeGuard Started")
        scope.launch {
            while (true) {

                val location = getLocation()

                if (location != null) {

                    val currentHour = Calendar.getInstance()
                        .get(Calendar.HOUR_OF_DAY)

                    val sensorInput = SensorInput(
                        distressAudio = false,
                        impactDetected = false,
                        latitude = location.first,
                        longitude = location.second,
                        hour = currentHour,
                        timestamp = System.currentTimeMillis()
                    )

                    val alert = processor.process(sensorInput, emptyList())

                    if (alert != null) {

                        val payload = hashMapOf(
                            "lat" to alert.lat,
                            "lng" to alert.lng,
                            "riskScore" to alert.riskScore,
                            "triggerType" to alert.triggerType,
                            "mode" to alert.mode,
                            "timestamp" to alert.timestamp
                        )

                        FirebaseFirestore.getInstance()
                            .collection("alerts")
                            .add(payload)
                            .addOnSuccessListener {
                                println("✅ Alert sent to Firebase")
                            }
                            .addOnFailureListener {
                                println("❌ Failed to send alert")
                            }
                    }                }

                delay(3000)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 🔹 Foreground notification
    private fun startForegroundService() {
        val channelId = "safeguard_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SafeGuard Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SafeGuard Active")
            .setContentText("Monitoring your safety...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }
    // 🔹 Get location
    private suspend fun getLocation(): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val client = LocationServices.getFusedLocationProviderClient(this@SafeGuardService)

            try {
                val location = client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                location?.let { Pair(it.latitude, it.longitude) }

            } catch (e: SecurityException) {
                null
            } catch (e: Exception) {
                null
            }
        }
}