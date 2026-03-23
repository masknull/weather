package com.weather.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Geocoding ────────────────────────────────────────────────────────────────

@Serializable
data class GeocodingResponse(
    val results: List<GeoLocation>? = null
)

@Serializable
data class GeoLocation(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val locationKey: String? = null,
    val country: String? = null,
    @SerialName("admin1") val region: String? = null,
    @SerialName("country_code") val countryCode: String? = null
) {
    val displayName: String get() = buildString {
        append(name)
        if (!region.isNullOrBlank()) append(", $region")
        if (!country.isNullOrBlank()) append(", $country")
    }
}

// ── Forecast ─────────────────────────────────────────────────────────────────

@Serializable
data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current") val current: CurrentWeather,
    @SerialName("hourly") val hourly: HourlyWeather,
    @SerialName("daily") val daily: DailyWeather
)

@Serializable
data class CurrentWeather(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val feelsLike: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("uv_index") val uvIndex: Double? = null,
    @SerialName("is_day") val isDay: Int = 1
)

@Serializable
data class HourlyWeather(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("precipitation_probability") val precipProb: List<Int>
)

@Serializable
data class DailyWeather(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val tempMax: List<Double>,
    @SerialName("temperature_2m_min") val tempMin: List<Double>,
    @SerialName("precipitation_probability_max") val precipProb: List<Int>
)

// ── Domain ───────────────────────────────────────────────────────────────────

data class WeatherCondition(
    val code: Int,
    val description: String,
    val icon: String
)

fun weatherCondition(code: Int, isDay: Boolean = true): WeatherCondition = when (code) {
    0 -> WeatherCondition(code, "Clear Sky", if (isDay) "☀️" else "🌙")
    1 -> WeatherCondition(code, "Mainly Clear", if (isDay) "🌤️" else "🌙")
    2 -> WeatherCondition(code, "Partly Cloudy", "⛅")
    3 -> WeatherCondition(code, "Overcast", "☁️")
    in 45..48 -> WeatherCondition(code, "Foggy", "🌫️")
    in 51..55 -> WeatherCondition(code, "Drizzle", "🌦️")
    in 61..65 -> WeatherCondition(code, "Rain", "🌧️")
    in 71..77 -> WeatherCondition(code, "Snow", "❄️")
    in 80..82 -> WeatherCondition(code, "Rain Showers", "🌧️")
    in 85..86 -> WeatherCondition(code, "Snow Showers", "❄️")
    in 95..99 -> WeatherCondition(code, "Thunderstorm", "⛈️")
    else -> WeatherCondition(code, "Unknown", "🌡️")
}

data class SavedCity(
    val id: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val locationKey: String? = null
)

fun GeoLocation.toSavedCity() = SavedCity(
    id = id, name = name, region = region,
    country = country, latitude = latitude, longitude = longitude, locationKey = locationKey
)
