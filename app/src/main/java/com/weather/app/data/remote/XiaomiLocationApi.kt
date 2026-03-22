package com.weather.app.data.remote

import com.weather.app.data.model.XiaomiCitySearchItem
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

object XiaomiLocationApi {

    private const val BASE = "https://weatherapi.market.xiaomi.com/wtr-v3/location/city"

    suspend fun searchCity(name: String): List<XiaomiCitySearchItem> =
        WeatherApi.client.get("$BASE/search") {
            parameter("name", name)
            parameter("appKey", "weather20151024")
            parameter("sign", "zUFJoAR2ZVrDy1vF3D07")
            parameter("appVersion", "17000318")
            parameter("alpha", "false")
            parameter("isGlobal", "false")
            parameter("locale", "zh_cn")
        }.body()
}
