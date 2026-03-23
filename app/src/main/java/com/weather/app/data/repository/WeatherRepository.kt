package com.weather.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weather.app.data.model.*
import com.weather.app.data.remote.WeatherApi
import com.weather.app.data.remote.XiaomiWeatherApi
import com.weather.app.data.remote.XiaomiLocationApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_prefs")

class WeatherRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val KEY_SAVED_CITIES = stringPreferencesKey("saved_cities")
        val KEY_LAST_LAT = stringPreferencesKey("last_lat")
        val KEY_LAST_LON = stringPreferencesKey("last_lon")
        val KEY_LAST_CITY_NAME = stringPreferencesKey("last_city_name")
    }

    // ── Network ───────────────────────────────────────────────────────────

    suspend fun searchCity(query: String): Result<List<GeoLocation>> = runCatching {
        val items = com.weather.app.data.remote.XiaomiLocationApi.searchCity(query)
        items
            .filter { (it.latitude?.toDoubleOrNull() ?: 0.0) != 0.0 && (it.longitude?.toDoubleOrNull() ?: 0.0) != 0.0 }
            .map { it.toGeoLocation(query) }
    }

    suspend fun getHotCities(): Result<List<GeoLocation>> = runCatching {
        com.weather.app.data.remote.XiaomiLocationApi.hotCities()
            .filter { (it.latitude?.toDoubleOrNull() ?: 0.0) != 0.0 && (it.longitude?.toDoubleOrNull() ?: 0.0) != 0.0 }
            .map { it.toGeoLocation(it.name ?: "热门城市") }
    }

    suspend fun getXiaomiWeather(lat: Double, lon: Double, locationKey: String? = null): Result<XiaomiWeatherResponse> = runCatching {
        XiaomiWeatherApi.getAll(lat, lon, locationKey = locationKey)
    }

    // ── Persistence ───────────────────────────────────────────────────────

    val savedCities: Flow<List<SavedCity>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SAVED_CITIES] ?: return@map emptyList()
        runCatching {
            json.decodeFromString<List<SavedCityDto>>(raw).map { it.toSavedCity() }
        }.getOrDefault(emptyList())
    }

    suspend fun saveCity(city: SavedCity) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                val raw = prefs[KEY_SAVED_CITIES] ?: "[]"
                json.decodeFromString<List<SavedCityDto>>(raw)
            }.getOrDefault(emptyList()).toMutableList()
            if (current.none { it.id == city.id }) {
                current.add(0, city.toDto())
                prefs[KEY_SAVED_CITIES] = json.encodeToString(current)
            }
        }
    }

    suspend fun removeCity(cityId: Long) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                val raw = prefs[KEY_SAVED_CITIES] ?: "[]"
                json.decodeFromString<List<SavedCityDto>>(raw)
            }.getOrDefault(emptyList()).toMutableList()
            current.removeAll { it.id == cityId }
            prefs[KEY_SAVED_CITIES] = json.encodeToString(current)
        }
    }

    suspend fun moveCity(cityId: Long, direction: Int) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                val raw = prefs[KEY_SAVED_CITIES] ?: "[]"
                json.decodeFromString<List<SavedCityDto>>(raw)
            }.getOrDefault(emptyList()).toMutableList()

            val fromIndex = current.indexOfFirst { it.id == cityId }
            if (fromIndex < 0) return@edit

            val toIndex = (fromIndex + direction).coerceIn(0, current.lastIndex)
            if (toIndex == fromIndex) return@edit

            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            prefs[KEY_SAVED_CITIES] = json.encodeToString(current)
        }
    }

    suspend fun replaceCities(cities: List<SavedCity>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_CITIES] = json.encodeToString(cities.map { it.toDto() })
        }
    }

    suspend fun saveLastLocation(lat: Double, lon: Double, cityName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_LAT] = lat.toString()
            prefs[KEY_LAST_LON] = lon.toString()
            prefs[KEY_LAST_CITY_NAME] = cityName
        }
    }

    val lastLocation: Flow<Triple<Double, Double, String>?> = context.dataStore.data.map { prefs ->
        val lat = prefs[KEY_LAST_LAT]?.toDoubleOrNull() ?: return@map null
        val lon = prefs[KEY_LAST_LON]?.toDoubleOrNull() ?: return@map null
        val name = prefs[KEY_LAST_CITY_NAME] ?: return@map null
        Triple(lat, lon, name)
    }
}

// ── DTO for serialization ─────────────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class SavedCityDto(
    val id: Long,
    val name: String,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
    val locationKey: String? = null
)

fun SavedCity.toDto() = SavedCityDto(id, name, region, country, latitude, longitude, locationKey)
fun SavedCityDto.toSavedCity() = SavedCity(id, name, region, country, latitude, longitude, locationKey)

private fun com.weather.app.data.model.XiaomiCitySearchItem.toGeoLocation(fallbackName: String) = GeoLocation(
    id = (locationKey ?: key ?: name ?: fallbackName).hashCode().toLong(),
    name = name ?: fallbackName,
    latitude = latitude?.toDoubleOrNull() ?: 0.0,
    longitude = longitude?.toDoubleOrNull() ?: 0.0,
    locationKey = locationKey,
    country = affiliation,
    region = null
)
