package com.weather.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path as GPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
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

private data class PagerCity(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrent: Boolean = false
) {
    val key: String get() = "${String.format("%.4f", latitude)},${String.format("%.4f", longitude)},$name"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onSearchClick: () -> Unit,
    selectedCityKey: String? = null,
    onSelectedCityChange: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cityStates by viewModel.cityStates.collectAsState()
    val savedCities by viewModel.savedCities.collectAsState()
    val lastLocation by viewModel.lastLocation.collectAsState()
    val scope = rememberCoroutineScope()

    val selectedState = selectedCityKey?.let { cityStates[it] }
    val selectedTransientCity = if (selectedState is WeatherUiState.SuccessXiaomi) {
        PagerCity(
            id = selectedCityKey.hashCode().toLong(),
            name = selectedState.cityName,
            latitude = lastLocation?.first ?: 0.0,
            longitude = lastLocation?.second ?: 0.0,
            isCurrent = true
        )
    } else null

    val pagerCities = remember(savedCities, selectedCityKey, selectedTransientCity) {
        buildList {
            if (selectedTransientCity != null && savedCities.none { "${String.format("%.4f", it.latitude)},${String.format("%.4f", it.longitude)},${it.name}" == selectedCityKey }) {
                add(selectedTransientCity)
            }
            addAll(savedCities.map {
                PagerCity(
                    id = it.id,
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    isCurrent = false
                )
            })
        }
    }

    val pageCount = maxOf(1, pagerCities.size)
    val initialPage = remember(pagerCities, selectedCityKey) {
        pagerCities.indexOfFirst { it.key == selectedCityKey }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    LaunchedEffect(selectedCityKey, pagerCities) {
        val index = pagerCities.indexOfFirst { it.key == selectedCityKey }
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.scrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.settledPage, pagerCities) {
        val city = pagerCities.getOrNull(pagerState.settledPage)
        if (city != null) {
            onSelectedCityChange(city.key)
            viewModel.loadCity(city.latitude, city.longitude, city.name, saveAsLast = false)
        }
    }

    val gradient = Brush.verticalGradient(listOf(Color(0xFF1C8DFF), Color(0xFF5BC8FA)))

    HorizontalPager(
        state = pagerState,
        key = { page -> pagerCities.getOrNull(page)?.id ?: page },
        modifier = Modifier.fillMaxSize().nestedScroll(object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
        }),
        pageSpacing = 0.dp,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapAnimationSpec = spring(dampingRatio = 0.85f, stiffness = 150f),
            snapPositionalThreshold = 0.1f,
            snapVelocityThreshold = 100.dp
        )
    ) { page ->
        val pageCity = pagerCities.getOrNull(page)
        val pageState = pageCity?.let { city ->
            cityStates[city.key]
        } ?: if (page == pagerState.settledPage) uiState else WeatherUiState.Idle

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            AnimatedContent(
                targetState = pageState,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                contentKey = { s ->
                    val pageKey = pagerCities.getOrNull(page)?.id?.toString() ?: page.toString()
                    if (s is WeatherUiState.SuccessXiaomi) "$pageKey:${s.cityName}:${s.weather.current?.pubTime}" else "$pageKey:${s::class}"
                },
                label = "weather_content"
            ) { state ->
                when (state) {
                    is WeatherUiState.Idle -> IdleContent(viewModel)
                    is WeatherUiState.Loading -> LoadingContent()
                    is WeatherUiState.SuccessXiaomi -> XiaomiSuccessContent(state, viewModel, onSearchClick, pagerCities, page, pagerState.currentPageOffsetFraction)
                    is WeatherUiState.Success -> LegacySuccessContent(state, viewModel, onSearchClick, savedCities)
                    is WeatherUiState.Error -> ErrorContent(state.message) { viewModel.retry() }
                }
            }
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
    val clipboard = LocalClipboardManager.current
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text("⚠️ 错误详情", fontSize = 28.sp, color = Color.White)
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = CardWhite)) {
                    Text("重试", color = Color.White)
                }
                OutlinedButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(message)) }) {
                    Text("复制错误")
                }
            }
        }
    }
}

