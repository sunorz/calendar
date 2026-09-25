package com.sunplace.calendar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.DayInfo
import com.sunplace.calendar.data.model.TodayPattern
import com.sunplace.calendar.util.TranslationHelper

val LOCALE_FESTIVAL_COLOR = Color(0xFF2E7D32) // Fresh Green for locale festivals like "三月三"

val AbrilFatfaceFontFamily = FontFamily(
    Font(R.font.abril_fatface, FontWeight.Normal)
)

val TodayBevelBorderBrush = Brush.verticalGradient(
    colors = listOf(Color.White.copy(alpha = 0.50f), Color.Black.copy(alpha = 0.15f))
)

private val TodayWorkdayBrush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFEAB308)))
private val TodayRestdayBrush = Brush.verticalGradient(listOf(Color(0xFFFF8585), Color(0xFFE53935)))
private val TodayLocaleBrush = Brush.verticalGradient(listOf(Color(0xFF6EE7B7), Color(0xFF22C55E)))

fun getTodayCardBrush(colorModeIndex: Int): Brush = when (colorModeIndex) {
    1 -> TodayRestdayBrush
    2 -> TodayLocaleBrush
    else -> TodayWorkdayBrush
}

fun getTodayCardBrush(isExplicitLocaleHoliday: Boolean, isRestDay: Boolean): Brush = when {
    isExplicitLocaleHoliday -> TodayLocaleBrush
    isRestDay -> TodayRestdayBrush
    else -> TodayWorkdayBrush
}

fun getTodayPatternTintColor(colorModeIndex: Int): Color =
    if (colorModeIndex == 0) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.55f)

fun getTodayPatternTintColor(isExplicitLocaleHoliday: Boolean, isRestDay: Boolean): Color =
    if (!isExplicitLocaleHoliday && !isRestDay) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.55f)

private val WEEKDAY_RES_IDS = listOf(
    R.string.weekday_mon,
    R.string.weekday_tue,
    R.string.weekday_wed,
    R.string.weekday_thu,
    R.string.weekday_fri,
    R.string.weekday_sat,
    R.string.weekday_sun
)

/**
 * Minimalist Weekday Header Row
 * Displays Mon-Sun using localized string resources.
 */
