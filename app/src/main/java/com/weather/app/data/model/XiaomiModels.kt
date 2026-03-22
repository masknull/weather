package com.weather.app.data.model

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
data class XiaomiUnitValue(
    val unit: String? = null,
    val value: String? = null,
)

@Serializable
data class XiaomiRangeValue(
    val from: String? = null,
    val to: String? = null,
)

@Serializable
data class XiaomiCurrent(
    val temperature: XiaomiUnitValue? = null,
    val feelsLike: XiaomiUnitValue? = null,
    val humidity: XiaomiUnitValue? = null,
    val weather: String? = null,
    val wind: XiaomiWind? = null,
)

@Serializable
data class XiaomiWind(
    val direction: XiaomiUnitValue? = null,
    val speed: XiaomiUnitValue? = null,
)

@Serializable
data class XiaomiForecastHourly(
    val status: Int? = null,
    val temperature: XiaomiSeries? = null,
    val weather: XiaomiSeries? = null,
    val aqi: XiaomiSeries? = null,
    val wind: XiaomiSeries? = null,
    val precipitationProbability: XiaomiSeries? = null,
    val desc: String? = null,
)

@Serializable
data class XiaomiForecastDaily(
    val status: Int? = null,
    val temperature: XiaomiRangeSeries? = null,
    val weather: XiaomiRangeSeries? = null,
    val aqi: XiaomiSeries? = null,
    val wind: XiaomiDailyWind? = null,
    val precipitationProbability: XiaomiStringSeries? = null,
    val sunRiseSet: XiaomiRangeSeries? = null,
    val pubTime: String? = null,
)

@Serializable
data class XiaomiSeries(
    val status: Int? = null,
    val unit: String? = null,
    val time: List<String>? = null,
    val value: List<Double>? = null,
    val from: String? = null,
    val pubTime: String? = null,
)

@Serializable
data class XiaomiStringSeries(
    val status: Int? = null,
    val unit: String? = null,
    val value: List<String>? = null,
)

@Serializable
data class XiaomiRangeSeries(
    val status: Int? = null,
    val unit: String? = null,
    val value: List<XiaomiRangeValue>? = null,
)

@Serializable
data class XiaomiDailyWind(
    val direction: XiaomiRangeSeries? = null,
    val speed: XiaomiRangeSeries? = null,
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
    val indices: List<XiaomiIndexItem>? = null,
)

@Serializable
data class XiaomiIndexItem(
    val name: String? = null,
    val desc: String? = null,
)
