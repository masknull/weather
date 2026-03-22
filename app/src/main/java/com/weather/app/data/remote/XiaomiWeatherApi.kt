package com.weather.app.data.remote

import com.weather.app.data.model.XiaomiWeatherResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

object XiaomiWeatherApi {

    private const val BASE = "https://weatherapi.market.xiaomi.com/wtr-v3/weather"

    suspend fun getAll(lat: Double, lon: Double, days: Int = 15): XiaomiWeatherResponse =
        WeatherApi.client.get("$BASE/all") {
            parameter("latitude", String.format("%.2f", lat))
            parameter("longitude", String.format("%.2f", lon))
            parameter("isLocated", "true")
            parameter("locationKey", "weathercn:101210606")
            parameter("days", days)
            parameter("appKey", "weather20151024")
            parameter("sign", "zUFJoAR2ZVrDy1vF3D07")
            parameter("appVersion", "17000318")
            parameter("alpha", "false")
            parameter("isGlobal", "false")
            parameter("locale", "zh_cn")
        }.body()
}
