package com.weather.app.data.remote

import com.weather.app.data.model.ForecastResponse
import com.weather.app.data.model.GeocodingResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object WeatherApi {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    private const val GEO_BASE = "https://geocoding-api.open-meteo.com/v1"
    private const val FORECAST_BASE = "https://api.open-meteo.com/v1"

    suspend fun searchCity(query: String): GeocodingResponse =
        client.get("$GEO_BASE/search") {
            parameter("name", query)
            parameter("count", 10)
            parameter("language", "zh")
            parameter("format", "json")
        }.body()

    suspend fun getForecast(lat: Double, lon: Double): ForecastResponse =
        client.get("$FORECAST_BASE/forecast") {
            parameter("latitude", lat)
            parameter("longitude", lon)
            parameter("current", "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,uv_index,is_day")
            parameter("hourly", "temperature_2m,weather_code,precipitation_probability")
            parameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
            parameter("forecast_days", 7)
            parameter("timezone", "auto")
        }.body()
}
