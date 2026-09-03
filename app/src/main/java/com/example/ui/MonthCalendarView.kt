package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayInfo

val WEEK_DAYS_MINIMAL = listOf("一", "二", "三", "四", "五", "六", "日")
val LOCALE_FESTIVAL_COLOR = Color(0xFF2E7D32) // Fresh Green for locale festivals like "三月三"

/**
 * Minimalist Weekday Header Row (一, 二, 三, 四, 五, 六, 日)
 * In Elderly / Small-screen Mode: Pure single characters (一 ~ 日), larger and bolder, no redundant "周" prefix
 */
@Composable
fun WeekdayHeaderRow(
    isElderlyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val restColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = if (isElderlyMode) 10.dp else 6.dp
            ),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        WEEK_DAYS_MINIMAL.forEachIndexed { index, label ->
            val isWeekend = index >= 5
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = if (isElderlyMode) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = if (isElderlyMode) 18.sp else 13.sp,
                color = if (isWeekend) restColor else MaterialTheme.colorScheme.onSurface
            )
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
                Text(
                    text = "${year} 年 ${month} 月",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                if (month == 1) {
                    Text(
                        text = "${year} 年",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                }
                Text(
                    text = "${month} 月",
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

    val numColor = when {
        dayInfo.isRestDay -> restColor
        dayInfo.isLocaleHoliday -> LOCALE_FESTIVAL_COLOR
        else -> workColor
    }

    // Today & Selection cell styling
    val cellModifier = when {
        isSelected && dayInfo.isToday -> {
            // Selected AND Today: Strong distinct full highlight with high-contrast badge/border
            if (isElderlyMode) {
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
            } else {
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    )
            }
        }
        isSelected -> {
            // Selected Day: High-contrast solid primary background for elderly mode
            if (isElderlyMode) {
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
            } else {
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
        }
        dayInfo.isToday -> {
            // Today (not selected): A small red "今" dot/badge and a subtle dashed/thin border, NO solid fill!
            if (isElderlyMode) {
                Modifier
                    .background(
                        color = if (isLightMode) Color(0xFFF7F2EC) else Color(0xFF2C2723),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFB8251B).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
            } else {
                if (isLightMode) {
                    Modifier
                        .background(
                            color = Color(0xFFEFE7F6),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.2.dp,
                            color = Color(0xFFD0BCFF),
                            shape = RoundedCornerShape(10.dp)
                        )
                } else {
                    Modifier
                        .background(
                            color = Color(0xFF3D3349),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.2.dp,
                            color = Color(0xFF6E5B88),
                            shape = RoundedCornerShape(10.dp)
                        )
                }
            }
        }
        else -> Modifier
    }

    // In elderly mode, cells are squarish and focus purely on huge numbers
    val cellAspectRatio = if (isElderlyMode) 1.0f else 0.92f

    // Calculate text color (when selected in elderly mode with solid background, use high-contrast onPrimary)
    val displayNumColor = if (isElderlyMode && isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        numColor
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cellAspectRatio)
            .then(cellModifier)
            .clip(RoundedCornerShape(if (isElderlyMode) 12.dp else 10.dp))
            .clickable { onClick() }
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

                val subText = dayInfo.displayNote
                val subColor = if (dayInfo.isHolidayWithRest) restColor else MaterialTheme.colorScheme.onSurfaceVariant

                Text(
                    text = subText.ifBlank { " " },
                    color = subColor,
                    fontSize = 9.sp,
                    fontWeight = if (dayInfo.isHolidayWithRest) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
