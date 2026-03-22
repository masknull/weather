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
    val pressure: XiaomiUnitValue? = null,
    val visibility: XiaomiUnitValue? = null,
    val weather: String? = null,
    val wind: XiaomiWind? = null,
    val pubTime: String? = null,
    val uvIndex: String? = null,
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
    val wind: XiaomiHourlyWind? = null,
    val precipitationProbability: XiaomiSeries? = null,
    val desc: String? = null,
)


@Serializable
data class XiaomiHourlyWind(
    val status: Int? = null,
    val value: List<XiaomiHourlyWindItem>? = null,
)

@Serializable
data class XiaomiHourlyWindItem(
    val datetime: String? = null,
    val direction: String? = null,
    val speed: String? = null,
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
    val desc: String? = null,
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
    val aqi: String? = null,
    val primary: String? = null,
    val suggest: String? = null,
    val pm25Desc: String? = null,
    val pm10Desc: String? = null,
    val no2Desc: String? = null,
    val so2Desc: String? = null,
    val coDesc: String? = null,
    val o3Desc: String? = null,
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
    val type: String? = null,
    val name: String? = null,
    val desc: String? = null,
    val value: String? = null,
)
