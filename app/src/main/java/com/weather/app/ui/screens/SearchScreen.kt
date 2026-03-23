package com.weather.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.data.model.GeoLocation
import com.weather.app.data.model.SavedCity
import com.weather.app.ui.theme.*
import com.weather.app.viewmodel.SearchState
import com.weather.app.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onBack: () -> Unit,
    onCitySelected: (Double, Double, String) -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val savedCities by viewModel.savedCities.collectAsState(emptyList())
    val hotCities by viewModel.hotCities.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.clearSearch()
        viewModel.loadHotCities()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyNight, SkyNightLight)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Search bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(CardWhite, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(if (searchQuery.isEmpty()) Color.Transparent else Color.White),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text("搜索城市…", color = TextSecondary)
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick entry: use current location
            ElevatedCard(
                onClick = {
                    onUseCurrentLocation()
                    viewModel.fetchCurrentLocation()
                },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = CardWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("使用当前位置", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text("自动定位并查看天气", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (val state = searchState) {
                is SearchState.Idle -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (savedCities.isNotEmpty()) {
                            item {
                                Text("已保存城市", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(8.dp))
                            }
                            items(savedCities) { city ->
                                SavedCityRow(
                                    city = city,
                                    onClick = {
                                        viewModel.loadCity(city.latitude, city.longitude, city.name, locationKey = city.locationKey)
                                        onCitySelected(city.latitude, city.longitude, city.name)
                                    },
                                    onDelete = { viewModel.removeSavedCity(city.id) }
                                )
                            }
                        }
                        if (hotCities.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(if (savedCities.isNotEmpty()) 8.dp else 0.dp))
                                Text("推荐城市", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(8.dp))
                                HotCitiesGrid(
                                    cities = hotCities,
                                    onClick = { city ->
                                        viewModel.loadCity(city.latitude, city.longitude, city.displayName, locationKey = city.locationKey)
                                        onCitySelected(city.latitude, city.longitude, city.displayName)
                                    }
                                )
                            }
                        }
                        if (savedCities.isEmpty() && hotCities.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                                    Text("先搜索一个城市开始使用", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
                is SearchState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    }
                }
                is SearchState.Results -> {
                    if (state.locations.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text("没有搜到结果", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.locations) { loc ->
                                LocationRow(loc) {
                                    viewModel.loadCity(loc.latitude, loc.longitude, loc.displayName, locationKey = loc.locationKey)
                                    onCitySelected(loc.latitude, loc.longitude, loc.displayName)
                                }
                            }
                        }
                    }
                }
                is SearchState.Error -> {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color(0xFFFF6B6B))
                    }
                }
            }
        }
    }
}

@Composable
fun LocationRow(location: GeoLocation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(CardWhite, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(location.displayName, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
fun HotCitiesGrid(cities: List<GeoLocation>, onClick: (GeoLocation) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cities.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { city ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(CardWhite, RoundedCornerShape(12.dp))
                            .clickable { onClick(city) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(city.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SavedCityRow(city: SavedCity, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(CardWhite, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Bookmark, contentDescription = null, tint = SunYellow, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(city.name, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
