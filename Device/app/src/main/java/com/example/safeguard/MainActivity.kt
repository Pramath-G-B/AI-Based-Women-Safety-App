package com.example.safeguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.safeguard.ui.theme.SafeGuardTheme

class MainActivity : ComponentActivity() {

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) checkNotificationPermissionAndStart()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startSafeGuardService()
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) checkAudioPermissionAndProceed()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkLocationPermissionAndProceed()

        setContent {
            val userId = UserSessionManager.getUserId(this)

            SafeGuardTheme {
                val liveState by MonitorState.state.collectAsState()
                val autoAlertState by AutoAlertState.state.collectAsState()

                val hasAlert = liveState.trigger
                val alertText = if (hasAlert) {
                    "⚠️ Alert: ${liveState.triggerType}"
                } else {
                    "✅ No Active Alerts"
                }

                if (autoAlertState.isVisible) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = {
                            Text(
                                text = "Automatic Alert Detected",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B2E5D)
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Trigger: ${autoAlertState.triggerType}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Emergency alert will be sent in ${autoAlertState.secondsLeft} seconds.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap cancel to stop it.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { AutoAlertState.requestCancel() }
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFFC8223B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFFFEEF5),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { },
                            containerColor = Color(0xFFF4A8C2),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            Text(
                                text = "🛡👩",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFEEF5))
                            .padding(innerPadding)
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "🛡👩 SafeGuard",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8B2E5D)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "System Active",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF4A2B39),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(0xFF58D06B), CircleShape)
                                    .shadow(8.dp, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                        ) {
                            Text(
                                text = "👤  USER ID: $userId",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF202020),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFFE3A1BC),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAFC)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 18.dp)
                            ) {
                                Text(
                                    text = "CURRENT READINGS",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                ReadingRow("🎤", "AUDIO", liveState.audioLevel.toString())
                                Spacer(modifier = Modifier.height(12.dp))
                                ReadingRow("💥", "IMPACT", liveState.impactLevel.toString())
                                Spacer(modifier = Modifier.height(12.dp))
                                ReadingRow("📊", "RISK LEVEL", liveState.risk.toString())
                                Spacer(modifier = Modifier.height(12.dp))
                                ReadingRow("⚙", "OPERATING MODE", liveState.mode)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 18.dp)
                            ) {
                                Text(
                                    text = "DEVICE LOCATION",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                ReadingRow("📍", "LATITUDE", liveState.lat.toString())
                                Spacer(modifier = Modifier.height(12.dp))
                                ReadingRow("📍", "LONGITUDE", liveState.lng.toString())
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (hasAlert) Color(0xFFFFDDE3) else Color(0xFFFDE7F0),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = alertText,
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 18.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (hasAlert) Color(0xFFB3261E) else Color(0xFF8B2E5D),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                FirebaseAlertHelper.sendManualSosAlert(
                                    context = this@MainActivity,
                                    lat = liveState.lat,
                                    lng = liveState.lng,
                                    onSuccess = { },
                                    onFailure = { }
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC8223B),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .width(160.dp)
                                .height(58.dp)
                        ) {
                            Text(
                                text = "SOS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }
    }

    private fun checkLocationPermissionAndProceed() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkAudioPermissionAndProceed()
        }
    }

    private fun checkAudioPermissionAndProceed() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            checkNotificationPermissionAndStart()
        }
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (
                ContextCompat.checkSelfPermission(
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun ReadingRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1F1F1F),
            fontWeight = FontWeight.Medium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}