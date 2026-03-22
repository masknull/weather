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
import kotlin.math.abs
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel, onSearchClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val savedCities by viewModel.savedCities.collectAsState()
    val scope = rememberCoroutineScope()

    // pages: 0 = current location/city, 1..N = saved cities
    val pageCount = maxOf(1, savedCities.size + 1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // 只在滑动完全停止后（settledPage）才触发数据请求，避免滑动中转圈
    LaunchedEffect(pagerState.settledPage) {
        val page = pagerState.settledPage
        if (page > 0) {
            val city = savedCities.getOrNull(page - 1)
            if (city != null) viewModel.loadCity(city.latitude, city.longitude, city.name)
        }
    }

    val gradient = Brush.verticalGradient(listOf(Color(0xFF1C8DFF), Color(0xFF5BC8FA)))

    HorizontalPager(
        state = pagerState,
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
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "weather_content"
            ) { state ->
                when (state) {
                    is WeatherUiState.Idle -> IdleContent(viewModel)
                    is WeatherUiState.Loading -> LoadingContent()
                    is WeatherUiState.SuccessXiaomi -> XiaomiSuccessContent(state, viewModel, onSearchClick, savedCities, pagerState.currentPage, pagerState.currentPageOffsetFraction)
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
    savedCities: List<com.weather.app.data.model.SavedCity> = emptyList(),
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

        // 城市名头部：当前城市居中大，左右最多各1个收藏城市，小且透明
        val allCityNames = listOf("我的位置") + savedCities.map { it.name }
        val curPage = currentPage
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                // 左侧城市（curPage-1）
                val leftName = if (curPage > 0) allCityNames.getOrNull(curPage - 1)?.split(" ")?.first()?.split(",")?.first() else null
                if (leftName != null) {
                    val leftAlpha = lerp(0.4f, 0.15f, pageOffset.coerceIn(0f, 1f))
                    val leftSize = lerp(16f, 13f, pageOffset.coerceIn(0f, 1f))
                    Text(
                        text = leftName,
                        color = Color.White,
                        fontSize = leftSize.sp,
                        modifier = Modifier.alpha(leftAlpha).padding(end = 8.dp),
                        maxLines = 1
                    )
                }
                // 当前城市
                Text(
                    text = state.cityName.split(" ").first().split(",").first().split("，").first(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                // 右侧城市（curPage+1）
                val rightName = allCityNames.getOrNull(curPage + 1)?.split(" ")?.first()?.split(",")?.first()
                if (rightName != null) {
                    val rightAlpha = lerp(0.4f, 0.6f, pageOffset.coerceIn(0f, 1f))
                    val rightSize = lerp(13f, 16f, pageOffset.coerceIn(0f, 1f))
                    Text(
                        text = rightName,
                        color = Color.White,
                        fontSize = rightSize.sp,
                        modifier = Modifier.alpha(rightAlpha).padding(start = 8.dp),
                        maxLines = 1
                    )
                }
            }
            val isSaved = savedCities.any { it.name == state.cityName }
            IconButton(onClick = { if (isSaved) viewModel.removeSavedCity(state.cityName.hashCode().toLong()) else viewModel.saveCurrentCity() }) {
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
        Text(
            text = xiaomiWeatherDesc(current?.weather),
            color = TextSecondary,
            fontSize = 20.sp
        )

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
                        if (curveTemps.size >= 2) {
                            val minT = curveTemps.min()
                            val maxT = curveTemps.max()
                            val tRange = (maxT - minT).coerceAtLeast(1.0)
                            Canvas(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                val itemPx = size.width / hCount
                                val h = size.height
                                val pts = curveTemps.mapIndexed { i, t ->
                                    Offset(
                                        x = i * itemPx + itemPx / 2f,
                                        y = h - ((t - minT) / tRange * (h - 16f) + 8f).toFloat()
                                    )
                                }
                                val path = GPath()
                                path.moveTo(pts[0].x, pts[0].y)
                                for (i in 1 until pts.size) {
                                    val cx = (pts[i-1].x + pts[i].x) / 2f
                                    path.cubicTo(cx, pts[i-1].y, cx, pts[i].y, pts[i].x, pts[i].y)
                                }
                                drawPath(path, color = androidx.compose.ui.graphics.Color(0xFF90CAF9), style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                                pts.forEach { pt -> drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 4f, center = pt) }
                                // 温度标注
                                curveTemps.forEachIndexed { i, t ->
                                    val pt = pts[i]
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "${t.toInt()}°",
                                        pt.x,
                                        pt.y - 10f,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 28f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            isAntiAlias = true
                                        }
                                    )
                                }
                            }
                        }
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
                                    Text(hTimes.getOrNull(idx) ?: "", color = Color.White.copy(alpha=0.9f), fontSize = 11.sp)
                                    Text(weatherEmojiFromText(xiaomiWeatherDesc(hWeathers.getOrNull(idx))), fontSize = 20.sp)
                                    if (precipPct > 0) Text("${precipPct}%", color = Color(0xFF90CAF9), fontSize = 10.sp)
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
                // 最高温曲线
                val dHighs = dRanges.map { it.from?.toDoubleOrNull() ?: 0.0 }
                val dLows = dRanges.map { it.to?.toDoubleOrNull() ?: 0.0 }
                val allTemps = dHighs + dLows
                val minT = allTemps.minOrNull() ?: 0.0
                val maxT = allTemps.maxOrNull() ?: 1.0
                val tRange = (maxT - minT).coerceAtLeast(1.0)
                val count = dRanges.size
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val step = w / (count - 1).coerceAtLeast(1)
                    // high curve
                    val highPts = dHighs.mapIndexed { i, t -> Offset(i * step, h - ((t - minT) / tRange * (h - 12f) + 6f).toFloat()) }
                    val lowPts = dLows.mapIndexed { i, t -> Offset(i * step, h - ((t - minT) / tRange * (h - 12f) + 6f).toFloat()) }
                    fun drawCurve(pts: List<Offset>, color: androidx.compose.ui.graphics.Color) {
                        if (pts.size < 2) return
                        val path = GPath()
                        path.moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            val cx = (pts[i-1].x + pts[i].x) / 2f
                            path.cubicTo(cx, pts[i-1].y, cx, pts[i].y, pts[i].x, pts[i].y)
                        }
                        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
                    }
                    drawCurve(highPts, androidx.compose.ui.graphics.Color(0xFFFFD54F))
                    drawCurve(lowPts, androidx.compose.ui.graphics.Color(0xFF90CAF9))
                }
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(count) { i ->
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(weatherEmojiFromText(xiaomiWeatherDesc(wRange?.from)), fontSize = 13.sp)
                                Text(xiaomiWeatherDesc(wRange?.from), color = Color.White, fontSize = 10.sp, maxLines = 1)
                            }
                            Text("${r?.from ?: "--"}°", color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${r?.to ?: "--"}°", color = Color(0xFF90CAF9), fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(weatherEmojiFromText(xiaomiWeatherDesc(wRange?.to)), fontSize = 13.sp)
                                Text(xiaomiWeatherDesc(wRange?.to), color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (aqi != null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("空气质量", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("AQI：${aqi.aqi ?: "--"}", color = Color.White)
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
