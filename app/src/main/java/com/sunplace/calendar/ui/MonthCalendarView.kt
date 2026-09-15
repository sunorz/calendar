package com.sunplace.calendar.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.DayInfo

val LOCALE_FESTIVAL_COLOR = Color(0xFF2E7D32) // Fresh Green for locale festivals like "三月三"

/**
 * Minimalist Weekday Header Row
 * Displays Mon-Sun using localized string resources.
 */
@Composable
fun WeekdayHeaderRow(
    isElderlyMode: Boolean = false,
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val restColor = MaterialTheme.colorScheme.error
    val weekdayResIds = listOf(
        R.string.weekday_mon,
        R.string.weekday_tue,
        R.string.weekday_wed,
        R.string.weekday_thu,
        R.string.weekday_fri,
        R.string.weekday_sat,
        R.string.weekday_sun
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = if (isElderlyMode) 8.dp else 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(if (isElderlyMode) 3.dp else 2.dp)
    ) {
        weekdayResIds.forEachIndexed { index, resId ->
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
                // In elderly mode: Always show Year and Month together clearly
                val monthTitle = if (com.sunplace.calendar.util.TranslationHelper.isEnglish()) {
                    "${com.sunplace.calendar.util.TranslationHelper.getMonthName(month)} $year"
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
                val monthTitle = if (com.sunplace.calendar.util.TranslationHelper.isEnglish()) {
                    com.sunplace.calendar.util.TranslationHelper.getMonthName(month)
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
                        val cellIndex = r * 7 + c
                        val dayIndex = cellIndex - leadingEmptyCount

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
 * - Large legible fonts
 * - Auspicious Chinese dual-color palette (Cinnabar Red for rest/festivals, Warm Ink/Moon Silk for workdays)
 * - No harsh stark black-and-white blocks
 * - Clear statutory rest/work tag indicators
 */
@Composable
fun MinimalDayCell(
    dayInfo: DayInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    isElderlyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isAppDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val restColor = MaterialTheme.colorScheme.error
    val workColor = MaterialTheme.colorScheme.onSurface
    val selectionBorderColor = MaterialTheme.colorScheme.primary
    val isLightMode = !isAppDark
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val density = LocalDensity.current

    val numColor = when {
        dayInfo.isExplicitLocaleHoliday -> LOCALE_FESTIVAL_COLOR
        dayInfo.isRestDay -> restColor
        else -> workColor
    }

    // In elderly mode, cells are squarish and focus purely on huge numbers
    val cellAspectRatio = if (isElderlyMode) 1.0f else 0.92f

    // Standard Mode Today: Pure background color change without border (when selected, applies selection border)
    val todayBgColor = if (isLightMode) com.sunplace.calendar.ui.theme.TodayHighlightLight else com.sunplace.calendar.ui.theme.TodayHighlightDark
    val todayTreeRingColor = if (isLightMode) com.sunplace.calendar.ui.theme.TodayTreeRingLight else com.sunplace.calendar.ui.theme.TodayTreeRingDark

    // Today & Selection cell styling
    val cellModifier = when {
        isElderlyMode -> {
            when {
                isSelected -> Modifier.background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
                dayInfo.isToday -> Modifier
                    .background(
                        color = if (isLightMode) Color(0xFFF7F2EC) else Color(0xFF2C2723),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFB8251B).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                else -> Modifier
            }
        }
        dayInfo.isToday -> {
            if (isSelected) {
                Modifier
                    .background(
                        color = todayBgColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = selectionBorderColor,
                        shape = RoundedCornerShape(10.dp)
                    )
            } else {
                Modifier.background(
                    color = todayBgColor,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
        isSelected -> {
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.5.dp,
                    color = selectionBorderColor,
                    shape = RoundedCornerShape(10.dp)
                )
        }
        else -> Modifier
    }

    // Calculate text color (when selected in elderly mode with solid background, use high-contrast onPrimary)
    val displayNumColor = if (isElderlyMode && isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        numColor
    }

    val cellClipShape = if (isElderlyMode) RoundedCornerShape(12.dp) else RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cellAspectRatio)
            .clip(cellClipShape)
            .then(cellModifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Today SVG Vector Icon (Shifted left by 25dp from right border center, clipped by cell shape)
        if (dayInfo.isToday && !isElderlyMode) {
            Image(
                painter = painterResource(id = R.drawable.ic_today_pinwheel),
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .rotate(TodayRotationHolder.angle),
                colorFilter = ColorFilter.tint(todayTreeRingColor)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 1.dp, vertical = if (isElderlyMode) 2.dp else 3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isElderlyMode) {
                // Elderly Mode: Clear, bold, readable numbers (comfortably fitting two digits on any screen size/landscape)
                Text(
                    text = dayInfo.day.toString(),
                    color = displayNumColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                // Standard Mode: Day number with lunar/festival subtitle
                val subText = com.sunplace.calendar.util.TranslationHelper.getFilteredDisplayNote(dayInfo)
                val subColor = when {
                    dayInfo.isHolidayWithRest -> restColor
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val isEn = com.sunplace.calendar.util.TranslationHelper.isEnglish()
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
                        fontWeight = if (dayInfo.isRestDay || dayInfo.isToday) FontWeight.Bold else FontWeight.SemiBold,
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
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                                    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                                    trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
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

/**
 * Draws a stroke-only outline of [text] using native Android Paint, in [strokeColor].
 * Meant to sit directly behind a normally-filled Text() of the same string/size,
 * so a tree-ring texture (or any busy background) is visually blocked around the
 * glyph edges. Reusable for both the day number and the lunar/festival subtitle.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.TodayTextStroke(
    text: String,
    fontSizeSp: Float,
    strokeColor: Color,
    strokeWidthDp: Float,
    bold: Boolean,
    density: androidx.compose.ui.unit.Density
) {
    val strokeColorArgb = strokeColor.toArgb()
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }
    val strokeWidthPx = with(density) { strokeWidthDp.dp.toPx() }
    Canvas(modifier = Modifier.matchParentSize()) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeMiter = 2f
                color = strokeColorArgb
                textSize = fontSizePx
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            }
            val bounds = android.graphics.Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val cx = size.width / 2f
            val cy = size.height / 2f + bounds.height() / 2f
            drawText(text, cx, cy, paint)
        }
    }
}