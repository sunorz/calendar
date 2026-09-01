package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.todayMonthIndex
    )

    // Compute currently visible Year and Month for TopBar title
    val currentVisibleYearMonth by remember {
        derivedStateOf {
            val visibleIndex = listState.firstVisibleItemIndex
            CalendarViewModel.getYearMonthFromIndex(visibleIndex)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "${currentVisibleYearMonth.first} 年 ${currentVisibleYearMonth.second} 月",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        // "今日" Jump button
                        Surface(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(viewModel.todayMonthIndex)
                                    }
                                }
                                .testTag("jump_today_button"),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = "今日",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "今",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        // Settings menu button
                        IconButton(
                            onClick = { viewModel.openSettingsDialog() },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Sticky Minimalist Weekday Header Row (一, 二, 三, 四, 五, 六, 日)
                WeekdayHeaderRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    thickness = 0.8.dp
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Infinite Scrolling List of Months
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("infinite_calendar_list")
            ) {
                items(
                    count = CalendarViewModel.TOTAL_MONTHS,
                    key = { index -> index }
                ) { index ->
                    val (year, month) = CalendarViewModel.getYearMonthFromIndex(index)
                    val days = remember(year, month, config) {
                        viewModel.getDaysForMonth(year, month)
                    }

                    MinimalMonthSection(
                        year = year,
                        month = month,
                        days = days,
                        selectedDay = selectedDay,
                        onDayClick = { day ->
                            viewModel.selectDay(if (selectedDay == day) null else day)
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Minimalist Selected Day Details Toast / Bottom Card
            AnimatedVisibility(
                visible = selectedDay != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                selectedDay?.let { day ->
                    SelectedDayDetailPill(
                        dayInfo = day,
                        onDismiss = { viewModel.selectDay(null) }
                    )
                }
            }
        }
    }

    // Settings & Theme Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentThemeMode = themeMode,
            currentConfigUrl = viewModel.getConfigUrl(),
            syncState = syncState,
            onThemeModeChange = { viewModel.setThemeMode(it) },
            onSaveConfigUrl = { viewModel.updateConfigUrlAndRefresh(it) },
            onResetDefaultUrl = { viewModel.resetConfigUrlToDefault() },
            onRefreshConfig = { viewModel.refreshConfig() },
            onDismiss = { viewModel.closeSettingsDialog() }
        )
    }
}

/**
 * Minimal floating pill card when a user clicks a specific day in the calendar
 */
@Composable
private fun SelectedDayDetailPill(
    dayInfo: DayInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("selected_day_detail_pill"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${dayInfo.year}年${dayInfo.month}月${dayInfo.day}日",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dayInfo.dayOfWeekChinese,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tag indicator:
                    // If locale festival (e.g. 三月三) -> Display "三月三" in Green!
                    // Else -> Display "休息日" (Red) or "工作日" (Non-red)
                    if (dayInfo.isLocaleHoliday) {
                        Text(
                            text = dayInfo.localeHolidayName ?: "三月三",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LOCALE_FESTIVAL_COLOR
                        )
                    } else {
                        Text(
                            text = if (dayInfo.isRestDay) "休息日" else "工作日",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dayInfo.isRestDay) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (dayInfo.fullLunarText.isNotBlank()) {
                        Text(
                            text = dayInfo.fullLunarText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (dayInfo.festivalName != null && !dayInfo.isLocaleHoliday) {
                        Text(
                            text = "· ${dayInfo.festivalName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dayInfo.isHolidayWithRest) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
