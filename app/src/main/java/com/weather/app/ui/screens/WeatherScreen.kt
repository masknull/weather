package com.weather.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.ui.components.GlassCard
import com.weather.app.ui.components.StatTile
import com.weather.app.ui.components.weatherEmoji
import com.weather.app.ui.theme.CardWhite
import com.weather.app.ui.theme.TextSecondary
import com.weather.app.viewmodel.WeatherUiState
import com.weather.app.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel, onSearchClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    val gradient = Brush.verticalGradient(listOf(Color(0xFF1C8DFF), Color(0xFF5BC8FA)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        when (val state = uiState) {
            is WeatherUiState.Idle -> IdleContent(viewModel)
            is WeatherUiState.Loading -> LoadingContent()
            is WeatherUiState.SuccessXiaomi -> XiaomiSuccessContent(state, viewModel, onSearchClick)
            is WeatherUiState.Success -> LegacySuccessContent(state, viewModel, onSearchClick)
            is WeatherUiState.Error -> ErrorContent(state.message) { viewModel.retry() }
        }
    }
}

@Composable
fun IdleContent(viewModel: WeatherViewModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("🌤️", fontSize = 80.sp)
            Text("天气", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("选择城市或使用当前位置", color = TextSecondary, fontSize = 15.sp)
            Button(
                onClick = { viewModel.fetchCurrentLocation() },
                colors = ButtonDefaults.buttonColors(containerColor = CardWhite)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("我的位置", color = Color.White)
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            Text("加载中…", color = TextSecondary, fontSize = 15.sp)
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("⚠️", fontSize = 48.sp)
            Text(message, color = Color.White)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = CardWhite)) {
                Text("重试", color = Color.White)
            }
        }
    }
}

@Composable
private fun XiaomiSuccessContent(
    state: WeatherUiState.SuccessXiaomi,
    viewModel: WeatherViewModel,
    onSearchClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val current = state.weather.current
    val hourlySeriesTemp = state.weather.forecastHourly?.temperature
    val hourlySeriesWeather = state.weather.forecastHourly?.weather

    val aqi = state.weather.aqi?.aqi
    val alerts = state.weather.alerts.orEmpty()
    val indices = state.weather.indices?.indices.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.cityName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.saveCurrentCity() }) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = "收藏", tint = Color.White)
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "搜索", tint = Color.White)
            }
        }

        val temp = current?.temperature?.value ?: "--"
        Text(
            text = "$temp°",
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Thin,
            lineHeight = 100.sp
        )
        Text(
            text = current?.weather ?: "",
            color = TextSecondary,
            fontSize = 20.sp
        )

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("体感", (current?.feelsLike?.value ?: "--") + "°", "🌡️", Modifier.weight(1f))
            StatTile("湿度", (current?.humidity?.value ?: "--") + "%", "💧", Modifier.weight(1f))
            StatTile("风速", (current?.wind?.speed?.value ?: "--") + (current?.wind?.speed?.unit ?: ""), "💨", Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        val hTimes = hourlySeriesTemp?.time.orEmpty()
        val hTemps = hourlySeriesTemp?.value.orEmpty().map { it.toString() }
        val hWeathers = hourlySeriesWeather?.value.orEmpty().map { it.toString() }
        if (hTimes.isNotEmpty() && hTemps.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("24小时预报", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    val count = minOf(24, hTimes.size, hTemps.size)
                    items(count) { idx ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(hTimes.getOrNull(idx) ?: "", color = TextSecondary, fontSize = 12.sp)
                            Text(weatherEmojiFromText(hWeathers.getOrNull(idx)), fontSize = 22.sp)
                            Text(((hourlySeriesTemp?.value?.getOrNull(idx) ?: Double.NaN).let { if (it.isNaN()) "--" else it.toInt().toString() }) + "°", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Daily: 小米返回是 series 结构，beta 先把 temperature.time/value 原样展示（后续再拆 min/max）
        val dTimes = state.weather.forecastDaily?.temperature?.time.orEmpty()
        val dTemps = state.weather.forecastDaily?.temperature?.value.orEmpty()
        if (dTimes.isNotEmpty() && dTemps.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("未来几天", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                val count = minOf(7, dTimes.size, dTemps.size)
                for (i in 0 until count) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(dTimes.getOrNull(i) ?: "", color = TextSecondary, modifier = Modifier.weight(1f))
                        Text(((dTemps.getOrNull(i) ?: Double.NaN).let { if (it.isNaN()) "--" else it.toInt().toString() }) + "°", color = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (aqi != null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("空气质量", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("AQI：${aqi.value ?: "--"}  ${aqi.level ?: ""}  ${aqi.desc ?: ""}", color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (alerts.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("预警", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                alerts.take(5).forEach { a ->
                    Text(a.title ?: "", color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (!a.detail.isNullOrBlank()) Text(a.detail!!, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (indices.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("生活指数", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                indices.take(12).forEach { i ->
                    Text(i.name ?: "", color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (!i.desc.isNullOrBlank()) Text(i.desc!!, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LegacySuccessContent(
    state: WeatherUiState.Success,
    viewModel: WeatherViewModel,
    onSearchClick: () -> Unit
) {
    // 保留旧实现，避免大改导致回归；beta 主要走 Xiaomi 分支
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.cityName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.saveCurrentCity() }) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = "收藏", tint = Color.White)
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "搜索", tint = Color.White)
            }
        }
        Text(
            text = "${state.forecast.current.temperature.toInt()}°",
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Thin,
            lineHeight = 100.sp
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun weatherEmojiFromText(text: String?): String {
    val t = (text ?: "").lowercase()
    return when {
        t.contains("雨") || t.contains("rain") -> "🌧️"
        t.contains("雪") || t.contains("snow") -> "❄️"
        t.contains("雷") || t.contains("thunder") -> "⛈️"
        t.contains("多云") || t.contains("cloud") -> "⛅"
        t.contains("阴") -> "☁️"
        t.contains("晴") || t.contains("sun") || t.contains("clear") -> "☀️"
        else -> "🌤️"
    }
}
