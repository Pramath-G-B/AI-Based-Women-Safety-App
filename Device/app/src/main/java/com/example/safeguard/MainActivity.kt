package com.example.safeguard

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.safeguard.ui.theme.SafeGuardTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import android.content.Intent
class MainActivity : ComponentActivity() {

    private fun checkNotificationPermissionAndStart() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startSafeGuardService()
            }
        } else {
            startSafeGuardService()
        }
    }
    private fun startSafeGuardService() {
        val intent = Intent(this, SafeGuardService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startSafeGuardService()
            } else {
                println("Notification permission denied")
            }
        }
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkNotificationPermissionAndStart()
            } else {
                println("Location permission denied")
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("Service started")
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkNotificationPermissionAndStart()
        }

        val intent = Intent(this, SafeGuardService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        enableEdgeToEdge()

        setContent {
            SafeGuardTheme {

                var resultText by remember {
                    mutableStateOf("Processing...")
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        fetchLocation { lat, lng ->

                            val currentHour = java.util.Calendar.getInstance()
                                .get(java.util.Calendar.HOUR_OF_DAY)

                            val sensorInput = SensorInput(
                                distressAudio = false,
                                impactDetected = false,
                                latitude = lat,
                                longitude = lng,
                                hour = currentHour,
                                timestamp = System.currentTimeMillis()
                            )

                            val processor = SafetyProcessor()

                            val placeTypes = listOf("bar","night_club")

                            val alert = processor.process(sensorInput, placeTypes)
                            resultText = if (alert != null) {
                                "🚨 ALERT GENERATED\nRisk: ${alert.riskScore}\nType: ${alert.triggerType}\nUpdated: ${System.currentTimeMillis()}"
                            } else {
                                "✅ No Alert\nUpdated: ${System.currentTimeMillis()}"
                            }
                        }

                        delay(3000)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SafeGuard",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "System Active",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { }
                        ) {
                            Text("SOS")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation(onLocationReceived: (Double, Double) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                onLocationReceived(location.latitude, location.longitude)
            }
        }
    }
}