package com.sunplace.calendar.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunplace.calendar.R
import com.sunplace.calendar.data.model.ThemeMode
import com.sunplace.calendar.data.model.TodayPattern
import com.sunplace.calendar.util.TranslationHelper

/**
 * Full Screen Secondary Menu / Settings Screen (全屏二级设置菜单)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    isElderlyMode: Boolean = false,
    showGuangxiSanyuesan: Boolean = true,
    currentTodayPattern: TodayPattern = TodayPattern.DEFAULT,
    appLanguage: String = "zh",
    currentVersion: String,
    updateAvailable: Boolean,
    remoteVersion: String?,
    isDownloading: Boolean,
    downloadProgress: Float,
    initialColorModeIndex: Int = 0,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleShowGuangxiSanyuesan: (Boolean) -> Unit,
    onToggleAppLanguage: (Boolean) -> Unit = {},
    onTodayPatternChange: (TodayPattern) -> Unit = {},
    onDownloadUpdate: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEnglish = appLanguage.equals("en", ignoreCase = true)
    var showPatternPicker by remember { mutableStateOf(false) }

    val todayBrush = remember(initialColorModeIndex) {
        getTodayCardBrush(initialColorModeIndex)
    }

    val todayTreeRingColor = remember(initialColorModeIndex) {
        getTodayPatternTintColor(initialColorModeIndex)
    }

    BackHandler(enabled = true) {
        onNavigateBack()
    }

    if (showPatternPicker) {
        TodayPatternPickerDialog(
            selectedPattern = currentTodayPattern,
            initialColorModeIndex = initialColorModeIndex,
            onSelectPattern = { newPattern ->
                onTodayPatternChange(newPattern)
                showPatternPicker = false
            },
            onDismiss = { showPatternPicker = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_dialog_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val sectionCardBorder = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
            val sectionCardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // 1. Appearance & Theme / 主题颜色
                if (isElderlyMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.elderly_mode_locked),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.elderly_mode_theme_tip),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = sectionCardColor,
                        border = sectionCardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeOptionChip(
                                label = stringResource(R.string.theme_system),
                                icon = Icons.Default.BrightnessAuto,
                                isSelected = currentThemeMode == ThemeMode.SYSTEM,
                                onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionChip(
                                label = stringResource(R.string.theme_light),
                                icon = Icons.Default.LightMode,
                                isSelected = currentThemeMode == ThemeMode.LIGHT,
                                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionChip(
                                label = stringResource(R.string.theme_dark),
                                icon = Icons.Default.DarkMode,
                                isSelected = currentThemeMode == ThemeMode.DARK,
                                onClick = { onThemeModeChange(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Today Card Pattern / 今日纹样自定义 - Hidden in Elderly Mode
                if (!isElderlyMode) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showPatternPicker = true }
                            .testTag("pattern_select_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = sectionCardColor,
                        border = sectionCardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(brush = todayBrush, shape = RoundedCornerShape(12.dp))
                                        .border(
                                            width = 0.8.dp,
                                            brush = TodayBevelBorderBrush,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = currentTodayPattern.drawableRes),
                                        contentDescription = currentTodayPattern.getDisplayName(isEnglish),
                                        colorFilter = ColorFilter.tint(todayTreeRingColor),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.pattern_select_title),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentTodayPattern.getDisplayName(isEnglish),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Holiday Preference / 广西节假日
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onToggleShowGuangxiSanyuesan(!showGuangxiSanyuesan) }
                        .testTag("sanyuesan_switch_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = sectionCardColor,
                    border = sectionCardBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (showGuangxiSanyuesan) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = null,
                                        tint = if (showGuangxiSanyuesan) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.sanyuesan_switch_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = showGuangxiSanyuesan,
                            onCheckedChange = { onToggleShowGuangxiSanyuesan(it) },
                            modifier = Modifier.testTag("sanyuesan_switch")
                        )
                    }
                }

                // 3. Language Preference / 中英文双向切换
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("language_switch_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = sectionCardColor,
                    border = sectionCardBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.language_section_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 双向切换开关 (Two-way Switch / Segmented Control)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (!isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable { if (isEnglish) onToggleAppLanguage(false) }
                                        .testTag("lang_btn_zh")
                                ) {
                                    Text(
                                        text = "简体中文",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (!isEnglish) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable { if (!isEnglish) onToggleAppLanguage(true) }
                                        .testTag("lang_btn_en")
                                ) {
                                    Text(
                                        text = "en-US",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isEnglish) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: About / 关于应用
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("about_section")
                ) {
                    Text(
                        text = stringResource(R.string.about_section_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = sectionCardColor,
                        border = sectionCardBorder
                    ) {
                        val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AboutInfoRow(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                label = stringResource(R.string.announcement_label)
                            ) {
                                Text(
                                    text = stringResource(R.string.announcement_content),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = dividerColor)

                            AboutInfoRow(
                                icon = Icons.Default.Person,
                                label = stringResource(R.string.author_label)
                            ) {
                                Text(
                                    text = String(Base64.decode("U3VucGxhY2U=", Base64.DEFAULT)),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = dividerColor)

                            AboutInfoRow(
                                icon = Icons.Default.Language,
                                label = stringResource(R.string.website_label),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        try {
                                            val url = String(Base64.decode("aHR0cHM6Ly9ra2lpLm9yZw==", Base64.DEFAULT))
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (_: Exception) {}
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = String(Base64.decode("a2tpaS5vcmc=", Base64.DEFAULT)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = dividerColor)

                            val canUpdate = updateAvailable && remoteVersion != null
                            AboutInfoRow(
                                icon = Icons.Default.Verified,
                                label = stringResource(R.string.version_label),
                                modifier = Modifier
                                    .let {
                                        if (canUpdate && !isDownloading) it.clickable { onDownloadUpdate() } else it
                                    }
                                    .padding(vertical = if (canUpdate) 4.dp else 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val baseVersionText = currentVersion
                                        .substringBefore("_")
                                        .substringBefore("(")
                                        .trim()
                                    val gitHashText = when {
                                        currentVersion.contains("_") -> currentVersion.substringAfter("_").trim()
                                        currentVersion.contains("(") -> currentVersion.substringAfter("(").substringBefore(")").trim()
                                        else -> ""
                                    }

                                    Text(
                                        text = "v$baseVersionText",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (gitHashText.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(
                                                0.8.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                            )
                                        ) {
                                            Text(
                                                text = gitHashText,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (canUpdate) {
                                        if (isDownloading) {
                                            DownloadPieProgress(
                                                actualProgress = downloadProgress,
                                                color = Color(0xFF22C55E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.SystemUpdate,
                                                contentDescription = "升级",
                                                tint = Color(0xFF22C55E),
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clickable { onDownloadUpdate() }
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingContent()
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.45f
    val clickModifier = if (enabled) Modifier.clickable { onClick() } else Modifier

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(clickModifier)
            .then(
                if (isSelected) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.5f),
                        RoundedCornerShape(14.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 0.75f else 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = (if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = (if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha)
            )
        }
    }
}

/**
 * Custom Download Pie Progress indicator.
 */
@Composable
private fun DownloadPieProgress(
    actualProgress: Float = 0f,
    color: Color = Color(0xFF22C55E),
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val animatedProgress = remember { Animatable(0f) }

    val standardCubicBezier = remember {
        CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    }

    LaunchedEffect(actualProgress) {
        val target = actualProgress.coerceIn(0f, 1f)
        val duration = if (target >= 1f) 200 else 250
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = duration,
                easing = standardCubicBezier
            )
        )
    }

    Canvas(modifier = modifier) {
        drawCircle(color = trackColor)
        val sweepAngle = 360f * animatedProgress.value.coerceIn(0f, 1f)
        if (sweepAngle > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = true
            )
        }
    }
}
