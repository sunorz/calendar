package com.sunplace.calendar.ui

import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunplace.calendar.data.model.TodayPattern
import com.sunplace.calendar.util.TranslationHelper
import com.sunplace.calendar.R
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayPatternPickerDialog(
    selectedPattern: TodayPattern,
    onSelectPattern: (TodayPattern) -> Unit,
    onDismiss: () -> Unit
) {
    val isEnglish = TranslationHelper.isEnglish()
    val allPatterns = remember { TodayPattern.entries.toList() }
    val uriHandler = LocalUriHandler.current
    
    val now = remember { Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")) }
    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
    val dayOfMonthString = dayOfMonth.toString()
    
    var colorModeIndex by remember { mutableIntStateOf(0) }
    
    var currentPatternIndex by remember { 
        mutableIntStateOf(allPatterns.indexOf(selectedPattern).takeIf { it >= 0 } ?: 0) 
    }
    
    val currentPreviewPattern = allPatterns[currentPatternIndex]

    val todayBrush = remember(colorModeIndex) {
        when (colorModeIndex) {
            0 -> Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFEAB308)))
            1 -> Brush.verticalGradient(listOf(Color(0xFFFF8585), Color(0xFFE53935)))
            2 -> Brush.verticalGradient(listOf(Color(0xFF6EE7B7), Color(0xFF22C55E)))
            else -> Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFEAB308)))
        }
    }
    
    val todayBevelBorder = remember {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.50f), Color.Black.copy(alpha = 0.15f)))
    }
    val AbrilFatfaceFontFamily = FontFamily(
        androidx.compose.ui.text.font.Font(R.font.abril_fatface, FontWeight.Normal)
    )
    val todayTreeRingColor = remember(colorModeIndex) {
        if (colorModeIndex == 0) {
            Color.White.copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.55f)
        }
    }
    val todayTextColor = Color(0xFF000000)

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    fun nextPattern() {
        currentPatternIndex = (currentPatternIndex + 1) % allPatterns.size
    }

    fun prevPattern() {
        currentPatternIndex = if (currentPatternIndex - 1 < 0) allPatterns.size - 1 else currentPatternIndex - 1
    }

    fun nextColor() {
        colorModeIndex = (colorModeIndex + 1) % 3
    }

    fun prevColor() {
        colorModeIndex = if (colorModeIndex - 1 < 0) 2 else colorModeIndex - 1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> { prevColor(); true }
                        KeyEvent.KEYCODE_DPAD_DOWN -> { nextColor(); true }
                        KeyEvent.KEYCODE_DPAD_LEFT -> { prevPattern(); true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { nextPattern(); true }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { 
                            onSelectPattern(currentPreviewPattern)
                            onDismiss()
                            true 
                        }
                        else -> false
                    }
                } else false
            },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (abs(offsetX) > abs(offsetY)) {
                                    if (offsetX > 40) prevPattern()
                                    else if (offsetX < -40) nextPattern()
                                } else {
                                    if (offsetY > 40) nextColor()
                                    else if (offsetY < -40) prevColor()
                                }
                                offsetX = 0f
                                offsetY = 0f
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fixed 200.dp x 200.dp Card
                BoxWithConstraints(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(brush = todayBrush, shape = RoundedCornerShape(16.dp))
                        .border(width = 0.8.dp, brush = todayBevelBorder, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val cardSize = 200.dp
                    val patternSize = cardSize * 1.12f
                    val centerXDp = cardSize * TodayPatternHolder.normCenterX
                    val centerYDp = cardSize * TodayPatternHolder.normCenterY

                    Image(
                        painter = painterResource(id = currentPreviewPattern.drawableRes),
                        contentDescription = null,
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

                    // hole
                    val holeOffset = cardSize.value * 0.08f
                    val holeSize = cardSize.value * 0.12f
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = holeOffset.dp, top = holeOffset.dp)
                            .size(holeSize.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 0.8.dp,
                                brush = todayBevelBorder,
                                shape = CircleShape
                            )
                    )

                    // Standard Mode - Today: No lunar/festival subtitle, Abril Fatface handwritten day number
                    Text(
                        text = dayOfMonthString,
                        color = todayTextColor,
                        fontSize = (cardSize.value * 0.48f).sp,
                        fontFamily = AbrilFatfaceFontFamily,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        style = LocalTextStyle.current.copy(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Pattern Name with fixed minimum height to prevent layout jumps on wrap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentPreviewPattern.getDisplayName(isEnglish),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CREDIT: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "黑洛视觉",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.iconfont.cn/collections/detail?cid=54419")
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isEnglish) "Confirm" else "确定",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSelectPattern(currentPreviewPattern)
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
    }
}
