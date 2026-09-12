package com.sunplace.calendar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.DayInfo
import com.sunplace.calendar.util.LunarCalculator
import java.util.Calendar

/**
 * Ultra-Small Screen / Smartwatch (Wear OS) / Micro Outer-screen Adaptive View.
 * Displays calendar in a day-by-day widget card format:
 * - Full vertical drag/swipe gestures to change days smoothly one by one.
 * - Minimal top navigation bar with quick "Today" jump, cross-date picker menu, and settings.
 * - Big, legible, high-contrast typography optimized for micro screens.
 */
@Composable
fun WatchDayWidgetView(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val nextYearConfig by viewModel.nextYearConfig.collectAsState()
    val isElderlyMode by viewModel.isElderlyMode.collectAsState()

    // Current selected date state in Watch mode (defaults to current real date in UTC+8)
    var currentYear by remember { mutableIntStateOf(LunarCalculator.getChinaCalendar().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(LunarCalculator.getChinaCalendar().get(Calendar.MONTH) + 1) }
    var currentDay by remember { mutableIntStateOf(LunarCalculator.getChinaCalendar().get(Calendar.DAY_OF_MONTH)) }

    // Dialog state for cross-date quick picker
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Compute DayInfo for current date
    val dayInfo = remember(currentYear, currentMonth, currentDay, config, nextYearConfig) {
        LunarCalculator.getDayInfoForDate(currentYear, currentMonth, currentDay, config, nextYearConfig)
    }

    // Helper functions to step days
    fun stepDay(delta: Int) {
        val cal = LunarCalculator.getChinaCalendar().apply {
            set(currentYear, currentMonth - 1, currentDay)
            add(Calendar.DAY_OF_MONTH, delta)
        }
        val clampedYear = cal.get(Calendar.YEAR).coerceIn(CalendarViewModel.START_YEAR, CalendarViewModel.END_YEAR)
        cal.set(Calendar.YEAR, clampedYear)
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH) + 1
        currentDay = cal.get(Calendar.DAY_OF_MONTH)
    }

    fun jumpToToday() {
        val cal = LunarCalculator.getChinaCalendar()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH) + 1
        currentDay = cal.get(Calendar.DAY_OF_MONTH)
    }

    // Accumulate drag delta for gesture threshold
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 50f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragAccumulator < -dragThreshold) {
                            // Swiped up -> next day
                            stepDay(1)
                        } else if (dragAccumulator > dragThreshold) {
                            // Swiped down -> previous day
                            stepDay(-1)
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = {
                        dragAccumulator = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    }
                )
            }
            .testTag("watch_day_widget_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact Top Bar for Watch / Micro Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // "今" Jump button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { jumpToToday() }
                    .testTag("watch_today_jump_button"),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = stringResource(R.string.today_btn_short),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            // Center: Compact Date Picker Menu Button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDatePickerDialog = true }
                    .testTag("watch_date_menu_button"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val pickerText = if (com.sunplace.calendar.util.TranslationHelper.isEnglish()) {
                        "${com.sunplace.calendar.util.TranslationHelper.getMonthName(currentMonth).take(3)} $currentYear"
                    } else {
                        "${currentYear}.${currentMonth}"
                    }
                    Text(
                        text = pickerText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.watch_select_date),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Right icons: Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.toggleElderlyMode() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isElderlyMode) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.watch_mode_toggle),
                        tint = if (isElderlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.openSettingsDialog() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Subtitle Up Arrow indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = stringResource(R.string.watch_prev_day),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .size(16.dp)
                .clickable { stepDay(-1) }
        )

        // Main Day Widget Card with smooth transition
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = dayInfo,
                transitionSpec = {
                    slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                },
                label = "watch_day_card_transition"
            ) { targetDay ->
                WatchDayCard(
                    dayInfo = targetDay,
                    isElderlyMode = isElderlyMode,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Down Arrow indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(R.string.watch_next_day),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .size(16.dp)
                .clickable { stepDay(1) }
        )
        Spacer(modifier = Modifier.height(2.dp))
    }

    // Cross-Date Quick Picker Dialog
    if (showDatePickerDialog) {
        WatchDatePickerDialog(
            initialYear = currentYear,
            initialMonth = currentMonth,
            initialDay = currentDay,
            onDismiss = { showDatePickerDialog = false },
            onConfirm = { y, m, d ->
                currentYear = y
                currentMonth = m
                currentDay = d
                showDatePickerDialog = false
            }
        )
    }
}

