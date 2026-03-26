package com.example.safeguard

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private var autoAlertJob: Job? = null
    private var monitoringStarted = false

    private var autoAlertSent = false
    private var lastAutoAlertTime = 0L

    companion object {
        private const val AUTO_ALERT_COOLDOWN_MS = 30_000L
    }

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
        if (monitoringStarted) return START_STICKY
        monitoringStarted = true

        scope.launch {
            while (true) {
                val location = getLocation()

                if (location != null) {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
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

                    val now = System.currentTimeMillis()
                    val inCooldown = (now - lastAutoAlertTime) < AUTO_ALERT_COOLDOWN_MS

                    if (evaluation.shouldTrigger && autoAlertJob == null && !inCooldown && !autoAlertSent) {
                        val initialTriggerType = resolveTriggerType(
                            rawTriggerType = evaluation.triggerType,
                            audioLevel = sensorInput.audioLevel,
                            impactLevel = sensorInput.impactLevel
                        )

                        autoAlertJob = scope.launch {
                            try {
                                AutoAlertState.show(initialTriggerType, 5)

                                for (sec in 5 downTo 1) {
                                    AutoAlertState.updateSeconds(sec)

                                    if (AutoAlertState.state.value.cancelRequested) {
                                        println("❌ Auto alert cancelled by user")
                                        return@launch
                                    }

                                    delay(1000)
                                }

                                val latestLocation = getLocation()
                                if (latestLocation == null) {
                                    println("❌ Could not get location for alert")
                                    return@launch
                                }

                                val latestInput = SensorInput(
                                    audioLevel = audioDetector.audioLevel,
                                    impactLevel = impactDetector.impactLevel,
                                    latitude = latestLocation.first,
                                    longitude = latestLocation.second,
                                    hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                                    timestamp = System.currentTimeMillis()
                                )

                                val latestEval = processor.evaluate(latestInput, placeTypes)

                                val finalTriggerType = resolveTriggerType(
                                    rawTriggerType = latestEval.triggerType,
                                    audioLevel = latestInput.audioLevel,
                                    impactLevel = latestInput.impactLevel,
                                    fallback = initialTriggerType
                                )

                                val userId = UserSessionManager.getUserId(this@SafeGuardService)

                                val payload = hashMapOf(
                                    "userId" to userId,
                                    "lat" to latestInput.latitude,
                                    "lng" to latestInput.longitude,
                                    "riskScore" to latestEval.riskScore,
                                    "triggerType" to finalTriggerType,
                                    "mode" to latestEval.mode.name,
                                    "timestamp" to latestInput.timestamp,
                                    "audioLevel" to latestInput.audioLevel,
                                    "impactLevel" to latestInput.impactLevel
                                )

                                try {
                                    FirebaseFirestore.getInstance()
                                        .collection("alerts")
                                        .add(payload)
                                        .await()

                                    println("🔥 ALERT SENT to Firebase")
                                    autoAlertSent = true
                                    lastAutoAlertTime = System.currentTimeMillis()
                                } catch (ex: Exception) {
                                    println("❌ Failed to send alert: ${ex.message}")
                                }
                            } finally {
                                AutoAlertState.hide()
                                autoAlertJob = null
                            }
                        }
                    }

                    if (!evaluation.shouldTrigger && autoAlertJob == null) {
                        autoAlertSent = false
                    }
                }

                delay(1000)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        autoAlertJob?.cancel()
        AutoAlertState.hide()
        impactDetector.stop()
        audioDetector.stop()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun resolveTriggerType(
        rawTriggerType: String?,
        audioLevel: Int,
        impactLevel: Int,
        fallback: String? = null
    ): String {
        val cleaned = rawTriggerType?.trim()?.uppercase()

        if (!cleaned.isNullOrBlank() && cleaned != "UNKNOWN" && cleaned != "NONE") {
            return cleaned
        }

        return when {
            audioLevel > 0 && impactLevel > 0 -> "AUDIO_IMPACT"
            impactLevel > 0 -> "IMPACT"
            audioLevel > 0 -> "AUDIO"
            !fallback.isNullOrBlank() -> fallback
            else -> "AUTO"
        }
    }

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
            val fineGranted = ContextCompat.checkSelfPermission(
                this@SafeGuardService,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseGranted = ContextCompat.checkSelfPermission(
                this@SafeGuardService,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!fineGranted && !coarseGranted) {
                return@withContext null
            }

            val client = LocationServices.getFusedLocationProviderClient(this@SafeGuardService)

            try {
                val location = client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                location?.let { Pair(it.latitude, it.longitude) }
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            }
        }
}