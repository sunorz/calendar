package com.sunplace.calendar.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    currentVersion: String,
    updateAvailable: Boolean,
    remoteVersion: String?,
    isDownloading: Boolean,
    downloadProgress: Float,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleShowGuangxiSanyuesan: (Boolean) -> Unit,
    onTodayPatternChange: (TodayPattern) -> Unit = {},
    onDownloadUpdate: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEnglish = TranslationHelper.isEnglish()
    var showPatternPicker by remember { mutableStateOf(false) }

    // Handle Android system back press to return to calendar
    BackHandler(enabled = true) {
        onNavigateBack()
    }

    if (showPatternPicker) {
        TodayPatternPickerDialog(
            selectedPattern = currentTodayPattern,
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
            Column(
                        
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Section 1: Holiday & Feature Preferences / 节假日设置
                Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onToggleShowGuangxiSanyuesan(!showGuangxiSanyuesan) }
                            .testTag("sanyuesan_switch_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
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
                }

                // Section 2: Appearance & Theme / 外观主题
                Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isElderlyMode) {
                        // Senior care mode locked notice
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
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
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
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
                }

                // Section 3: Today Card Pattern / 今日纹样自定义 (40款传统祥纹) - Hidden in Elderly Mode
                if (!isElderlyMode) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { showPatternPicker = true }
                                .testTag("pattern_select_card"),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
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
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                painter = painterResource(id = currentTodayPattern.drawableRes),
                                                contentDescription = currentTodayPattern.getDisplayName(isEnglish),
                                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
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
                                    border = androidx.compose.foundation.BorderStroke(
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
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                        
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Slogan / Quote
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.announcement_label),
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.announcement_content),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Author
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.author_label),
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = String(android.util.Base64.decode("U3VucGxhY2U=", android.util.Base64.DEFAULT)),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Website
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        try {
                                            val url = String(android.util.Base64.decode("aHR0cHM6Ly9ra2lpLm9yZw==", android.util.Base64.DEFAULT))
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Handle exception
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.website_label),
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = String(android.util.Base64.decode("a2tpaS5vcmc=", android.util.Base64.DEFAULT)),
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

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Version & In-App Auto Update
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .let {
                                        if (updateAvailable && remoteVersion != null && !isDownloading) {
                                            it.clickable { onDownloadUpdate() }
                                        } else {
                                            it
                                        }
                                    }
                                    .padding(vertical = if (updateAvailable && remoteVersion != null) 4.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.version_label),
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "v$currentVersion",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (updateAvailable && remoteVersion != null) {
                                        if (isDownloading) {
                                            LinearProgressIndicator(
                                                progress = { downloadProgress },
                                                modifier = Modifier.width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                                            )
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "v$remoteVersion 升级",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
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
    }
}

/**
 * Backward compatibility alias forwarding to SettingsScreen
 */
@Composable
fun SettingsDialog(
    currentThemeMode: ThemeMode,
    isElderlyMode: Boolean = false,
    showGuangxiSanyuesan: Boolean = true,
    currentTodayPattern: TodayPattern = TodayPattern.DEFAULT,
    currentVersion: String,
    updateAvailable: Boolean,
    remoteVersion: String?,
    isDownloading: Boolean,
    downloadProgress: Float,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleShowGuangxiSanyuesan: (Boolean) -> Unit,
    onTodayPatternChange: (TodayPattern) -> Unit = {},
    onDownloadUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    SettingsScreen(
        currentThemeMode = currentThemeMode,
        isElderlyMode = isElderlyMode,
        showGuangxiSanyuesan = showGuangxiSanyuesan,
        currentTodayPattern = currentTodayPattern,
        currentVersion = currentVersion,
        updateAvailable = updateAvailable,
        remoteVersion = remoteVersion,
        isDownloading = isDownloading,
        downloadProgress = downloadProgress,
        onThemeModeChange = onThemeModeChange,
        onToggleShowGuangxiSanyuesan = onToggleShowGuangxiSanyuesan,
        onTodayPatternChange = onTodayPatternChange,
        onDownloadUpdate = onDownloadUpdate,
        onNavigateBack = onDismiss
    )
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.45f
    val clickModifier = if (enabled) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

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
