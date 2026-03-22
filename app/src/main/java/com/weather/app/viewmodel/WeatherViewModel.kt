package com.weather.app.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.weather.app.data.model.*
import com.weather.app.data.repository.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(
        val cityName: String,
        val forecast: ForecastResponse
    ) : WeatherUiState

    data class SuccessXiaomi(
        val cityName: String,
        val weather: XiaomiWeatherResponse
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

sealed interface SearchState {
    object Idle : SearchState
    object Loading : SearchState
    data class Results(val locations: List<GeoLocation>) : SearchState
    data class Error(val message: String) : SearchState
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = WeatherRepository(application)
    private val fusedLocation = LocationServices.getFusedLocationProviderClient(application)
    private var permissionLauncher: ((Array<String>) -> Unit)? = null

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val savedCities: StateFlow<List<SavedCity>> = repo.savedCities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastLocation: StateFlow<Triple<Double, Double, String>?> = repo.lastLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setLocationPermissionLauncher(launcher: (Array<String>) -> Unit) {
        permissionLauncher = launcher
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
        if (granted) fetchCurrentLocation()
        else _uiState.value = WeatherUiState.Error("未授予定位权限，可选择城市继续使用")
    }

    fun requestLocationPermission() {
        permissionLauncher?.invoke(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    fun requestOrFetchCurrentLocation() {
        if (!_permissionGranted.value) {
            requestLocationPermission()
            return
        }
        fetchCurrentLocationInternal()
    }

    fun fetchCurrentLocation() {
        // keep old API but make it safe on HyperOS revoke
        requestOrFetchCurrentLocation()
    }

    private fun fetchCurrentLocationInternal() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                // Hard timeout to avoid endless spinner
                val cts = CancellationTokenSource()
                val location = kotlinx.coroutines.withTimeoutOrNull(4000) {
                    fusedLocation.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).await()
                }
                if (location != null) {
                    loadWeather(location.latitude, location.longitude, "我的位置")
                    repo.saveLastLocation(location.latitude, location.longitude, "我的位置")
                } else {
                    _uiState.value = WeatherUiState.Error("定位超时或不可用，请打开系统定位或手动选择城市")
                }
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("定位失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun loadCity(lat: Double, lon: Double, name: String) {
        viewModelScope.launch {
            loadWeather(lat, lon, name)
            repo.saveLastLocation(lat, lon, name)
        }
    }

    fun retry() {
        _uiState.value = WeatherUiState.Idle
    }

    fun clearWeather() {
        _uiState.value = WeatherUiState.Idle
    }

    fun saveCurrentCity() {
        val state = _uiState.value
        if (state is WeatherUiState.Success) {
            viewModelScope.launch {
                // NOTE: placeholder save for now; proper city saving will be wired from Search results
                repo.saveCity(
                    SavedCity(
                        id = state.cityName.hashCode().toLong(),
                        name = state.cityName,
                        region = null,
                        country = null,
                        latitude = 0.0,
                        longitude = 0.0
                    )
                )
            }
        }
    }

    fun removeSavedCity(cityId: Long) {
        viewModelScope.launch { repo.removeCity(cityId) }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        val q = query.trim()
        if (q.length >= 2) searchCityVariants(q)
        else _searchState.value = SearchState.Idle
    }

    private fun searchCityVariants(query: String) {
        // Try common CN suffix variants to improve county-level hit rate
        val variants = linkedSetOf(
            query,
            query.replace(" ", ""),
            query.removeSuffix("县"),
            query.removeSuffix("市"),
            query.removeSuffix("区"),
        ).filter { it.length >= 2 }

        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            val all = mutableListOf<GeoLocation>()
            var lastErr: Throwable? = null
            for (v in variants) {
                val res = repo.searchCity(v)
                res.fold(
                    onSuccess = { all.addAll(it) },
                    onFailure = { lastErr = it }
                )
                if (all.isNotEmpty()) break
            }
            if (all.isNotEmpty()) {
                _searchState.value = SearchState.Results(all.distinctBy { it.id })
            } else {
                _searchState.value = SearchState.Error(lastErr?.message ?: "没有搜到结果")
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchState.value = SearchState.Idle
    }

    private fun searchCity(query: String) {
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            repo.searchCity(query).fold(
                onSuccess = { _searchState.value = if (it.isEmpty()) SearchState.Results(emptyList()) else SearchState.Results(it) },
                onFailure = { _searchState.value = SearchState.Error(it.message ?: "Search error") }
            )
        }
    }

    private suspend fun loadWeather(lat: Double, lon: Double, name: String) {
        _uiState.value = WeatherUiState.Loading
        repo.getXiaomiWeather(lat, lon).fold(
            onSuccess = { _uiState.value = WeatherUiState.SuccessXiaomi(name, it) },
            onFailure = { _uiState.value = WeatherUiState.Error(it.message ?: "网络请求失败") }
        )
    }
}
