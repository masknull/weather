package com.weather.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class XiaomiCitySearchItem(
    val affiliation: String? = null,
    val key: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val locationKey: String? = null,
    val name: String? = null,
    val status: Int? = null,
    val timeZoneShift: Int? = null,
)
