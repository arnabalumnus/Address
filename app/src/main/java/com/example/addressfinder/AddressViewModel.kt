package com.example.addressfinder

import android.app.Application
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.addressfinder.data.AppDatabase
import com.example.addressfinder.data.toEntity
import com.example.addressfinder.location.AddressDetails
import com.example.addressfinder.location.LocationRepository
import com.google.android.gms.common.api.ResolvableApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddressViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocationRepository(application)
    private val addressDao = AppDatabase.getInstance(application).addressDao()

    private val _uiState = MutableStateFlow<AddressUiState>(AddressUiState.PermissionRequired)
    val uiState: StateFlow<AddressUiState> = _uiState.asStateFlow()

    /** One-shot events the UI must launch through an ActivityResult contract. */
    private val _settingsResolutionRequests = MutableSharedFlow<IntentSenderRequest>(extraBufferCapacity = 1)
    val settingsResolutionRequests = _settingsResolutionRequests

    /** Call once when the screen appears; resumes the flow if permission is already granted. */
    fun start() {
        if (repository.hasLocationPermission()) {
            checkLocationSettingsAndFetch()
        } else {
            _uiState.value = AddressUiState.PermissionRequired
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            checkLocationSettingsAndFetch()
        } else {
            AddressLogger.logInfo("Location permission denied by user")
            _uiState.value = AddressUiState.Error("Location permission is required to fetch your address.")
        }
    }

    fun onLocationSettingsResolutionResult(enabled: Boolean) {
        if (enabled) {
            fetchLocationAndResolveAddress()
        } else {
            AddressLogger.logInfo("User declined to enable device location (GPS)")
            _uiState.value = AddressUiState.LocationSettingsUnresolvable
        }
    }

    fun retry() {
        if (!repository.hasLocationPermission()) {
            _uiState.value = AddressUiState.PermissionRequired
            return
        }
        checkLocationSettingsAndFetch()
    }

    private fun checkLocationSettingsAndFetch() {
        _uiState.value = AddressUiState.CheckingLocationSettings
        viewModelScope.launch {
            try {
                repository.checkLocationSettings()
                fetchLocationAndResolveAddress()
            } catch (resolvable: ResolvableApiException) {
                val request = IntentSenderRequest.Builder(resolvable.resolution).build()
                _settingsResolutionRequests.emit(request)
            } catch (t: Throwable) {
                AddressLogger.logError("Device location settings are not satisfiable", t)
                _uiState.value = AddressUiState.LocationSettingsUnresolvable
            }
        }
    }

    private fun fetchLocationAndResolveAddress() {
        _uiState.value = AddressUiState.FetchingLocation
        viewModelScope.launch {
            try {
                val location = repository.getCurrentLocation()
                AddressLogger.logInfo(
                    "Location fix obtained: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m"
                )

                _uiState.value = AddressUiState.ResolvingAddress
                val address = repository.reverseGeocode(location.latitude, location.longitude)
                val details = AddressDetails.from(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    address = address
                )

                AddressLogger.logAddressDetails(details)
                _uiState.value = AddressUiState.Success(details)
            } catch (t: Throwable) {
                AddressLogger.logError("Failed to resolve address from location", t)
                _uiState.value = AddressUiState.Error(t.message ?: "Unable to determine your address.")
            }
        }
    }

    /** Called from the UI's Save button; the user may have hand-edited [details] first. */
    fun saveAddress(details: AddressDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            val rowId = addressDao.insert(details.toEntity())
            AddressLogger.logInfo("Saved address to database (row id=$rowId)")
        }
    }
}
