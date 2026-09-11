package com.sunplace.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.CalendarConfig
import com.sunplace.calendar.data.model.DayInfo
import com.sunplace.calendar.util.LunarCalculator
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

    val nextYearConfig by viewModel.nextYearConfig.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = viewModel.todayMonthIndex
    )

    // Today's DayInfo fallback if no date is explicitly clicked
    val todayInfo = remember(config, nextYearConfig) {
        val cal = LunarCalculator.getChinaCalendar()
        LunarCalculator.getDayInfoForDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            config,
            nextYearConfig
        )
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

        // Helper to compute optimal vertical scroll offset so Today is comfortably visible and not cut off
        val computeTodayScrollOffset = remember(columnCount, isElderlyMode, totalHeight) {
            {
                val cal = LunarCalculator.getChinaCalendar()
                val todayDay = cal.get(Calendar.DAY_OF_MONTH)
                val todayYear = cal.get(Calendar.YEAR)
                val todayMonth = cal.get(Calendar.MONTH) + 1
                val days = viewModel.getDaysForMonth(todayYear, todayMonth)
                val firstDayOfWeek = days.firstOrNull()?.dayOfWeekNumber ?: 1
                val leadingEmpty = firstDayOfWeek - 1
                val todayCellIndex = leadingEmpty + (todayDay - 1)
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

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT AREA: Compact Top Bar + Dynamic Multi-Column Month Grid (Seamless Transparent Canvas)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Compact Landscape Top Bar (Spans full left width to screen edge x=0)
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
                        // Title & Icon
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

                        // Actions: Today Button + Mode Switch + Settings (Text in elderly mode, icons in standard mode)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isElderlyMode) 6.dp else 4.dp)
                        ) {
                            // Quick "Today" Jump Button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        viewModel.selectToday()
                                        coroutineScope.launch {
                                            gridState.animateScrollToItem(
                                                index = viewModel.todayMonthIndex,
                                                scrollOffset = computeTodayScrollOffset()
                                            )
                                        }
                                    }
                                    .testTag("large_screen_jump_today_top_button"),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isElderlyMode) 0.18f else 0.12f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                if (isElderlyMode) {
                                    Text(
                                        text = stringResource(R.string.elderly_btn_today),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 3.5.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Today,
                                            contentDescription = stringResource(R.string.today_btn),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.today_btn_short),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (isElderlyMode) {
                                // Mode toggle as clear text
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

                                // Settings / Theme Color as clear text
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
                                // Mode toggle icon
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

                                // Settings icon
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

                // Multi-Column Month Grid (1 to 4 columns, Transparent Canvas seamlessly blending with background)
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
                        val days = remember(year, month, config) {
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
                                showWeekdayHeader = true, // Each month column in grid has its own self-contained week header
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
                        gridState.animateScrollToItem(
                            index = viewModel.todayMonthIndex,
                            scrollOffset = computeTodayScrollOffset()
                        )
                    }
                },
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
 * Vertically balanced and cohesive card layout without scattered spacing.
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
            border = androidx.compose.foundation.BorderStroke(
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
                // Gregorian Date & Weekday
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

                // Workday / Restday & Today status badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dayInfo.isLocaleHoliday) {
                        val tagBgColor = if (isElderlyMode) neutralBadgeBg else LOCALE_FESTIVAL_COLOR.copy(alpha = 0.14f)
                        val tagTextColor = if (isElderlyMode) neutralBadgeText else LOCALE_FESTIVAL_COLOR
                        Surface(
                            shape = RoundedCornerShape(if (isElderlyMode) 10.dp else 6.dp),
                            color = tagBgColor
                        ) {
                            Text(
                                text = com.sunplace.calendar.util.TranslationHelper.translateFestival(dayInfo.localeHolidayName) ?: "Shangsi",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = if (isElderlyMode) 15.sp else 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagTextColor
                            )
                        }
                    } else {
                        val isRest = dayInfo.isRestDay
                        val tagBgColor = if (isElderlyMode) {
                            neutralBadgeBg
                        } else if (isRest) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        }
                        val tagTextColor = if (isElderlyMode) {
                            neutralBadgeText
                        } else if (isRest) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
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
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )

                // Lunar & Solar Term Details (Follows card lunar priority rule)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val lunarPriority = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(dayInfo)

                    if (lunarPriority.hasFestival) {
                        val festBgColor = if (isElderlyMode) {
                            neutralBadgeBg
                        } else if (lunarPriority.isHolidayWithRest) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        }
                        val festTextColor = if (isElderlyMode) {
                            neutralBadgeText
                        } else if (lunarPriority.isHolidayWithRest) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
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

                    val fullLunar = com.sunplace.calendar.util.TranslationHelper.getLocalizedFullLunarText(dayInfo)
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

                // Return to Today Button
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
