package com.example.addressfinder.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Wraps the Play Services fused location APIs and platform [Geocoder] behind a
 * small suspend-function surface so the ViewModel doesn't have to juggle callbacks.
 */
class LocationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(appContext)
    private val geocoder = Geocoder(appContext, Locale.getDefault())

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun buildLocationRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).build()

    /**
     * Asks Play Services whether the device's current location settings (GPS enabled,
     * mode, etc.) satisfy [buildLocationRequest]. Throws [com.google.android.gms.common.api.ResolvableApiException]
     * when the user can fix it via a system dialog - the caller should catch that and
     * launch its `.resolution` IntentSender.
     */
    suspend fun checkLocationSettings() {
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(buildLocationRequest())
            .setAlwaysShow(true)
            .build()
        settingsClient.checkLocationSettings(request).await()
    }

    suspend fun getCurrentLocation(): Location {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()
        return fusedLocationClient.getCurrentLocation(request, null).await()
            ?: error("Location unavailable - no fix could be obtained")
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): Address {
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reverseGeocodeAsync(latitude, longitude)
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)
        }
        return addresses?.firstOrNull()
            ?: error("No address found for ($latitude, $longitude)")
    }

    private suspend fun reverseGeocodeAsync(latitude: Double, longitude: Double): List<Address>? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(latitude, longitude, 1) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }
}
