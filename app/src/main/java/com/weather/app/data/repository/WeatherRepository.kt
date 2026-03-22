package com.weather.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weather.app.data.model.*
import com.weather.app.data.remote.WeatherApi
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
        WeatherApi.searchCity(query).results ?: emptyList()
    }

    suspend fun getForecast(lat: Double, lon: Double): Result<ForecastResponse> = runCatching {
        WeatherApi.getForecast(lat, lon)
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
    val longitude: Double
)

fun SavedCity.toDto() = SavedCityDto(id, name, region, country, latitude, longitude)
fun SavedCityDto.toSavedCity() = SavedCity(id, name, region, country, latitude, longitude)
