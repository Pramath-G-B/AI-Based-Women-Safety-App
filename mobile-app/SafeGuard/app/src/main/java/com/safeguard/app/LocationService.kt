package com.safeguard.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log

class LocationService(context: Context) {

    companion object {
        private const val TAG = "LocationService"
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Volatile
    var latitude: Double = 0.0
        private set

    @Volatile
    var longitude: Double = 0.0
        private set

    @Volatile
    var hasLocation: Boolean = false
        private set

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            latitude = location.latitude
            longitude = location.longitude
            hasLocation = true
            Log.d(TAG, "Real location received: $latitude, $longitude")
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                3000L,
                0f,
                locationListener
            )
            Log.d(TAG, "Started location tracking with LocationManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location tracking: ${e.message}")
        }
    }

    fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
            Log.d(TAG, "Stopped location tracking")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop location tracking: ${e.message}")
        }
    }
}