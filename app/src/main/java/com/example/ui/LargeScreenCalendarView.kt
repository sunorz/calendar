package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarConfig
import com.example.data.model.DayInfo
import com.example.util.LunarCalculator
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Large Screen / Foldable / Tablet / Android TV / Landscape Adaptive Layout.
 * Features:
 * - Dynamic 1 to 4 month columns side-by-side depending on screen width (up to TV 4K resolution).
 * - Persistent right-hand Master-Detail Pane (no bottom sheet overlay obstructing the calendar).
 * - Each month in multi-column mode renders its own self-contained Weekday header (一 ~ 日).
 * - Clicking any date immediately reflects in the right detail panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeScreenCalendarView(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val isElderlyMode by viewModel.isElderlyMode.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = viewModel.todayMonthIndex
    )

    // Today's DayInfo fallback if no date is explicitly clicked
    val todayInfo = remember(config) {
        val cal = Calendar.getInstance()
        LunarCalculator.getDayInfoForDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            config
        )
    }

    val displayDay = selectedDay ?: todayInfo

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val totalWidth = maxWidth

        // Calculate right panel width based on total screen width
        val rightPanelWidth: Dp = when {
            totalWidth >= 1400.dp -> 380.dp // TV or large display
            totalWidth >= 840.dp -> 340.dp  // Large tablet or laptop
            else -> 280.dp                 // Landscape phone / compact tablet
        }

        // Available width for the month grid area
        val gridAreaWidth = totalWidth - rightPanelWidth

        // Adaptive column count for months (1 to 4 columns)
        val columnCount = if (isElderlyMode) {
            when {
                gridAreaWidth >= 1350.dp -> 3  // TV / Ultra-wide: 3 months
                gridAreaWidth >= 880.dp -> 2   // Large tablet: 2 months
                else -> 1                      // Tablet / Landscape phone: 1 spacious month
            }
        } else {
            when {
                gridAreaWidth >= 1200.dp -> 4  // TV / 4K landscape: 4 months
                gridAreaWidth >= 820.dp -> 3   // Desktop / Ultra-wide: 3 months
                gridAreaWidth >= 520.dp -> 2   // Tablet: 2 months
                else -> 1                      // Landscape phone: 1 month
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT AREA: Top App Bar + Dynamic Multi-Column Month Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Large Screen Top Bar
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isElderlyMode) 28.dp else 22.dp)
                            )
                            Text(
                                text = "智能万年历",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isElderlyMode) 22.sp else 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Quick "Today" Jump Button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .padding(end = 6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.selectToday()
                                        coroutineScope.launch {
                                            gridState.animateScrollToItem(viewModel.todayMonthIndex)
                                        }
                                    },
                                    modifier = Modifier.size(if (isElderlyMode) 32.dp else 26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = "今日",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(if (isElderlyMode) 22.dp else 18.dp)
                                    )
                                }
                                Text(
                                    text = "今",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = if (isElderlyMode) 16.sp else 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Mode toggle
                        IconButton(
                            onClick = { viewModel.toggleElderlyMode() },
                            modifier = Modifier.size(if (isElderlyMode) 48.dp else 40.dp)
                        ) {
                            Icon(
                                imageVector = if (isElderlyMode) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "模式切换",
                                tint = if (isElderlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (isElderlyMode) 26.dp else 22.dp)
                            )
                        }

                        // Settings
                        IconButton(
                            onClick = { viewModel.openSettingsDialog() },
                            modifier = Modifier.size(if (isElderlyMode) 48.dp else 40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (isElderlyMode) 26.dp else 22.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )

                // Multi-Column Month Grid (1 to 4 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("large_screen_month_grid")
                ) {
                    items(
                        count = CalendarViewModel.TOTAL_MONTHS,
                        key = { index -> index }
                    ) { index ->
                        val (year, month) = CalendarViewModel.getYearMonthFromIndex(index)
                        val days = remember(year, month, config) {
                            viewModel.getDaysForMonth(year, month)
                        }

                        Card(
                            modifier = Modifier
                                .padding(6.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            MinimalMonthSection(
                                year = year,
                                month = month,
                                days = days,
                                selectedDay = selectedDay,
                                isElderlyMode = isElderlyMode,
                                showWeekdayHeader = true, // Each card in grid has its own self-contained week header
                                onDayClick = { day ->
                                    val nextSelected = if (selectedDay == day) null else day
                                    viewModel.selectDay(nextSelected)
                                }
                            )
                        }
                    }
                }
            }

            // High-contrast Divider between Calendar Grid and Detail Panel
            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                thickness = 1.dp
            )

            // RIGHT AREA: Persistent Master-Detail Supporting Pane
            LargeScreenDetailPanel(
                dayInfo = displayDay,
                isElderlyMode = isElderlyMode,
                onJumpToday = {
                    viewModel.selectToday()
                    coroutineScope.launch {
                        gridState.animateScrollToItem(viewModel.todayMonthIndex)
                    }
                },
                modifier = Modifier
                    .width(rightPanelWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

/**
 * Persistent Right-hand Day Detail Panel for Large Screen / TV / Tablet.
 * No bottom sheet popup required; displays selected day details continuously.
 */
@Composable
private fun LargeScreenDetailPanel(
    dayInfo: DayInfo,
    isElderlyMode: Boolean,
    onJumpToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neutralBadgeBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val neutralBadgeText = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(width = 4.dp, height = 18.dp)
            ) {}
            Text(
                text = "日程与黄历详情",
                fontWeight = FontWeight.Bold,
                fontSize = if (isElderlyMode) 18.sp else 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Gregorian Date Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val dateTitle = if (dayInfo.isToday) {
                "${dayInfo.year}年${dayInfo.month}月${dayInfo.day}日 (今天)"
            } else {
                "${dayInfo.year}年${dayInfo.month}月${dayInfo.day}日"
            }
            Text(
                text = dateTitle,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isElderlyMode) 26.sp else 22.sp,
                lineHeight = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dayInfo.dayOfWeekChinese,
                fontSize = if (isElderlyMode) 20.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            thickness = 1.dp
        )

        // Workday / Restday status: Neutral gray bg + black text, NO red
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isRest = dayInfo.isRestDay
            val tagText = if (dayInfo.isLocaleHoliday) {
                dayInfo.localeHolidayName ?: "三月三"
            } else if (isRest) {
                "休息日"
            } else {
                "工作日"
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = neutralBadgeBg
            ) {
                Text(
                    text = tagText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = if (isElderlyMode) 18.sp else 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = neutralBadgeText
                )
            }

            if (dayInfo.isToday) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "今日",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = if (isElderlyMode) 16.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Lunar & Solar Term Details
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (dayInfo.fullLunarText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "农历：",
                        fontSize = if (isElderlyMode) 20.sp else 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dayInfo.fullLunarText,
                        fontSize = if (isElderlyMode) 21.sp else 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!dayInfo.solarTermName.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "节气：",
                        fontSize = if (isElderlyMode) 20.sp else 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dayInfo.solarTermName,
                        fontSize = if (isElderlyMode) 21.sp else 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Festival / Memorial / Commemoration: Gray bg + black text
            if (!dayInfo.festivalName.isNullOrBlank() && !dayInfo.isLocaleHoliday) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = neutralBadgeBg
                    ) {
                        Text(
                            text = dayInfo.festivalName,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = if (isElderlyMode) 19.sp else 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = neutralBadgeText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // Return to Today Button
        Button(
            onClick = onJumpToday,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isElderlyMode) 52.dp else 44.dp)
                .testTag("large_screen_jump_today_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Today,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "定位到今日",
                fontSize = if (isElderlyMode) 16.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
