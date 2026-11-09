package com.ejemplo.laptimer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class GpsLocationManager(
    private val context: Context,
    private val onLocationChanged: (Location) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var internalLocationListener: LocationListener? = null

    fun connect(activity: AppCompatActivity? = null): Boolean {
        if (internalLocationListener != null) return true
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    101
                )
            }
            onStatus("Pidiendo permiso GPS...")
            return false
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                onLocationChanged(loc)
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                onStatus("GPS desactivado en ajustes")
            }
        }
        internalLocationListener = listener

        try {
            var connectedAny = false
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
                    connectedAny = true
                } catch (_: Exception) {}
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
                    connectedAny = true
                } catch (_: Exception) {}
            }
            if (!connectedAny) {
                for (p in locationManager.allProviders) {
                    try {
                        locationManager.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper())
                        connectedAny = true
                    } catch (_: Exception) {}
                }
            }
            if (connectedAny) {
                onStatus("Esperando señal GPS...")
                return true
            } else {
                onStatus("GPS providers disabled")
                return false
            }
        } catch (e: SecurityException) {
            onStatus("Permiso de ubicación denegado")
            return false
        } catch (e: Exception) {
            onStatus("Error GPS: ${e.message}")
            return false
        }
    }

    fun disconnect() {
        internalLocationListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (_: Exception) {}
            internalLocationListener = null
        }
    }
}
