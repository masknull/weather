package com.weather.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XiaomiWeatherResponse(
    val current: XiaomiCurrent? = null,
    val forecastHourly: XiaomiForecastHourly? = null,
    val forecastDaily: XiaomiForecastDaily? = null,
    val aqi: XiaomiAqi? = null,
    val alerts: List<XiaomiAlert>? = null,
    val indices: XiaomiIndices? = null,
    val updateTime: String? = null,
)

@Serializable
data class XiaomiCurrent(
    val temperature: String? = null,
    val feelsLike: String? = null,
    val humidity: String? = null,
    val weather: String? = null,
    val windDirection: String? = null,
    val windSpeed: String? = null,
)

@Serializable
data class XiaomiForecastHourly(
    val data: List<XiaomiHourlyItem>? = null,
)

@Serializable
data class XiaomiHourlyItem(
    val time: String? = null,
    val weather: String? = null,
    val temperature: String? = null,
)

@Serializable
data class XiaomiForecastDaily(
    val data: List<XiaomiDailyItem>? = null,
)

@Serializable
data class XiaomiDailyItem(
    val date: String? = null,
    val weather: String? = null,
    val tempMax: String? = null,
    val tempMin: String? = null,
)

@Serializable
data class XiaomiAqi(
    val aqi: XiaomiAqiValue? = null,
)

@Serializable
data class XiaomiAqiValue(
    val value: String? = null,
    val level: String? = null,
    val desc: String? = null,
)

@Serializable
data class XiaomiAlert(
    val title: String? = null,
    val detail: String? = null,
)

@Serializable
data class XiaomiIndices(
    val data: List<XiaomiIndexItem>? = null,
)

@Serializable
data class XiaomiIndexItem(
    val name: String? = null,
    val desc: String? = null,
)
