package com.example.addressfinder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.addressfinder.data.AddressEntity
import com.example.addressfinder.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SavedAddressesViewModel(application: Application) : AndroidViewModel(application) {

    private val addressDao = AppDatabase.getInstance(application).addressDao()

    val savedAddresses: StateFlow<List<AddressEntity>> = addressDao.getAllOrderedByTimestampDesc()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
