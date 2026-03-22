package com.weather.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.runtime.*
import com.weather.app.ui.screens.SearchScreen
import com.weather.app.ui.screens.WeatherScreen
import com.weather.app.ui.theme.WeatherTheme
import com.weather.app.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.setLocationPermissionLauncher {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent {
            WeatherTheme {
                var showSearch by remember { mutableStateOf(false) }
                AnimatedContent(
                    targetState = showSearch,
                    transitionSpec = {
                        if (targetState) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "screen_transition"
                ) { isSearch ->
                    if (isSearch) {
                        SearchScreen(
                            viewModel = viewModel,
                            onBack = { showSearch = false }
                        )
                    } else {
                        WeatherScreen(
                            viewModel = viewModel,
                            onSearchClick = { showSearch = true }
                        )
                    }
                }
            }
        }
    }
}
