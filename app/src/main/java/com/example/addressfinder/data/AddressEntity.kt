package com.example.addressfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.addressfinder.location.AddressDetails

/**
 * Room row for one resolved address lookup: an id, the moment it was
 * resolved, and every field from [AddressDetails] flattened out.
 */
@Entity(tableName = "saved_addresses")
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
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
)

fun AddressDetails.toEntity(timestamp: Long = System.currentTimeMillis()): AddressEntity = AddressEntity(
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    fullAddressLine = fullAddressLine,
    featureName = featureName,
    subThoroughfare = subThoroughfare,
    thoroughfare = thoroughfare,
    subLocality = subLocality,
    locality = locality,
    subAdminArea = subAdminArea,
    adminArea = adminArea,
    postalCode = postalCode,
    countryName = countryName,
    countryCode = countryCode
)
