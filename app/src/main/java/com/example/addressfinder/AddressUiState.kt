package com.example.addressfinder

import com.example.addressfinder.location.AddressDetails

sealed interface AddressUiState {
    data object PermissionRequired : AddressUiState
    data object CheckingLocationSettings : AddressUiState
    data object LocationSettingsUnresolvable : AddressUiState
    data object FetchingLocation : AddressUiState
    data object ResolvingAddress : AddressUiState
    data class Success(val details: AddressDetails) : AddressUiState
    data class Error(val message: String) : AddressUiState
}
