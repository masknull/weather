package com.weather.app

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import com.weather.app.ui.screens.HomeScreen
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
                var route by remember { mutableStateOf("home") }
                val uiState by viewModel.uiState.collectAsState()
                val lastLocation by viewModel.lastLocation.collectAsState(initial = null)

                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        slideInHorizontally { it / 2 } togetherWith slideOutHorizontally { -it / 2 }
                    },
                    label = "route"
                ) { r ->
                    when (r) {
                        "home" -> HomeScreen(
                            message = when (uiState) {
                                is com.weather.app.viewmodel.WeatherUiState.Error -> (uiState as com.weather.app.viewmodel.WeatherUiState.Error).message
                                else -> null
                            },
                            onUseLocation = {
                                viewModel.requestLocationPermission()
                            },
                            onChooseCity = { route = "search" }
                        )
                        "search" -> SearchScreen(
                            viewModel = viewModel,
                            onBack = { route = "weather" }
                        )
                        "weather" -> WeatherScreen(
                            viewModel = viewModel,
                            onSearchClick = { route = "search" }
                        )
                        else -> HomeScreen(
                            onUseLocation = { viewModel.requestLocationPermission() },
                            onChooseCity = { route = "search" }
                        )
                    }
                }

                // System back behavior
                var lastBackPressed by remember { mutableStateOf(0L) }
                BackHandler(enabled = true) {
                    when (route) {
                        "search" -> route = "weather"
                        "weather" -> {
                            val now = System.currentTimeMillis()
                            if (now - lastBackPressed < 2000) {
                                finish()
                            } else {
                                lastBackPressed = now
                                Toast.makeText(this@MainActivity, "再按一次退出", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> finish()
                    }
                }

                LaunchedEffect(lastLocation) {
                    val ll = lastLocation
                    if (ll != null && route == "home") {
                        // restore last city on cold start
                        viewModel.loadCity(ll.first, ll.second, ll.third)
                        route = "weather"
                    }
                }

                LaunchedEffect(uiState) {
                    if (uiState is com.weather.app.viewmodel.WeatherUiState.Success ||
                        uiState is com.weather.app.viewmodel.WeatherUiState.SuccessXiaomi
                    ) {
                        route = "weather"
                    }
                }
            }
        }
    }
}
