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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class SafeGuardService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private lateinit var processor: SafetyProcessor
    private lateinit var impactDetector: ImpactDetector
    private lateinit var audioDetector: AudioDistressDetector

    override fun onCreate() {
        super.onCreate()

        processor = SafetyProcessor()

        impactDetector = ImpactDetector(this)
        impactDetector.start()

        audioDetector = AudioDistressDetector()
        audioDetector.start()

        startMyForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            while (true) {
                val location = getLocation()

                if (location != null) {
                    val currentHour = Calendar.getInstance()
                        .get(Calendar.HOUR_OF_DAY)

                    val placeTypes = listOf("bar", "night_club")

                    val sensorInput = SensorInput(
                        audioLevel = audioDetector.audioLevel,
                        impactLevel = impactDetector.impactLevel,
                        latitude = location.first,
                        longitude = location.second,
                        hour = currentHour,
                        timestamp = System.currentTimeMillis()
                    )

                    val evaluation = processor.evaluate(sensorInput, placeTypes)

                    MonitorState.update(
                        LiveMonitorData(
                            audioLevel = sensorInput.audioLevel,
                            impactLevel = sensorInput.impactLevel,
                            risk = evaluation.riskScore,
                            mode = evaluation.mode.name,
                            trigger = evaluation.shouldTrigger,
                            triggerType = evaluation.triggerType,
                            lat = sensorInput.latitude,
                            lng = sensorInput.longitude
                        )
                    )

                    if (evaluation.shouldTrigger) {
                        val payload = hashMapOf(
                            "lat" to sensorInput.latitude,
                            "lng" to sensorInput.longitude,
                            "riskScore" to evaluation.riskScore,
                            "triggerType" to evaluation.triggerType,
                            "mode" to evaluation.mode.name,
                            "timestamp" to sensorInput.timestamp,
                            "audioLevel" to sensorInput.audioLevel,
                            "impactLevel" to sensorInput.impactLevel
                        )

                        try {
                            FirebaseFirestore.getInstance()
                                .collection("alerts")
                                .add(payload)
                                .await()
                            println("✅ Alert sent to Firebase")
                        } catch (e: Exception) {
                            println("❌ Failed to send alert: ${e.message}")
                        }
                    }
                }

                delay(3000)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        impactDetector.stop()
        audioDetector.stop()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMyForegroundNotification() {
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