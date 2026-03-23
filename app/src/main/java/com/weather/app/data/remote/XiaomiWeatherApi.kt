package com.weather.app.data.remote

import com.weather.app.data.model.XiaomiWeatherResponse
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

object XiaomiWeatherApi {

    private const val BASE = "https://weatherapi.market.xiaomi.com/wtr-v3/weather"

    @Volatile
    var lastRaw: String? = null
        private set

    private val rawJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    suspend fun getAll(lat: Double, lon: Double, days: Int = 15): XiaomiWeatherResponse {
        val text = WeatherApi.client.get("$BASE/all") {
            parameter("latitude", String.format("%.2f", lat))
            parameter("longitude", String.format("%.2f", lon))
            parameter("isLocated", "true")
            parameter("days", days)
            parameter("appKey", "weather20151024")
            parameter("sign", "zUFJoAR2ZVrDy1vF3D07")
            parameter("appVersion", "17000318")
            parameter("alpha", "false")
            parameter("isGlobal", "false")
            parameter("locale", "zh_cn")
        }.bodyAsText()

        // keep a snippet for error screen copy
        lastRaw = text.take(8000)

        return rawJson.decodeFromString(XiaomiWeatherResponse.serializer(), text)
    }
}
