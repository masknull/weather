package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.ui.theme.*

// ── Glass card ───────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(16.dp),
        content = content
    )
}

// ── Weather icon (emoji-based, no asset dependency) ──────────────────────────

fun weatherEmoji(code: Int, isDay: Boolean = true): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1 -> if (isDay) "🌤️" else "🌙"
    2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55 -> "🌦️"
    61, 63, 65 -> "🌧️"
    71, 73, 75 -> "❄️"
    77 -> "🌨️"
    80, 81, 82 -> "🌧️"
    85, 86 -> "❄️"
    95 -> "⛈️"
    96, 99 -> "⛈️"
    else -> "🌡️"
}

fun weatherDescription(code: Int): String = when (code) {
    0 -> "晴"
    1 -> "大部晴朗"
    2 -> "局部多云"
    3 -> "阴"
    45, 48 -> "有雾"
    51, 53, 55 -> "毛毛雨"
    61, 63, 65 -> "雨"
    71, 73, 75 -> "雪"
    77 -> "米雪"
    80, 81, 82 -> "阵雨"
    85, 86 -> "阵雪"
    95 -> "雷暴"
    96, 99 -> "雷暴伴冰雹"
    else -> "未知"
}

// ── Sky gradient background ───────────────────────────────────────────────────

fun skyGradient(weatherCode: Int, isDay: Boolean): Brush {
    return if (!isDay) {
        Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF1A2F5E)))
    } else when (weatherCode) {
        0, 1 -> Brush.verticalGradient(listOf(Color(0xFF1C8DFF), Color(0xFF5BC8FA)))
        2, 3 -> Brush.verticalGradient(listOf(Color(0xFF4A7FA5), Color(0xFF7BAEC8)))
        45, 48 -> Brush.verticalGradient(listOf(Color(0xFF6B7B8D), Color(0xFF9AAAB8)))
        in 51..82 -> Brush.verticalGradient(listOf(Color(0xFF3A5F7D), Color(0xFF5A8AA0)))
        else -> Brush.verticalGradient(listOf(Color(0xFF1C8DFF), Color(0xFF5BC8FA)))
    }
}

// ── Stat tile ────────────────────────────────────────────────────────────────

@Composable
fun StatTile(label: String, value: String, icon: String, modifier: Modifier = Modifier, valueSize: Int = 22) {
    GlassCard(modifier = modifier.fillMaxHeight()) {
        Text(text = "$icon  $label", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(text = value, color = TextPrimary, fontSize = valueSize.sp, fontWeight = FontWeight.SemiBold)
    }
}