/**
 * High-legibility Day Widget Card for small screen / watch
 */
@Composable
private fun WatchDayCard(
    dayInfo: DayInfo,
    isElderlyMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Row 1: Weekday & Rest/Work Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(getFullWeekdayResId(dayInfo.dayOfWeekNumber)),
                    fontSize = if (isElderlyMode) 16.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // High-contrast Gray background, black text badge
                val isRest = dayInfo.isRestDay
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = if (isRest) stringResource(R.string.restday_label) else stringResource(R.string.workday_label),
                        fontSize = if (isElderlyMode) 13.sp else 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (dayInfo.isLocaleHoliday) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = com.sunplace.calendar.util.TranslationHelper.translateFestival(dayInfo.localeHolidayName ?: "三月三") ?: "三月三",
                            fontSize = if (isElderlyMode) 13.sp else 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (dayInfo.isToday) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.today_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Row 2: Giant Day Number
            Text(
                text = dayInfo.day.toString(),
                fontSize = if (isElderlyMode) 64.sp else 54.sp,
                fontWeight = FontWeight.Black,
                color = if (dayInfo.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                lineHeight = if (isElderlyMode) 68.sp else 58.sp,
                textAlign = TextAlign.Center
            )

            // Row 3: Year & Month
            val yearMonthText = com.sunplace.calendar.util.TranslationHelper.formatYearMonth(dayInfo.year, dayInfo.month)
            Text(
                text = yearMonthText,
                fontSize = if (isElderlyMode) 15.sp else 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Row 4 & 5: Card Lunar Priority (Festival + Solar Term, or Solar Term, or Ordinary Lunar)
            val lunarPriority = com.sunplace.calendar.util.TranslationHelper.getCardLunarPriorityData(dayInfo)

            if (lunarPriority.hasFestival) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = lunarPriority.festivalText!!,
                        fontSize = if (isElderlyMode) 14.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }
                if (!lunarPriority.isLunarFestival) {
                    val fullLunarText = com.sunplace.calendar.util.TranslationHelper.getLocalizedFullLunarText(dayInfo)
                    if (fullLunarText.isNotBlank()) {
                        Text(
                            text = (stringResource(R.string.lunar_label) + " " + fullLunarText).trim(),
                            fontSize = if (isElderlyMode) 13.5.sp else 11.5.sp,
                            lineHeight = if (isElderlyMode) 17.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            softWrap = true,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (lunarPriority.hasSolarTerm) {
                    Text(
                        text = lunarPriority.solarTermText!!,
                        fontSize = if (isElderlyMode) 13.sp else 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (lunarPriority.hasSolarTerm) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = lunarPriority.solarTermText!!,
                        fontSize = if (isElderlyMode) 14.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }
            } else {
                val fullLunarText = com.sunplace.calendar.util.TranslationHelper.getLocalizedFullLunarText(dayInfo)
                Text(
                    text = (stringResource(R.string.lunar_label) + " " + fullLunarText).trim(),
                    fontSize = if (isElderlyMode) 13.5.sp else 11.5.sp,
                    lineHeight = if (isElderlyMode) 17.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    softWrap = true,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Compact Cross-Date Quick Picker Dialog for small screen
 */
@Composable
private fun WatchDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit
) {
    var selYear by remember { mutableIntStateOf(initialYear) }
    var selMonth by remember { mutableIntStateOf(initialMonth) }
    var selDay by remember { mutableIntStateOf(initialDay) }

    val maxDaysInMonth = remember(selYear, selMonth) {
        val cal = LunarCalculator.getChinaCalendar().apply {
            set(Calendar.YEAR, selYear)
            set(Calendar.MONTH, selMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Clamp selDay
    if (selDay > maxDaysInMonth) {
        selDay = maxDaysInMonth
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.watch_quick_jump),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Year selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.watch_year_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (selYear > CalendarViewModel.START_YEAR) selYear-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = stringResource(R.string.single_year_format, selYear),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { if (selYear < CalendarViewModel.END_YEAR) selYear++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Month selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.watch_month_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (selMonth > 1) selMonth-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = stringResource(R.string.single_month_format, selMonth),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { if (selMonth < 12) selMonth++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Day selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.watch_day_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (selDay > 1) selDay-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "$selDay",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { if (selDay < maxDaysInMonth) selDay++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { onConfirm(selYear, selMonth, selDay) }) {
                        Text(stringResource(R.string.btn_jump), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
