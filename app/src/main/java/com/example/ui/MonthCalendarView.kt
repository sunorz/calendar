package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 */
@Composable
fun WeekdayHeaderRow(modifier: Modifier = Modifier) {
    val restColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        WEEK_DAYS_MINIMAL.forEachIndexed { index, label ->
            val isWeekend = index >= 5
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isWeekend) restColor else MaterialTheme.colorScheme.onSurfaceVariant
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("month_section_${year}_$month")
    ) {
        // Month Title Header (Minimalist & Crisp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${year} 年",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "${month} 月",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Days Grid: Empty leading spaces + Month days
        val firstDayOfWeek = days.firstOrNull()?.dayOfWeekNumber ?: 1
        val leadingEmptyCount = firstDayOfWeek - 1
        val totalCells = leadingEmptyCount + days.size
        val rows = (totalCells + 6) / 7

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
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
 * - NO "休" or "班" text badges.
 * - Rest days: RED number (using colorScheme.error).
 * - Work days: Black in light mode, White in dark mode (using colorScheme.onSurface).
 * - Subtitle note:
 *     - Only RED when the day has a festival and it is a rest day (isHolidayWithRest).
 *     - GREEN for locale festivals (isLocaleHoliday).
 *     - Regular subtle color for ordinary rest days and non-holiday workdays.
 * - Today: Marked with a clean circular indicator.
 */
@Composable
fun MinimalDayCell(
    dayInfo: DayInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val restColor = MaterialTheme.colorScheme.error
    val workColor = MaterialTheme.colorScheme.onSurface

    // Determine day number text color: Red for rest day, Green for locale holiday (if not rest day), onSurface for work day
    val numColor = when {
        dayInfo.isToday -> Color.White
        dayInfo.isRestDay -> restColor
        dayInfo.isLocaleHoliday -> LOCALE_FESTIVAL_COLOR
        else -> workColor
    }

    // Determine cell container background for Today & Selected state
    val todayBgColor = if (dayInfo.isRestDay) restColor else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .then(
                if (isSelected && !dayInfo.isToday) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    )
                } else Modifier
            )
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Day Number Container (Special circle badge if Today)
            if (dayInfo.isToday) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(todayBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayInfo.day.toString(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = dayInfo.day.toString(),
                    color = numColor,
                    fontSize = 16.sp,
                    fontWeight = if (dayInfo.isRestDay) FontWeight.Bold else FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle text:
            // 1. Locale festival (e.g. 三月三) -> Green
            // 2. Festival on a rest day -> Red
            // 3. Others (normal rest day lunar text, work day lunar text) -> onSurfaceVariant
            val subText = dayInfo.displayNote
            val subColor = when {
                dayInfo.isHolidayWithRest -> restColor
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Text(
                text = subText.ifBlank { " " },
                color = subColor,
                fontSize = 9.5.sp,
                fontWeight = if (dayInfo.isHolidayWithRest) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
