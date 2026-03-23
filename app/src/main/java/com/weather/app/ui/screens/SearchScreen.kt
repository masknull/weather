package com.weather.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.data.model.GeoLocation
import com.weather.app.data.model.SavedCity
import com.weather.app.ui.theme.*
import com.weather.app.viewmodel.SearchState
import com.weather.app.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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
    val currentLocationSelection by viewModel.currentLocationSelection.collectAsState()
    val isRefreshing = searchState is SearchState.Loading && searchQuery.isBlank()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.clearSearch()
            viewModel.loadHotCities()
        }
    )

    LaunchedEffect(Unit) {
        viewModel.clearSearch()
        viewModel.loadHotCities()
        if (currentLocationSelection == null) {
            viewModel.fetchCurrentLocation()
        }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                when (val state = searchState) {
                    is SearchState.Idle -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (savedCities.isNotEmpty()) {
                                item {
                                    Text("已保存城市", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(8.dp))
                                }
                                items(savedCities, key = { city -> city.id }) { city ->
                                    SavedCityRow(
                                        city = city,
                                        onClick = {
                                            viewModel.rememberCitySelection(city.latitude, city.longitude, city.name, locationKey = city.locationKey)
                                            onCitySelected(city.latitude, city.longitude, city.name)
                                        },
                                        onDelete = { viewModel.removeSavedCity(city.id) },
                                        canMoveUp = savedCities.indexOfFirst { it.id == city.id } > 0,
                                        canMoveDown = savedCities.indexOfFirst { it.id == city.id } < savedCities.lastIndex,
                                        onMoveUp = { viewModel.moveSavedCity(city.id, -1) },
                                        onMoveDown = { viewModel.moveSavedCity(city.id, 1) }
                                    )
                                }
                            }
                            if (hotCities.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(if (savedCities.isNotEmpty()) 8.dp else 0.dp))
                                    Text("推荐城市", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(8.dp))
                                    HotCitiesGrid(
                                        currentLocationName = currentLocationSelection?.shortName ?: "当前位置",
                                        cities = hotCities,
                                        onCurrentLocationClick = {
                                            onUseCurrentLocation()
                                            viewModel.fetchCurrentLocation()
                                        },
                                        onClick = { city ->
                                            viewModel.rememberCitySelection(city.latitude, city.longitude, city.displayName, locationKey = city.locationKey)
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
                                        viewModel.rememberCitySelection(loc.latitude, loc.longitude, loc.displayName, locationKey = loc.locationKey)
                                        onCitySelected(loc.latitude, loc.longitude, loc.displayName)
                                    }
                                }
                            }
                        }
                    }
                    is SearchState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(state.message, color = Color(0xFFFF6B6B))
                            if (hotCities.isNotEmpty()) {
                                Text("下拉可重新加载推荐城市", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = CardWhite,
                    contentColor = Color.White
                )
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
fun HotCitiesGrid(
    currentLocationName: String = "当前位置",
    cities: List<GeoLocation>,
    onCurrentLocationClick: () -> Unit = {},
    onClick: (GeoLocation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val gridItems = buildList {
            add(currentLocationName)
            addAll(cities)
        }
        gridItems.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(CardWhite, RoundedCornerShape(12.dp))
                            .clickable {
                                when (item) {
                                    is GeoLocation -> onClick(item)
                                    is String -> onCurrentLocationClick()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (item is String) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = when (item) {
                                    is GeoLocation -> item.name
                                    is String -> item
                                    else -> ""
                                },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
fun SavedCityRow(
    city: SavedCity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(0.dp, RoundedCornerShape(12.dp))
            .background(CardWhite, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Bookmark, contentDescription = null, tint = SunYellow, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(city.name, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "上移", tint = if (canMoveUp) TextSecondary else TextSecondary.copy(alpha = 0.35f), modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "下移", tint = if (canMoveDown) TextSecondary else TextSecondary.copy(alpha = 0.35f), modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
