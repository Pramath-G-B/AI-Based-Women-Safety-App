package com.safeguard.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
private lateinit var locationService: LocationService
class SafeGuardService : Service() {

    companion object {
        const val CHANNEL_ID = "safeguard_channel"
        const val NOTIFICATION_ID = 1001
        const val TAG = "SafeGuardService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "✅ Service Created")
        locationService = LocationService(this)

        if (PermissionHelper.hasLocationPermission(this)) {
            locationService.startTracking()
        } else {
            Log.w(TAG, "Location permission not granted")
        }
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "✅ Service Started!")

        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 🔥 ADD THIS PART HERE
        Thread {
            while (true) {
                if (locationService.hasLocation) {
                    Log.d(TAG, "📍 LAT: ${locationService.latitude}, LNG: ${locationService.longitude}")
                } else {
                    Log.d(TAG, "⌛ Waiting for location...")
                }

                Thread.sleep(3000)
            }
        }.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "🛑 Service Destroyed")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SafeGuard Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps SafeGuard running in the background"
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeGuard is Active")
            .setContentText("Your protection is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}