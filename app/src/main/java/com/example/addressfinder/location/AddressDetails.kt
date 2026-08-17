package com.example.addressfinder.location

import android.location.Address

/**
 * Flattened, UI/Logcat-friendly view of the fields we care about from
 * [android.location.Address] plus the raw coordinates that produced it.
 */
data class AddressDetails(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val fullAddressLine: String?,
    val featureName: String?,
    val subThoroughfare: String?,
    val thoroughfare: String?,
    val subLocality: String?,
    val locality: String?,
    val subAdminArea: String?,
    val adminArea: String?,
    val postalCode: String?,
    val countryName: String?,
    val countryCode: String?
) {
    companion object {
        fun from(latitude: Double, longitude: Double, accuracyMeters: Float?, address: Address): AddressDetails {
            val lines = (0..address.maxAddressLineIndex)
                .mapNotNull { address.getAddressLine(it) }
            return AddressDetails(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters,
                fullAddressLine = lines.joinToString(separator = ", ").ifBlank { null },
                featureName = address.featureName,
                subThoroughfare = address.subThoroughfare,
                thoroughfare = address.thoroughfare,
                subLocality = address.subLocality,
                locality = address.locality,
                subAdminArea = address.subAdminArea,
                adminArea = address.adminArea,
                postalCode = address.postalCode,
                countryName = address.countryName,
                countryCode = address.countryCode
            )
        }
    }
}
