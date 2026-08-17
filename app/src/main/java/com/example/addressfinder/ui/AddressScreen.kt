package com.example.addressfinder.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun AddressScreen(viewModel: AddressViewModel = viewModel()) {
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
            TopAppBar(title = { Text("Address Finder") })
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
                    onRefresh = { viewModel.retry() }
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

@Composable
private fun AddressResultContent(details: AddressDetails, onRefresh: () -> Unit) {
    val rows = listOfNotNull(
        "Latitude" to details.latitude.toString(),
        "Longitude" to details.longitude.toString(),
        details.accuracyMeters?.let { "Accuracy" to "${it} m" },
        details.fullAddressLine?.let { "Full address" to it },
        details.featureName?.let { "Feature" to it },
        details.thoroughfare?.let { "Street" to it },
        details.subLocality?.let { "Sub-locality" to it },
        details.locality?.let { "City" to it },
        details.subAdminArea?.let { "District" to it },
        details.adminArea?.let { "State/Region" to it },
        details.postalCode?.let { "Postal code" to it },
        details.countryName?.let { "Country" to "$it (${details.countryCode ?: "-"})" }
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Current Address", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                items(rows) { (label, value) ->
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(value, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh")
        }
    }
}
