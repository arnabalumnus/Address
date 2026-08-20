package com.example.addressfinder.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.addressfinder.AddressUiState
import com.example.addressfinder.AddressViewModel
import com.example.addressfinder.location.AddressDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(
    viewModel: AddressViewModel = viewModel(),
    onViewSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }

    val settingsResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onLocationSettingsResolutionResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) {
        viewModel.settingsResolutionRequests.collect { request: IntentSenderRequest ->
            settingsResolutionLauncher.launch(request)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.start()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Address Finder") },
                actions = {
                    IconButton(onClick = onViewSaved) {
                        Icon(Icons.Filled.History, contentDescription = "Saved addresses")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AddressUiState.PermissionRequired -> StatusContent(
                    icon = Icons.Filled.LocationOff,
                    title = "Location permission needed",
                    message = "This app needs access to your device location to look up your current address.",
                    actionLabel = "Grant permission",
                    onAction = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                is AddressUiState.CheckingLocationSettings -> LoadingContent("Checking location settings…")

                is AddressUiState.LocationSettingsUnresolvable -> StatusContent(
                    icon = Icons.Filled.LocationOff,
                    title = "Location (GPS) is off",
                    message = "Please enable device location to continue.",
                    actionLabel = "Try again",
                    onAction = { viewModel.retry() }
                )

                is AddressUiState.FetchingLocation -> LoadingContent("Fetching current location…")

                is AddressUiState.ResolvingAddress -> LoadingContent("Resolving address from coordinates…")

                is AddressUiState.Success -> AddressResultContent(
                    details = state.details,
                    onRefresh = { viewModel.retry() },
                    onSave = { viewModel.saveAddress(it) }
                )

                is AddressUiState.Error -> StatusContent(
                    icon = Icons.Filled.LocationOff,
                    title = "Something went wrong",
                    message = state.message,
                    actionLabel = "Retry",
                    onAction = { viewModel.retry() }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatusContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

/**
 * String-backed mirror of [AddressDetails] so every field can sit in an
 * editable [OutlinedTextField] and be typed into freely before saving.
 */
private data class EditableAddressFields(
    val latitude: String,
    val longitude: String,
    val accuracyMeters: String,
    val fullAddressLine: String,
    val featureName: String,
    val thoroughfare: String,
    val subLocality: String,
    val locality: String,
    val subAdminArea: String,
    val adminArea: String,
    val postalCode: String,
    val countryName: String,
    val countryCode: String
)

private fun AddressDetails.toEditableFields() = EditableAddressFields(
    latitude = latitude.toString(),
    longitude = longitude.toString(),
    accuracyMeters = accuracyMeters?.toString().orEmpty(),
    fullAddressLine = fullAddressLine.orEmpty(),
    featureName = featureName.orEmpty(),
    thoroughfare = thoroughfare.orEmpty(),
    subLocality = subLocality.orEmpty(),
    locality = locality.orEmpty(),
    subAdminArea = subAdminArea.orEmpty(),
    adminArea = adminArea.orEmpty(),
    postalCode = postalCode.orEmpty(),
    countryName = countryName.orEmpty(),
    countryCode = countryCode.orEmpty()
)

/** [original] supplies the raw GPS fix values ([AddressDetails.subThoroughfare]) that aren't user-editable. */
private fun EditableAddressFields.toAddressDetails(original: AddressDetails) = AddressDetails(
    latitude = latitude.toDoubleOrNull() ?: original.latitude,
    longitude = longitude.toDoubleOrNull() ?: original.longitude,
    accuracyMeters = accuracyMeters.toFloatOrNull(),
    fullAddressLine = fullAddressLine.ifBlank { null },
    featureName = featureName.ifBlank { null },
    subThoroughfare = original.subThoroughfare,
    thoroughfare = thoroughfare.ifBlank { null },
    subLocality = subLocality.ifBlank { null },
    locality = locality.ifBlank { null },
    subAdminArea = subAdminArea.ifBlank { null },
    adminArea = adminArea.ifBlank { null },
    postalCode = postalCode.ifBlank { null },
    countryName = countryName.ifBlank { null },
    countryCode = countryCode.ifBlank { null }
)

@Composable
private fun AddressResultContent(
    details: AddressDetails,
    onRefresh: () -> Unit,
    onSave: (AddressDetails) -> Unit
) {
    var fields by remember(details) { mutableStateOf(details.toEditableFields()) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Current Address", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Edit any field below if it isn't quite right, then tap Save.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                item {
                    AddressField("Latitude", fields.latitude) { fields = fields.copy(latitude = it) }
                }
                item {
                    AddressField("Longitude", fields.longitude) { fields = fields.copy(longitude = it) }
                }
                item {
                    AddressField("Accuracy (m)", fields.accuracyMeters) { fields = fields.copy(accuracyMeters = it) }
                }
                item {
                    AddressField("Full address", fields.fullAddressLine) { fields = fields.copy(fullAddressLine = it) }
                }
                item {
                    AddressField("Feature", fields.featureName) { fields = fields.copy(featureName = it) }
                }
                item {
                    AddressField("Street", fields.thoroughfare) { fields = fields.copy(thoroughfare = it) }
                }
                item {
                    AddressField("Sub-locality", fields.subLocality) { fields = fields.copy(subLocality = it) }
                }
                item {
                    AddressField("City", fields.locality) { fields = fields.copy(locality = it) }
                }
                item {
                    AddressField("District", fields.subAdminArea) { fields = fields.copy(subAdminArea = it) }
                }
                item {
                    AddressField("State/Region", fields.adminArea) { fields = fields.copy(adminArea = it) }
                }
                item {
                    AddressField("Postal code", fields.postalCode) { fields = fields.copy(postalCode = it) }
                }
                item {
                    AddressField("Country", fields.countryName) { fields = fields.copy(countryName = it) }
                }
                item {
                    AddressField("Country code", fields.countryCode) { fields = fields.copy(countryCode = it) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                Text("Refresh")
            }
            Button(
                onClick = { onSave(fields.toAddressDetails(original = details)) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun AddressField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}
