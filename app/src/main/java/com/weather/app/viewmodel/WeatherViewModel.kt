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

    private fun cityKey(lat: Double, lon: Double, name: String): String =
        "${String.format("%.4f", lat)},${String.format("%.4f", lon)},$name"

    private val cityLocationKeys = MutableStateFlow<Map<String, String?>>(emptyMap())

    private val repo = WeatherRepository(application)
    private val fusedLocation = LocationServices.getFusedLocationProviderClient(application)
    private var permissionLauncher: ((Array<String>) -> Unit)? = null

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _cityStates = MutableStateFlow<Map<String, WeatherUiState>>(emptyMap())
    val cityStates: StateFlow<Map<String, WeatherUiState>> = _cityStates.asStateFlow()

    // Track current coordinates for saving cities
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _hotCities = MutableStateFlow<List<GeoLocation>>(emptyList())
    val hotCities: StateFlow<List<GeoLocation>> = _hotCities.asStateFlow()

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

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationInternal() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                // 先试 lastKnownLocation（快，无需等待 GPS 锁定）
                val last = fusedLocation.lastLocation.await()
                if (last != null) {
                    loadWeather(last.latitude, last.longitude, "我的位置")
                    repo.saveLastLocation(last.latitude, last.longitude, "我的位置")
                    return@launch
                }
                // fallback: getCurrentLocation，超时 8 秒
                val cts = CancellationTokenSource()
                val location = kotlinx.coroutines.withTimeoutOrNull(8000) {
                    fusedLocation.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).await()
                }
                if (location != null) {
                    loadWeather(location.latitude, location.longitude, "我的位置")
                    repo.saveLastLocation(location.latitude, location.longitude, "我的位置")
                } else {
                    _uiState.value = WeatherUiState.Error("定位失败：系统无位置信息，请检查定位开关或手动选择城市")
                }
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("定位失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun loadCity(lat: Double, lon: Double, name: String, saveAsLast: Boolean = true, locationKey: String? = null) {
        viewModelScope.launch {
            cityLocationKeys.update { it + (cityKey(lat, lon, name) to locationKey) }
            loadWeather(lat, lon, name)
            if (saveAsLast) repo.saveLastLocation(lat, lon, name)
        }
    }

    fun getCityState(lat: Double, lon: Double, name: String): WeatherUiState? {
        return cityStates.value[cityKey(lat, lon, name)]
    }

    fun restoreCurrentCity() {
        // 滑回第0页时，如果当前不是第0页的城市，重新加载上次位置
        val last = lastLocation.value ?: return
        val curState = _uiState.value
        if (curState is WeatherUiState.SuccessXiaomi && curState.cityName == last.third) return
        viewModelScope.launch { loadWeather(last.first, last.second, last.third) }
    }

    fun retry() {
        _uiState.value = WeatherUiState.Idle
    }

    fun clearWeather() {
        _uiState.value = WeatherUiState.Idle
    }

    fun saveCurrentCity() {
        val state = _uiState.value
        val (name, lat, lon) = when (state) {
            is WeatherUiState.SuccessXiaomi -> Triple(state.cityName, currentLat, currentLon)
            is WeatherUiState.Success -> Triple(state.cityName, currentLat, currentLon)
            else -> return
        }
        if (lat == 0.0 && lon == 0.0) return
        viewModelScope.launch {
            repo.saveCity(
                SavedCity(
                    id = "$name:$lat:$lon".hashCode().toLong(),
                    name = name,
                    region = null,
                    country = null,
                    latitude = lat,
                    longitude = lon
                )
            )
        }
    }

    fun removeSavedCity(cityId: Long) {
        viewModelScope.launch { repo.removeCity(cityId) }
    }

    fun removeCurrentCity() {
        val lat = currentLat
        val lon = currentLon
        val state = _uiState.value
        val name = when (state) {
            is WeatherUiState.SuccessXiaomi -> state.cityName
            is WeatherUiState.Success -> state.cityName
            else -> null
        } ?: return
        if (lat == 0.0 && lon == 0.0) return
        val cityId = "$name:$lat:$lon".hashCode().toLong()
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

    fun loadHotCities() {
        viewModelScope.launch {
            repo.getHotCities().fold(
                onSuccess = { _hotCities.value = it },
                onFailure = { _hotCities.value = emptyList() }
            )
        }
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

    private fun isValidLatLon(lat: Double, lon: Double): Boolean {
        if (lat.isNaN() || lon.isNaN()) return false
        if (lat !in -90.0..90.0) return false
        if (lon !in -180.0..180.0) return false
        if (kotlin.math.abs(lat) < 1e-9 && kotlin.math.abs(lon) < 1e-9) return false
        return true
    }

    private suspend fun loadWeather(lat: Double, lon: Double, name: String) {
        val key = cityKey(lat, lon, name)
        if (!isValidLatLon(lat, lon)) {
            val error = WeatherUiState.Error("坐标无效（$lat,$lon），请重新定位或手动选择城市")
            _uiState.value = error
            _cityStates.update { it + (key to error) }
            return
        }
        _uiState.value = WeatherUiState.Loading
        _cityStates.update { it + (key to WeatherUiState.Loading) }
        currentLat = lat
        currentLon = lon
        repo.getXiaomiWeather(lat, lon, cityLocationKeys.value[key]).fold(
            onSuccess = {
                val success = WeatherUiState.SuccessXiaomi(name, it)
                _uiState.value = success
                _cityStates.update { map -> map + (key to success) }
            },
            onFailure = {
                val raw = com.weather.app.data.remote.XiaomiWeatherApi.lastRaw
                val extra = if (!raw.isNullOrBlank()) "\n\n原始响应(截断)：\n$raw" else ""
                val error = WeatherUiState.Error((it.message ?: "网络请求失败") + extra)
                _uiState.value = error
                _cityStates.update { map -> map + (key to error) }
            }
        )
    }
}
