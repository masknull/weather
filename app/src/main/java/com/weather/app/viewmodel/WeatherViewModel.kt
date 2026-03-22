package com.weather.app.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.weather.app.data.model.*
import com.weather.app.data.repository.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(
        val cityName: String,
        val forecast: com.weather.app.data.model.ForecastResponse
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

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val savedCities: StateFlow<List<SavedCity>> = repo.savedCities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Restore last viewed city on startup
        viewModelScope.launch {
            repo.lastLocation.firstOrNull()?.let { (lat, lon, name) ->
                loadForecast(lat, lon, name)
            }
        }
        // Debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .filter { it.length >= 2 }
                .collectLatest { query ->
                    _searchState.value = SearchState.Loading
                    repo.searchCity(query).fold(
                        onSuccess = { _searchState.value = SearchState.Results(it) },
                        onFailure = { _searchState.value = SearchState.Error(it.message ?: "Search failed") }
                    )
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) _searchState.value = SearchState.Idle
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchState.value = SearchState.Idle
    }

    fun selectLocation(location: GeoLocation) {
        viewModelScope.launch {
            clearSearch()
            loadForecast(location.latitude, location.longitude, location.displayName)
            repo.saveLastLocation(location.latitude, location.longitude, location.displayName)
        }
    }

    fun selectSavedCity(city: SavedCity) {
        viewModelScope.launch {
            loadForecast(city.latitude, city.longitude, city.name)
            repo.saveLastLocation(city.latitude, city.longitude, city.name)
        }
    }

    fun saveCity(location: GeoLocation) {
        viewModelScope.launch {
            repo.saveCity(location.toSavedCity())
        }
    }

    fun removeCity(cityId: Long) {
        viewModelScope.launch { repo.removeCity(cityId) }
    }

    @SuppressLint("MissingPermission")
    fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val cts = CancellationTokenSource()
                val location: Location = fusedLocation
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .await()
                loadForecast(location.latitude, location.longitude, "Current Location")
                repo.saveLastLocation(location.latitude, location.longitude, "Current Location")
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Could not get location: ${e.message}")
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        if (state is WeatherUiState.Success) {
            viewModelScope.launch {
                repo.lastLocation.firstOrNull()?.let { (lat, lon, name) ->
                    loadForecast(lat, lon, name)
                }
            }
        }
    }

    private suspend fun loadForecast(lat: Double, lon: Double, name: String) {
        _uiState.value = WeatherUiState.Loading
        repo.getForecast(lat, lon).fold(
            onSuccess = { _uiState.value = WeatherUiState.Success(name, it) },
            onFailure = { _uiState.value = WeatherUiState.Error(it.message ?: "Failed to load weather") }
        )
    }
}
