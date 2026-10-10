package com.sunplace.calendar.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.DayInfo
import com.sunplace.calendar.util.TranslationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val isElderlyMode by viewModel.isElderlyMode.collectAsState()
    val showGuangxiSanyuesan by viewModel.showGuangxiSanyuesan.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val updateAvailable by viewModel.updateAvailable.collectAsState()
    val remoteVersion by viewModel.remoteVersion.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val todayPattern by viewModel.todayPattern.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()

    BackHandler(enabled = showSettingsDialog) {
        viewModel.closeSettingsDialog()
    }

    BackHandler(enabled = !showSettingsDialog && selectedDay != null) {
        viewModel.selectDay(null)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth

        when {
            screenWidth < 360.dp -> {
                WatchDayWidgetView(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            screenWidth >= 600.dp -> {
                LargeScreenCalendarView(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                StandardPhoneCalendarView(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showSettingsDialog,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        SettingsScreen(
            currentThemeMode = themeMode,
            isElderlyMode = isElderlyMode,
            showGuangxiSanyuesan = showGuangxiSanyuesan,
            currentTodayPattern = todayPattern,
            currentVersion = viewModel.getAppVersion(),
            updateAvailable = updateAvailable,
            remoteVersion = remoteVersion,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            initialColorModeIndex = viewModel.getTodayColorModeIndex(),
            onThemeModeChange = { viewModel.setThemeMode(it) },
            onToggleShowGuangxiSanyuesan = { viewModel.setShowGuangxiSanyuesan(it) },

            onTodayPatternChange = { viewModel.setTodayPattern(it) },
            onDownloadUpdate = { viewModel.downloadAndUpdate() },
            onNavigateBack = { viewModel.closeSettingsDialog() }
        )
    }
}

/**
 * Standard Phone Calendar View for vertical handheld screens (360dp ~ 599dp)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardPhoneCalendarView(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val nextYearConfig by viewModel.nextYearConfig.collectAsState()
    val isElderlyMode by viewModel.isElderlyMode.collectAsState()
    val showGuangxiSanyuesan by viewModel.showGuangxiSanyuesan.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val todayPattern by viewModel.todayPattern.collectAsState()
    val currentTodayDate by viewModel.todayDate.collectAsState()
    val resetScrollEvent by viewModel.resetScrollEvent.collectAsState()
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.todayMonthIndex
    )

    LaunchedEffect(resetScrollEvent) {
        if (resetScrollEvent > 0L) {
            listState.scrollToItem(viewModel.todayMonthIndex)
        }
    }

    val currentVisibleYearMonth by remember {
        derivedStateOf {
            CalendarViewModel.getYearMonthFromIndex(listState.firstVisibleItemIndex)
        }
    }

    val todayDayInfo = remember(currentTodayDate, config, nextYearConfig, showGuangxiSanyuesan) {
        viewModel.getTodayDayInfo()
    }
    val todayTextColor = when {
        todayDayInfo.isExplicitLocaleHoliday -> LOCALE_FESTIVAL_COLOR
        todayDayInfo.isRestDay -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = {
                        val (visYear, visMonth) = currentVisibleYearMonth
                        val topBarTitle = if (TranslationHelper.isEnglish()) {
                            if (isElderlyMode) {
                                TranslationHelper.formatShortYearMonth(visYear, visMonth)
                            } else {
                                TranslationHelper.formatYearMonth(visYear, visMonth)
                            }
                        } else {
                            stringResource(R.string.year_month_format, visYear, visMonth)
                        }
                        Text(
                            text = topBarTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isElderlyMode) 19.sp else 18.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        Surface(
                            modifier = Modifier
                                .padding(start = if (isElderlyMode) 6.dp else 12.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    viewModel.checkAndRefreshDate()
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(viewModel.todayMonthIndex)
                                        viewModel.selectToday()
                                    }
                                }
                                .testTag("jump_today_button"),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            if (isElderlyMode) {
                                Text(
                                    text = stringResource(R.string.elderly_btn_today),
                                    color = todayTextColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = todayPattern.drawableRes),
                                        contentDescription = stringResource(R.string.today_btn),
                                        colorFilter = ColorFilter.tint(todayTextColor),
                                        modifier = Modifier.size(15.dp)
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
                    },
                    actions = {
                        if (isElderlyMode) {
                            Surface(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { viewModel.toggleElderlyMode() }
                                    .testTag("elderly_mode_toggle_button"),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.elderly_btn_mode_exit),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { viewModel.openSettingsDialog() }
                                    .testTag("settings_button"),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.elderly_btn_theme),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.toggleElderlyMode() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("elderly_mode_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = stringResource(R.string.elderly_mode_enter),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.openSettingsDialog() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings_title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                WeekdayHeaderRow(
                    isElderlyMode = isElderlyMode,
                    horizontalPadding = if (isElderlyMode) 10.dp else 12.dp,
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val elderlyCardMaxHeight = maxHeight * 2f / 3f

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
                    val days = remember(year, month, currentTodayDate, config, nextYearConfig, showGuangxiSanyuesan) {
                        viewModel.getDaysForMonth(year, month)
                    }

                    MinimalMonthSection(
                        year = year,
                        month = month,
                        days = days,
                        selectedDay = selectedDay,
                        isElderlyMode = isElderlyMode,
                        todayPattern = todayPattern,
                        onDayClick = { day ->
                            val nextSelected = if (selectedDay == day) null else day
                            viewModel.selectDay(nextSelected)
                            if (nextSelected != null && isElderlyMode) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedDay != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                selectedDay?.let { day ->
                    SelectedDayDetailPill(
                        dayInfo = day,
                        isElderlyMode = isElderlyMode,
                        maxCardHeight = if (isElderlyMode) elderlyCardMaxHeight else null,
                        onDismiss = { viewModel.selectDay(null) },
                        modifier = Modifier.padding(
                            horizontal = if (isElderlyMode) 8.dp else 16.dp,
                            vertical = if (isElderlyMode) 6.dp else 16.dp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Minimal floating pill card when a user clicks a specific day in the calendar
 */
@Composable
private fun SelectedDayDetailPill(
    dayInfo: DayInfo,
    onDismiss: () -> Unit,
    isElderlyMode: Boolean = false,
    maxCardHeight: Dp? = null,
    modifier: Modifier = Modifier
) {
    val dateTitle = if (dayInfo.isToday) {
        stringResource(R.string.date_format_full_today, dayInfo.year, dayInfo.month, dayInfo.day)
    } else {
        stringResource(R.string.date_format_full, dayInfo.year, dayInfo.month, dayInfo.day)
    }
    val lunarPriority = TranslationHelper.getCardLunarPriorityData(dayInfo)

    if (isElderlyMode) {
        val surfaceModifier = if (maxCardHeight != null) {
            modifier.fillMaxWidth().heightIn(max = maxCardHeight)
        } else {
            modifier.fillMaxWidth()
        }

        val neutralBadgeBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        val neutralBadgeText = MaterialTheme.colorScheme.onSurface
        val pillShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

        Surface(
            modifier = surfaceModifier
                .clip(pillShape)
                .testTag("selected_day_detail_pill"),
            shape = pillShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 12.dp,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dateTitle,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            lineHeight = 36.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(getFullWeekdayResId(dayInfo.dayOfWeekNumber)),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onDismiss() }
                            .testTag("close_detail_card_button"),
                        color = neutralBadgeBg,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.close_btn),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    thickness = 1.5.dp
                )
                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = neutralBadgeBg
                ) {
                    Text(
                        text = if (dayInfo.isRestDay) stringResource(R.string.restday_label) else stringResource(R.string.workday_label),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = neutralBadgeText
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (lunarPriority.hasFestival) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = neutralBadgeBg
                        ) {
                            Text(
                                text = lunarPriority.festivalText!!,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                fontSize = 22.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = neutralBadgeText
                            )
                        }
                    }

                    if (lunarPriority.hasSolarTerm) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.solar_term_label),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lunarPriority.solarTermText!!,
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val fullLunar = TranslationHelper.getLocalizedFullLunarText(dayInfo)
                    if (fullLunar.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lunar_label),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = fullLunar,
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .testTag("selected_day_detail_pill"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                            text = dateTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(getFullWeekdayResId(dayInfo.dayOfWeekNumber)),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val isRest = dayInfo.isRestDay
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isRest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = if (isRest) stringResource(R.string.restday_label) else stringResource(R.string.workday_label),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRest) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (lunarPriority.combinedText.isNotBlank()) {
                        val separator = if (TranslationHelper.isEnglish()) " · " else "·"
                        val normalColor = MaterialTheme.colorScheme.onSurfaceVariant
                        val festColor = if (lunarPriority.isHolidayWithRest) MaterialTheme.colorScheme.error else normalColor

                        val annotatedText = buildAnnotatedString {
                            val lunarStr = lunarPriority.ordinaryLunarText
                            if (lunarStr.isNotBlank()) {
                                withStyle(SpanStyle(color = normalColor, fontWeight = FontWeight.Normal)) {
                                    append(lunarStr)
                                }
                            }

                            if (lunarPriority.hasSolarTerm) {
                                if (this.length > 0) {
                                    withStyle(SpanStyle(color = normalColor, fontWeight = FontWeight.Normal)) {
                                        append(separator)
                                    }
                                }
                                withStyle(SpanStyle(color = normalColor, fontWeight = FontWeight.SemiBold)) {
                                    append(lunarPriority.solarTermText!!)
                                }
                            }

                            if (lunarPriority.hasFestival) {
                                val festParts = lunarPriority.festivalText!!.split(" · ", "·").filter { it.isNotBlank() }
                                festParts.forEach { f ->
                                    if (this.length > 0) {
                                        withStyle(SpanStyle(color = normalColor, fontWeight = FontWeight.Normal)) {
                                            append(separator)
                                        }
                                    }
                                    withStyle(SpanStyle(color = festColor, fontWeight = FontWeight.SemiBold)) {
                                        append(f)
                                    }
                                }
                            }
                        }

                        Text(
                            text = annotatedText,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_btn),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
