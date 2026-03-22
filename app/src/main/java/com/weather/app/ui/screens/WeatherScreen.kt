package com.weather.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.data.model.ForecastResponse
import com.weather.app.ui.components.*
import com.weather.app.ui.theme.*
import com.weather.app.viewmodel.WeatherUiState
import com.weather.app.viewmodel.WeatherViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel, onSearchClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val gradient = when (val s = uiState) {
        is WeatherUiState.Success -> skyGradient(
            s.forecast.current.weatherCode,
            s.forecast.current.isDay == 1
        )
        else -> Brush.verticalGradient(listOf(Color(0xFF1C8DFF), Color(0xFF5BC8FA)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        when (val state = uiState) {
            is WeatherUiState.Idle -> IdleContent(viewModel)
            is WeatherUiState.Loading -> LoadingContent()
            is WeatherUiState.Success -> SuccessContent(state, viewModel, onSearchClick)
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            Text(message, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = CardWhite)) {
                Text("重试", color = Color.White)
            }
        }
    }
}

@Composable
fun SuccessContent(state: WeatherUiState.Success, viewModel: WeatherViewModel, onSearchClick: () -> Unit) {
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
        // City name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.cityName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.saveCurrentCity() }) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = "Save", tint = Color.White)
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
        }

        // Big temperature
        Text(
            text = "${state.forecast.current.temperature.toInt()}°",
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Thin,
            lineHeight = 100.sp
        )
        Text(
            text = weatherDescription(state.forecast.current.weatherCode),
            color = TextSecondary,
            fontSize = 20.sp
        )
        Text(
            text = "H:${state.forecast.daily.tempMax[0].toInt()}°  L:${state.forecast.daily.tempMin[0].toInt()}°",
            color = TextSecondary,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(24.dp))

        // Stats row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Feels Like", "${state.forecast.current.feelsLike.toInt()}°", "🌡️", Modifier.weight(1f))
            StatTile("Humidity", "${state.forecast.current.humidity}%", "💧", Modifier.weight(1f))
            StatTile("Wind", "${state.forecast.current.windSpeed.toInt()} km/h", "💨", Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Hourly
        HourlyForecastCard(state.forecast)

        Spacer(Modifier.height(12.dp))

        // Daily
        DailyForecastCard(state.forecast)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun HourlyForecastCard(forecast: ForecastResponse) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("逐小时预报", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        val now = LocalDateTime.now()
        val hourFormatter = DateTimeFormatter.ofPattern("ha", Locale.US)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            val times = forecast.hourly.time
            val temps = forecast.hourly.temperature
            val codes = forecast.hourly.weatherCode
            // show next 24 hours
            val startIdx = times.indexOfFirst {
                try { LocalDateTime.parse(it).isAfter(now.minusHours(1)) } catch (e: Exception) { false }
            }.coerceAtLeast(0)
            items(minOf(24, times.size - startIdx)) { i ->
                val idx = startIdx + i
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (i == 0) "现在" else try { LocalDateTime.parse(times[idx]).format(hourFormatter) } catch (e: Exception) { "" },
                        color = TextSecondary, fontSize = 12.sp
                    )
                    Text(weatherEmoji(codes[idx]), fontSize = 22.sp)
                    Text("${temps[idx].toInt()}°", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun DailyForecastCard(forecast: ForecastResponse) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("7日预报", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)
        forecast.daily.time.forEachIndexed { i, dateStr ->
            if (i > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (i == 0) "今天" else try { java.time.LocalDate.parse(dateStr).format(dayFormatter) } catch (e: Exception) { dateStr },
                    color = Color.White, fontSize = 15.sp, modifier = Modifier.width(64.dp)
                )
                Text(weatherEmoji(forecast.daily.weatherCode[i]), fontSize = 20.sp, modifier = Modifier.width(36.dp))
                Spacer(Modifier.weight(1f))
                Text("${forecast.daily.tempMin[i].toInt()}°", color = TextSecondary, fontSize = 15.sp)
                Spacer(Modifier.width(12.dp))
                Text("${forecast.daily.tempMax[i].toInt()}°", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
