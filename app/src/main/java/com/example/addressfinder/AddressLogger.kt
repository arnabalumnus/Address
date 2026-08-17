package com.example.addressfinder

import android.util.Log
import com.example.addressfinder.location.AddressDetails

/**
 * Prints the resolved address in a clearly delimited, easy-to-spot block so it
 * stands out while scrolling Logcat. Filter on tag "AddressFinder" to isolate it.
 */
object AddressLogger {
    const val TAG = "AddressFinder"
    private val DIVIDER = "=".repeat(64)

    fun logAddressDetails(details: AddressDetails) {
        Log.i(TAG, DIVIDER)
        Log.i(TAG, "★★★ ADDRESS RESOLVED ★★★")
        Log.i(TAG, DIVIDER)
        Log.i(TAG, "Latitude         : ${details.latitude}")
        Log.i(TAG, "Longitude        : ${details.longitude}")
        Log.i(TAG, "Accuracy (m)     : ${details.accuracyMeters ?: "-"}")
        Log.i(TAG, "Full address     : ${details.fullAddressLine ?: "-"}")
        Log.i(TAG, "Feature name     : ${details.featureName ?: "-"}")
        Log.i(TAG, "Sub-thoroughfare : ${details.subThoroughfare ?: "-"}")
        Log.i(TAG, "Thoroughfare     : ${details.thoroughfare ?: "-"}")
        Log.i(TAG, "Sub-locality     : ${details.subLocality ?: "-"}")
        Log.i(TAG, "Locality         : ${details.locality ?: "-"}")
        Log.i(TAG, "Sub-admin area   : ${details.subAdminArea ?: "-"}")
        Log.i(TAG, "Admin area       : ${details.adminArea ?: "-"}")
        Log.i(TAG, "Postal code      : ${details.postalCode ?: "-"}")
        Log.i(TAG, "Country          : ${details.countryName ?: "-"} (${details.countryCode ?: "-"})")
        Log.i(TAG, DIVIDER)
    }

    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, ">>> $message", throwable)
    }

    fun logInfo(message: String) {
        Log.i(TAG, message)
    }
}
