package com.sunplace.calendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.DayInfo
import com.sunplace.calendar.util.TranslationHelper
import kotlinx.coroutines.launch

/**
 * Large Screen / Foldable / Tablet / Android TV / Landscape Adaptive Layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeScreenCalendarView(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val isElderlyMode by viewModel.isElderlyMode.collectAsState()
    val showGuangxiSanyuesan by viewModel.showGuangxiSanyuesan.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val todayPattern by viewModel.todayPattern.collectAsState()
    val currentTodayDate by viewModel.todayDate.collectAsState()
    val resetScrollEvent by viewModel.resetScrollEvent.collectAsState()

    val nextYearConfig by viewModel.nextYearConfig.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = viewModel.todayMonthIndex
    )

    val todayInfo = remember(currentTodayDate, config, nextYearConfig, showGuangxiSanyuesan) {
        viewModel.getTodayDayInfo()
    }

    val todayTextColor = when {
        todayInfo.isExplicitLocaleHoliday -> LOCALE_FESTIVAL_COLOR
        todayInfo.isRestDay -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val displayDay = selectedDay ?: todayInfo

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val density = LocalDensity.current

        val rightPanelWidth: Dp = when {
            totalWidth >= 1400.dp -> 380.dp
            totalWidth >= 840.dp -> 340.dp
            else -> 280.dp
        }

        val gridAreaWidth = totalWidth - rightPanelWidth

        val columnCount = if (isElderlyMode) {
            when {
                gridAreaWidth >= 1350.dp -> 3
                gridAreaWidth >= 880.dp -> 2
                else -> 1
            }
        } else {
            when {
                gridAreaWidth >= 1200.dp -> 4
                gridAreaWidth >= 820.dp -> 3
                gridAreaWidth >= 520.dp -> 2
                else -> 1
            }
        }

        val computeTodayScrollOffset = remember(currentTodayDate, columnCount, isElderlyMode, totalHeight) {
            {
                val todayDay = currentTodayDate.day
                val days = viewModel.getDaysForMonth(currentTodayDate.year, currentTodayDate.month)
                val firstDayOfWeek = days.firstOrNull()?.dayOfWeekNumber ?: 1
                val todayCellIndex = (firstDayOfWeek - 1) + (todayDay - 1)
                val todayRow = todayCellIndex / 7

                if (totalHeight < 550.dp || columnCount == 1) {
                    if (todayRow >= 1) {
                        val rowHeight = if (isElderlyMode) 58.dp else 48.dp
                        with(density) {
                            val offsetDp = (todayRow * rowHeight.value) + (if (isElderlyMode) 16 else 10)
                            offsetDp.dp.roundToPx()
                        }
                    } else {
                        0
                    }
                } else {
                    0
                }
            }
        }

        val jumpToTodayAction: () -> Unit = {
            viewModel.checkAndRefreshDate()
            viewModel.selectToday()
            coroutineScope.launch {
                gridState.animateScrollToItem(
                    index = viewModel.todayMonthIndex,
                    scrollOffset = computeTodayScrollOffset()
                )
            }
        }

        LaunchedEffect(resetScrollEvent) {
            if (resetScrollEvent > 0L) {
                gridState.scrollToItem(
                    index = viewModel.todayMonthIndex,
                    scrollOffset = computeTodayScrollOffset()
                )
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT AREA: Compact Top Bar + Dynamic Multi-Column Month Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Start))
                            .height(if (isElderlyMode) 46.dp else 40.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isElderlyMode) 24.dp else 20.dp)
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isElderlyMode) 19.sp else 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isElderlyMode) 6.dp else 4.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { jumpToTodayAction() }
                                    .testTag("large_screen_jump_today_top_button"),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                if (isElderlyMode) {
                                    Text(
                                        text = stringResource(R.string.elderly_btn_today),
                                        color = todayTextColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = todayPattern.drawableRes),
                                            contentDescription = stringResource(R.string.today_btn),
                                            colorFilter = ColorFilter.tint(todayTextColor),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.today_btn_short),
                                            color = todayTextColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (isElderlyMode) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { viewModel.toggleElderlyMode() }
                                        .testTag("large_screen_elderly_toggle_button"),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.elderly_btn_mode_exit),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { viewModel.openSettingsDialog() }
                                        .testTag("large_screen_settings_button"),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.elderly_btn_theme),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.toggleElderlyMode() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = stringResource(R.string.elderly_mode_enter),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.openSettingsDialog() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.settings_title),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("large_screen_month_grid")
                ) {
                    items(
                        count = CalendarViewModel.TOTAL_MONTHS,
                        key = { index -> index }
                    ) { index ->
                        val (year, month) = CalendarViewModel.getYearMonthFromIndex(index)
                        val days = remember(year, month, currentTodayDate, config, nextYearConfig, showGuangxiSanyuesan) {
                            viewModel.getDaysForMonth(year, month)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            MinimalMonthSection(
                                year = year,
                                month = month,
                                days = days,
                                selectedDay = selectedDay,
                                isElderlyMode = isElderlyMode,
                                showWeekdayHeader = true,
                                todayPattern = todayPattern,
                                onDayClick = { day ->
                                    val nextSelected = if (selectedDay == day) null else day
                                    viewModel.selectDay(nextSelected)
                                }
                            )

                            if (columnCount == 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    thickness = 0.6.dp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                thickness = 1.dp
            )

            LargeScreenDetailPanel(
                dayInfo = displayDay,
                isElderlyMode = isElderlyMode,
                onJumpToday = jumpToTodayAction,
                modifier = Modifier
                    .width(rightPanelWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.End + WindowInsetsSides.Bottom))
            )
        }
    }
}

/**
 * Persistent Right-hand Day Detail Panel for Large Screen / TV / Tablet.
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

    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("large_screen_detail_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(
                if (isElderlyMode) 1.5.dp else 1.dp,
                MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val dateTitle = if (dayInfo.isToday) {
                        stringResource(R.string.date_format_full_today, dayInfo.year, dayInfo.month, dayInfo.day)
                    } else {
                        stringResource(R.string.date_format_full, dayInfo.year, dayInfo.month, dayInfo.day)
                    }
                    Text(
                        text = dateTitle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isElderlyMode) 22.sp else 18.sp,
                        lineHeight = if (isElderlyMode) 26.sp else 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(getFullWeekdayResId(dayInfo.dayOfWeekNumber)),
                        fontSize = if (isElderlyMode) 17.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isRest = dayInfo.isRestDay
                    val tagBgColor = when {
                        isElderlyMode -> neutralBadgeBg
                        isRest -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    }
                    val tagTextColor = when {
                        isElderlyMode -> neutralBadgeText
                        isRest -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        shape = RoundedCornerShape(if (isElderlyMode) 10.dp else 6.dp),
                        color = tagBgColor
                    ) {
                        Text(
                            text = if (isRest) stringResource(R.string.restday_label) else stringResource(R.string.workday_label),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = if (isElderlyMode) 15.sp else 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagTextColor
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val lunarPriority = TranslationHelper.getCardLunarPriorityData(dayInfo)

                    if (lunarPriority.hasFestival) {
                        val festBgColor = when {
                            isElderlyMode -> neutralBadgeBg
                            lunarPriority.isHolidayWithRest -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        }
                        val festTextColor = when {
                            isElderlyMode -> neutralBadgeText
                            lunarPriority.isHolidayWithRest -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(if (isElderlyMode) 10.dp else 6.dp),
                            color = festBgColor
                        ) {
                            Text(
                                text = lunarPriority.festivalText!!,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = if (isElderlyMode) 16.sp else 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = festTextColor
                            )
                        }
                    }

                    if (lunarPriority.hasSolarTerm) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.solar_term_label),
                                fontSize = if (isElderlyMode) 16.sp else 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lunarPriority.solarTermText!!,
                                fontSize = if (isElderlyMode) 17.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isElderlyMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val fullLunar = TranslationHelper.getLocalizedFullLunarText(dayInfo)
                    if (fullLunar.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lunar_label),
                                fontSize = if (isElderlyMode) 16.sp else 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = fullLunar,
                                fontSize = if (isElderlyMode) 17.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isElderlyMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )

                Button(
                    onClick = onJumpToday,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isElderlyMode) 48.dp else 40.dp)
                        .testTag("large_screen_jump_today_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isElderlyMode) neutralBadgeBg else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isElderlyMode) neutralBadgeText else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    if (!isElderlyMode) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = stringResource(R.string.jump_today),
                        fontSize = if (isElderlyMode) 17.sp else 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