@Composable
fun WeekdayHeaderRow(
    isElderlyMode: Boolean = false,
    horizontalPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val restColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = if (isElderlyMode) 8.dp else 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(if (isElderlyMode) 3.dp else 2.dp)
    ) {
        WEEKDAY_RES_IDS.forEachIndexed { index, resId ->
            val isWeekend = index >= 5
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(resId),
                    textAlign = TextAlign.Center,
                    fontWeight = if (isElderlyMode) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = if (isElderlyMode) 18.sp else 13.sp,
                    color = if (isWeekend) restColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Minimalist Month Section for the infinite scrolling list
 */
@Composable
fun MinimalMonthSection(
    year: Int,
    month: Int,
    days: List<DayInfo>,
    selectedDay: DayInfo?,
    onDayClick: (DayInfo) -> Unit,
    isElderlyMode: Boolean = false,
    showWeekdayHeader: Boolean = false,
    todayPattern: TodayPattern = TodayPattern.DEFAULT,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isElderlyMode) 10.dp else 12.dp,
                vertical = if (isElderlyMode) 16.dp else 14.dp
            )
            .testTag("month_section_${year}_$month")
    ) {
        // Month Title Header (Minimalist & Crisp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = if (isElderlyMode) 6.dp else 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isElderlyMode) {
                val monthTitle = if (TranslationHelper.isEnglish()) {
                    "${TranslationHelper.getMonthName(month)} $year"
                } else {
                    stringResource(R.string.year_month_format, year, month)
                }
                Text(
                    text = monthTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                if (month == 1) {
                    Text(
                        text = stringResource(R.string.single_year_format, year),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                }
                val monthTitle = if (TranslationHelper.isEnglish()) {
                    TranslationHelper.getMonthName(month)
                } else {
                    stringResource(R.string.single_month_format, month)
                }
                Text(
                    text = monthTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isElderlyMode) 8.dp else 6.dp))

        if (showWeekdayHeader) {
            WeekdayHeaderRow(
                isElderlyMode = isElderlyMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        // Days Grid: Empty leading spaces + Month days
        val firstDayOfWeek = days.firstOrNull()?.dayOfWeekNumber ?: 1
        val leadingEmptyCount = firstDayOfWeek - 1
        val totalCells = leadingEmptyCount + days.size
        val rows = (totalCells + 6) / 7

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (isElderlyMode) 6.dp else 4.dp)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isElderlyMode) 3.dp else 2.dp)
                ) {
                    for (c in 0 until 7) {
                        val dayIndex = (r * 7 + c) - leadingEmptyCount
                        if (dayIndex in days.indices) {
                            val dayInfo = days[dayIndex]
                            val isSelected = selectedDay != null &&
                                selectedDay.year == dayInfo.year &&
                                selectedDay.month == dayInfo.month &&
                                selectedDay.day == dayInfo.day

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                MinimalDayCell(
                                    dayInfo = dayInfo,
                                    isSelected = isSelected,
                                    isElderlyMode = isElderlyMode,
                                    todayPattern = todayPattern,
                                    onClick = { onDayClick(dayInfo) }
                                )
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimalist Day Cell:
 * Supports standard mode and Elderly Care mode (适老关怀模式)
 */
@Composable
fun MinimalDayCell(
    dayInfo: DayInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    isElderlyMode: Boolean = false,
    todayPattern: TodayPattern = TodayPattern.DEFAULT,
    modifier: Modifier = Modifier
) {
    val isLightMode = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val restColor = MaterialTheme.colorScheme.error
    val workColor = MaterialTheme.colorScheme.onSurface
    val selectionBorderColor = MaterialTheme.colorScheme.primary

    val numColor = when {
        dayInfo.isExplicitLocaleHoliday -> LOCALE_FESTIVAL_COLOR
        dayInfo.isRestDay -> restColor
        else -> workColor
    }

    val cellAspectRatio = if (isElderlyMode) 1.0f else 0.92f

    val todayBrush = remember(dayInfo.isExplicitLocaleHoliday, dayInfo.isRestDay) {
        getTodayCardBrush(dayInfo.isExplicitLocaleHoliday, dayInfo.isRestDay)
    }

    val todayTreeRingColor = remember(dayInfo.isExplicitLocaleHoliday, dayInfo.isRestDay) {
        getTodayPatternTintColor(dayInfo.isExplicitLocaleHoliday, dayInfo.isRestDay)
    }
    val todayTextColor = Color(0xFF000000)

    val cellClipShape = if (isElderlyMode) RoundedCornerShape(12.dp) else RoundedCornerShape(10.dp)

    val cellModifier = when {
        isElderlyMode -> when {
            isSelected -> Modifier.background(color = MaterialTheme.colorScheme.primary, shape = cellClipShape)
            dayInfo.isToday -> Modifier
                .background(
                    color = if (isLightMode) Color(0xFFF7F2EC) else Color(0xFF2C2723),
                    shape = cellClipShape
                )
                .border(
                    width = 1.5.dp,
                    color = Color(0xFFB8251B).copy(alpha = 0.6f),
                    shape = cellClipShape
                )
            else -> Modifier
        }
        dayInfo.isToday -> {
            val baseBg = Modifier.background(brush = todayBrush, shape = cellClipShape)
            if (isSelected) {
                baseBg.border(width = 1.5.dp, color = selectionBorderColor, shape = cellClipShape)
            } else {
                baseBg.border(width = 0.8.dp, brush = TodayBevelBorderBrush, shape = cellClipShape)
            }
        }
        isSelected -> Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = cellClipShape
            )
            .border(width = 1.5.dp, color = selectionBorderColor, shape = cellClipShape)
        else -> Modifier
    }

    val displayNumColor = if (isElderlyMode && isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        numColor
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cellAspectRatio)
            .clip(cellClipShape)
            .then(cellModifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val holeOffset = 5.dp
        val holeSize = 6.dp

        if (dayInfo.isToday && !isElderlyMode) {
            val patternSize = maxOf(maxWidth, maxHeight) * 1.12f
            val centerXDp = maxWidth * TodayPatternHolder.normCenterX
            val centerYDp = maxHeight * TodayPatternHolder.normCenterY

            Image(
                painter = painterResource(id = todayPattern.drawableRes),
                contentDescription = todayPattern.getDisplayName(TranslationHelper.isEnglish()),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(patternSize)
                    .graphicsLayer {
                        translationX = (centerXDp - (patternSize / 2)).toPx()
                        translationY = (centerYDp - (patternSize / 2)).toPx()
                        rotationZ = TodayPatternHolder.angle
                        transformOrigin = TransformOrigin.Center
                    },
                colorFilter = ColorFilter.tint(todayTreeRingColor)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = holeOffset, top = holeOffset)
                    .size(holeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        width = 0.8.dp,
                        brush = TodayBevelBorderBrush,
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 1.dp, vertical = if (isElderlyMode) 2.dp else 3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isElderlyMode) {
                Text(
                    text = dayInfo.day.toString(),
                    color = displayNumColor,
                    fontSize = if (dayInfo.isToday) 23.sp else 21.sp,
                    fontWeight = if (dayInfo.isToday) FontWeight.ExtraBold else FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            } else if (dayInfo.isToday) {
                Text(
                    text = dayInfo.day.toString(),
                    color = todayTextColor,
                    fontSize = 24.sp,
                    fontFamily = AbrilFatfaceFontFamily,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            } else {
                val subText = TranslationHelper.getFilteredDisplayNote(dayInfo)
                val subColor = if (dayInfo.isHolidayWithRest) restColor else MaterialTheme.colorScheme.onSurfaceVariant
                val isEn = TranslationHelper.isEnglish()
                val subFontSize = when {
                    isEn && subText.length > 7 -> 7.5.sp
                    isEn && subText.length > 5 -> 8.sp
                    else -> 9.sp
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayInfo.day.toString(),
                        color = numColor,
                        fontSize = 15.sp,
                        fontWeight = if (dayInfo.isRestDay) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().height(20.dp)
                    ) {
                        Text(
                            text = subText.ifBlank { " " },
                            color = subColor,
                            fontSize = subFontSize,
                            fontWeight = if (dayInfo.isHolidayWithRest) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            style = LocalTextStyle.current.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns the string resource ID for full weekday name (Monday..Sunday)
 */
fun getFullWeekdayResId(dayOfWeekNumber: Int): Int {
    return when (dayOfWeekNumber) {
        1 -> R.string.weekday_full_mon
        2 -> R.string.weekday_full_tue
        3 -> R.string.weekday_full_wed
        4 -> R.string.weekday_full_thu
        5 -> R.string.weekday_full_fri
        6 -> R.string.weekday_full_sat
        7 -> R.string.weekday_full_sun
        else -> R.string.weekday_full_mon
    }
}