@Composable
private fun XiaomiSuccessContent(
    state: WeatherUiState.SuccessXiaomi,
    viewModel: WeatherViewModel,
    onSearchClick: () -> Unit,
    pagerCities: List<PagerCity> = emptyList(),
    currentPage: Int = 0,
    pageOffset: Float = 0f
) {
    val scrollState = rememberScrollState()

    val current = state.weather.current
    val hourlySeriesTemp = state.weather.forecastHourly?.temperature
    val hourlySeriesWeather = state.weather.forecastHourly?.weather

    val aqi = state.weather.aqi
    val alerts = state.weather.alerts.orEmpty()
    val indices = state.weather.indices?.indices.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // 城市名头部：左城市名区域（左/中/右）+ 右侧按钮
        val pagerNames = pagerCities.map { it.name.split(",").first().split("，").first() }
        val curPage = currentPage
        val centerName = state.cityName.split(" ").first().split(",").first().split("，").first()
        val leftName = pagerNames.getOrNull(curPage - 1)
        val rightName = pagerNames.getOrNull(curPage + 1)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // 城市名区域（占满剩余空间）
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (leftName != null) Text(text = leftName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Text(
                    text = centerName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (rightName != null && rightName != centerName) Text(text = rightName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            // 按钮区域（固定右侧）
            val isSaved = pagerCities.any { !it.isCurrent && it.name == state.cityName }
            IconButton(onClick = { if (isSaved) viewModel.removeCurrentCity() else viewModel.saveCurrentCity() }) {
                Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = if (isSaved) "已收藏" else "收藏", tint = Color.White)
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
        // 天气 最高温 最低温 AQI 一行
        val todayHigh = state.weather.forecastDaily?.temperature?.value?.firstOrNull()?.from ?: "--"
        val todayLow = state.weather.forecastDaily?.temperature?.value?.firstOrNull()?.to ?: "--"
        val aqiVal = aqi?.aqi?.toIntOrNull()
        val aqiLabel = when {
            aqiVal == null -> ""
            aqiVal <= 50 -> "优"
            aqiVal <= 100 -> "良"
            aqiVal <= 150 -> "轻度"
            aqiVal <= 200 -> "中度"
            aqiVal <= 300 -> "重度"
            else -> "严重"
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(xiaomiWeatherDesc(current?.weather), color = TextSecondary, fontSize = 16.sp)
            Text("${todayHigh}°↑", color = Color(0xFFFFD54F), fontSize = 15.sp)
            Text("${todayLow}°↓", color = Color(0xFF90CAF9), fontSize = 15.sp)
            if (aqiVal != null) Text("AQI $aqiVal $aqiLabel", color = when {
                aqiVal <= 50 -> Color(0xFF66BB6A)
                aqiVal <= 100 -> Color(0xFFFFCA28)
                aqiVal <= 150 -> Color(0xFFFFA726)
                aqiVal <= 200 -> Color(0xFFEF5350)
                else -> Color(0xFFAB47BC)
            }, fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("体感", (current?.feelsLike?.value ?: "--") + "°", "🌡️", Modifier.weight(1f))
            StatTile("湿度", (current?.humidity?.value ?: "--") + "%", "💧", Modifier.weight(1f))
            val sunrise = state.weather.forecastDaily?.sunRiseSet?.value?.firstOrNull()?.from?.substringAfter("T")?.take(5) ?: "--"
            val sunset = state.weather.forecastDaily?.sunRiseSet?.value?.firstOrNull()?.to?.substringAfter("T")?.take(5) ?: "--"
            StatTile("日出日落", "$sunrise/$sunset", "🌅", Modifier.weight(1f), valueSize = 13)
        }

        Spacer(Modifier.height(12.dp))

        val hTemps = hourlySeriesTemp?.value.orEmpty().map { it.toString() }
        val hWeathers = hourlySeriesWeather?.value.orEmpty().map { it.toInt().toString() }
        // time 字段接口返回为空，从当前整点推算
        val hTimes = hourlySeriesTemp?.time.orEmpty().let { apiTimes ->
            if (apiTimes.isNotEmpty()) apiTimes
            else {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                (0 until hTemps.size).map {
                    cal.add(java.util.Calendar.HOUR_OF_DAY, if (it == 0) 0 else 1)
                    String.format("%02d:00", cal.get(java.util.Calendar.HOUR_OF_DAY))
                }
            }
        }
        if (hTemps.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                val hPrecip = state.weather.forecastHourly?.precipitationProbability?.value.orEmpty()
                val firstRainIdx = hPrecip.indexOfFirst { it.toInt() > 0 }
                val rainHint = if (firstRainIdx >= 0) {
                    val hh = hTimes.getOrNull(firstRainIdx) ?: ""
                    "预计 $hh 有雨"
                } else ""
                Text("24小时预报" + if (rainHint.isNotBlank()) "  ☔ $rainHint" else "", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                val curveTemps = hourlySeriesTemp?.value.orEmpty().take(24)
                val hCount = minOf(24, hTimes.size, hTemps.size)
                val itemWDp = 60
                val totalWDp = itemWDp * hCount
                // 曲线 + 文字共享同一个水平滚动区域，精确对齐
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.width(totalWDp.dp)) {
                        // 温度曲线
                        val minT = curveTemps.minOrNull() ?: 0.0
                        val maxT = curveTemps.maxOrNull() ?: 0.0
                        val tRange = (maxT - minT).coerceAtLeast(1.0)
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(74.dp)) {
                            if (curveTemps.size >= 2) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomStart)) {
                                    val itemPx = size.width / hCount
                                    val h = size.height
                                    val pts = curveTemps.mapIndexed { i, t ->
                                        Offset(
                                            x = i * itemPx + itemPx / 2f,
                                            y = h - ((t - minT) / tRange * (h - 16f) + 8f).toFloat()
                                        )
                                    }
                                    fun tempColor(t: Double): androidx.compose.ui.graphics.Color {
                                        val stops = listOf(
                                            -10.0 to androidx.compose.ui.graphics.Color(0xFF5C6BC0),
                                            0.0 to androidx.compose.ui.graphics.Color(0xFF42A5F5),
                                            10.0 to androidx.compose.ui.graphics.Color(0xFF26C6DA),
                                            20.0 to androidx.compose.ui.graphics.Color(0xFF66BB6A),
                                            25.0 to androidx.compose.ui.graphics.Color(0xFFFFCA28),
                                            30.0 to androidx.compose.ui.graphics.Color(0xFFFFA726),
                                            40.0 to androidx.compose.ui.graphics.Color(0xFFEF5350)
                                        )
                                        val lo = stops.lastOrNull { it.first <= t } ?: stops.first()
                                        val hi = stops.firstOrNull { it.first > t } ?: stops.last()
                                        if (lo == hi) return lo.second
                                        val f = ((t - lo.first) / (hi.first - lo.first)).toFloat().coerceIn(0f, 1f)
                                        return androidx.compose.ui.graphics.Color(
                                            red = lerp(lo.second.red, hi.second.red, f),
                                            green = lerp(lo.second.green, hi.second.green, f),
                                            blue = lerp(lo.second.blue, hi.second.blue, f),
                                            alpha = 1f
                                        )
                                    }
                                    for (i in 1 until pts.size) {
                                        val segPath = GPath()
                                        segPath.moveTo(pts[i-1].x, pts[i-1].y)
                                        val cx = (pts[i-1].x + pts[i].x) / 2f
                                        segPath.cubicTo(cx, pts[i-1].y, cx, pts[i].y, pts[i].x, pts[i].y)
                                        val avgT = (curveTemps[i-1] + curveTemps[i]) / 2.0
                                        drawPath(segPath, color = tempColor(avgT), style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                                    }
                                    pts.forEachIndexed { i, pt -> drawCircle(color = tempColor(curveTemps[i]), radius = 4f, center = pt) }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
                                for (idx in 0 until hCount) {
                                    val t = curveTemps.getOrNull(idx)
                                    if (t != null) {
                                        val normalized = ((t - minT) / tRange).toFloat().coerceIn(0f, 1f)
                                        val tempYOffset = ((1f - normalized) * 28f).dp
                                        Box(modifier = Modifier.width(itemWDp.dp).fillMaxHeight()) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .offset(y = tempYOffset),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "${t.toInt()}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "°",
                                                    color = Color.White,
                                                    fontSize = 4.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.offset(y = 1.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(itemWDp.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        // 时间/天气行
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val hPrecipLocal = hPrecip
                            for (idx in 0 until hCount) {
                                val precipPct = hPrecipLocal.getOrNull(idx)?.let { it.toInt() } ?: 0
                                Column(
                                    modifier = Modifier.width(itemWDp.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(hTimes.getOrNull(idx) ?: "", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                                    Text(weatherEmojiFromText(xiaomiWeatherDesc(hWeathers.getOrNull(idx))), fontSize = 20.sp)
                                    if (precipPct > 0) Text("${precipPct}%", color = Color(0xFFD6F0FF), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        val dRanges = state.weather.forecastDaily?.temperature?.value.orEmpty()
        val dWeathers = state.weather.forecastDaily?.weather?.value.orEmpty()
        if (dRanges.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("未来${dRanges.size}天", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dRanges.size) { i ->
                        val r = dRanges.getOrNull(i)
                        val wRange = dWeathers.getOrNull(i)
                        val dayLabel = when (i) {
                            0 -> "今天"
                            1 -> "明天"
                            2 -> "后天"
                            else -> {
                                val cal = java.util.Calendar.getInstance()
                                cal.add(java.util.Calendar.DAY_OF_YEAR, i)
                                val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                                "${day}号"
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(56.dp)) {
                            Text(dayLabel, color = TextSecondary, fontSize = 10.sp)
                            Text(weatherEmojiFromText(xiaomiWeatherDesc(wRange?.from)), fontSize = 18.sp)
                            Text(xiaomiWeatherDesc(wRange?.from), color = Color.White, fontSize = 10.sp, maxLines = 1)
                            Text("${r?.from ?: "--"}°", color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${r?.to ?: "--"}°", color = Color(0xFFCDEBFF), fontSize = 11.sp)
                            Text(xiaomiWeatherDesc(wRange?.to), color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                            Text(weatherEmojiFromText(xiaomiWeatherDesc(wRange?.to)), fontSize = 18.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        val nonEmptyIndices = indices.filter { !it.value.isNullOrBlank() && it.value != "0" }
        if (nonEmptyIndices.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("生活指数", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(nonEmptyIndices.size) { idx ->
                        val item = nonEmptyIndices[idx]
                        val label = indexTypeLabel(item.type ?: "")
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                            Text(label, color = TextSecondary, fontSize = 10.sp)
                            Text(item.value ?: "--", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (aqi != null) {
            val aqiVal = aqi.aqi?.toIntOrNull() ?: -1
            val (aqiLabel, aqiColor) = when {
                aqiVal < 0 -> "--" to Color.White
                aqiVal <= 50 -> "优" to Color(0xFF4CAF50)
                aqiVal <= 100 -> "良" to Color(0xFF8BC34A)
                aqiVal <= 150 -> "轻度污染" to Color(0xFFFF9800)
                aqiVal <= 200 -> "中度污染" to Color(0xFFF44336)
                aqiVal <= 300 -> "重度污染" to Color(0xFF9C27B0)
                else -> "严重污染" to Color(0xFF7B1FA2)
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text("空气质量", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AQI ${aqi.aqi ?: "--"}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(aqiLabel, color = aqiColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                if (!aqi.primary.isNullOrBlank()) Text("首要污染物：${aqi.primary}", color = TextSecondary)
                if (!aqi.suggest.isNullOrBlank()) Text(aqi.suggest!!, color = TextSecondary)
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

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LegacySuccessContent(
    state: WeatherUiState.Success,
    viewModel: WeatherViewModel,
    onSearchClick: () -> Unit,
    savedCities: List<com.weather.app.data.model.SavedCity> = emptyList()
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
            val isSaved = savedCities.any { it.name == state.cityName }
            IconButton(onClick = { if (isSaved) viewModel.removeSavedCity(state.cityName.hashCode().toLong()) else viewModel.saveCurrentCity() }) {
                Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = if (isSaved) "已收藏" else "收藏", tint = Color.White)
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

private fun indexTypeLabel(type: String): String = when (type) {
    "uvIndex" -> "紫外线"
    "humidity" -> "相对湿度"
    "feelsLike" -> "体感温度"
    "pressure" -> "气压"
    "carWash" -> "洗车指数"
    "sports" -> "运动指数"
    "dressing" -> "穿衣指数"
    "comfort" -> "舒适度"
    "flu" -> "感冒指数"
    "travel" -> "旅游指数"
    "sunscreen" -> "防晒指数"
    else -> type
}

fun xiaomiWeatherDesc(code: String?): String = when (code) {
    "0" -> "晴"
    "1" -> "多云"
    "2" -> "阴"
    "3" -> "阵雨"
    "4" -> "雷阵雨"
    "5" -> "雷阵雨伴冰雹"
    "6" -> "雨夹雪"
    "7" -> "小雨"
    "8" -> "中雨"
    "9" -> "大雨"
    "10" -> "暴雨"
    "11" -> "大暴雨"
    "12" -> "特大暴雨"
    "13" -> "阵雪"
    "14" -> "小雪"
    "15" -> "中雪"
    "16" -> "大雪"
    "17" -> "暴雪"
    "18" -> "雾"
    "19" -> "冻雨"
    "20" -> "沙尘暴"
    "21" -> "小到中雨"
    "22" -> "中到大雨"
    "23" -> "大到暴雨"
    "24" -> "暴雨到大暴雨"
    "25" -> "大暴雨到特大暴雨"
    "26" -> "小到中雪"
    "27" -> "中到大雪"
    "28" -> "大到暴雪"
    "29" -> "浮尘"
    "30" -> "扬沙"
    "31" -> "强沙尘暴"
    "53" -> "霾"
    null, "" -> "--"
    else -> code
}
