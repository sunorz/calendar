package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThemeMode
import com.example.data.repository.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDialog(
    currentThemeMode: ThemeMode,
    syncState: SyncState,
    currentVersion: String,
    updateAvailable: Boolean,
    remoteVersion: String?,
    isDownloading: Boolean,
    downloadProgress: Float,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRefreshConfig: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "应用设置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Appearance & Theme
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "外观主题",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeOptionChip(
                            label = "跟随系统",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = currentThemeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "浅色",
                            icon = Icons.Default.LightMode,
                            isSelected = currentThemeMode == ThemeMode.LIGHT,
                            onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "深色",
                            icon = Icons.Default.DarkMode,
                            isSelected = currentThemeMode == ThemeMode.DARK,
                            onClick = { onThemeModeChange(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 2: Online Configuration & Sync
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "调休配置文件同步",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "每年节假日及调休安排将依据官方配置自动同步",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Sync Status message
                    when (syncState) {
                        is SyncState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "正在同步配置...",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is SyncState.Success -> {
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(syncState.lastSyncTime))
                            Text(
                                text = "✓ ${syncState.message} ($timeStr)",
                                fontSize = 11.5.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is SyncState.Error -> {
                            Text(
                                text = "✗ ${syncState.error}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }

                    Button(
                        onClick = { onRefreshConfig() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即同步配置", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 3: About / 关于
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("about_section")
                ) {
                    Text(
                        text = "关于应用",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Slogan / Quote
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "感悟",
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "东墙变成西墙，该开心吗？",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Author
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "作者",
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Sunplace",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Website
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kkii.org"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Handle exception
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "官方网址",
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "kkii.org",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            // Version
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "当前版本",
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = currentVersion,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (updateAvailable) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "检测到新版本 v${remoteVersion ?: ""}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (isDownloading) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { downloadProgress },
                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = onDownloadUpdate,
                                        enabled = !isDownloading,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (isDownloading) "下载中..." else "立即下载", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("完成", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
