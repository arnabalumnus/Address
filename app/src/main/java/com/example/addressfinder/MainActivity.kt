package com.example.addressfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.addressfinder.ui.AddressScreen
import com.example.addressfinder.ui.SavedAddressesScreen
import com.example.addressfinder.ui.theme.AddressFinderTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_SAVED = "saved"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AddressFinderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AddressFinderApp()
                }
            }
        }
    }
}

@Composable
private fun AddressFinderApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            AddressScreen(onViewSaved = { navController.navigate(ROUTE_SAVED) })
        }
        composable(ROUTE_SAVED) {
            SavedAddressesScreen(onBack = { navController.popBackStack() })
        }
    }
}
