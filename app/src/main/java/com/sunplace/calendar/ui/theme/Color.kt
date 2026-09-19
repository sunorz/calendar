package com.sunplace.calendar.ui.theme

import androidx.compose.ui.graphics.Color

// Ultra-soft, warm, eye-friendly editorial palette
val LightPrimary = Color(0xFFB85C53)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFF9EBEA)
val LightOnPrimaryContainer = Color(0xFF7A2E27)

val DarkPrimary = Color(0xFFD48B84)
val DarkOnPrimary = Color(0xFF221615)
val DarkPrimaryContainer = Color(0xFF422623)
val DarkOnPrimaryContainer = Color(0xFFF4D4D1)

// Rest day / highlight red (warm brick red, non-glaring)
val LightError = Color(0xFFBA4A43)
val LightOnError = Color(0xFFFFFFFF)
val DarkError = Color(0xFFD97771)
val DarkOnError = Color(0xFF221615)

// Surfaces & Backgrounds: Warm paper/parchment light, deep espresso dark
val LightBackground = Color(0xFFF7F6F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8E5DF)
val LightOnBackground = Color(0xFF383635)
val LightOnSurface = Color(0xFF383635)
val LightOnSurfaceVariant = Color(0xFF85817E)
val LightOutline = Color(0xFFE4E1DA)
val LightOutlineVariant = Color(0xFFE8E5DF)

val DarkBackground = Color(0xFF191817)
val DarkSurface = Color(0xFF222120)
val DarkSurfaceVariant = Color(0xFF2D2C2A)
val DarkOnBackground = Color(0xFFE6E3DE)
val DarkOnSurface = Color(0xFFE6E3DE)
val DarkOnSurfaceVariant = Color(0xFF9E9A94)
val DarkOutline = Color(0xFF3A3835)
val DarkOutlineVariant = Color(0xFF2D2C2A)

// Standard Calendar Today Highlight Colors by Day Category
// 浅色模式：高亮偏暗，搭配白色文字 #FFFFFF
val TodayGreenLight = Color(0xFF1B5E20)  // 三月三假期：深暗绿
val TodayYellowLight = Color(0xFFA17400) // 工作日：深暗柠檬金黄，确保白字清晰
val TodayRedLight = Color(0xFFB71C1C)    // 休息日：深暗红

// 深色模式：高亮偏亮，搭配黑色文字 #000000
val TodayGreenDark = Color(0xFF4ADE80)   // 三月三假期：明亮绿
val TodayYellowDark = Color(0xFFFDE047)  // 工作日：明亮鲜艳柠檬黄，黑字清晰
val TodayRedDark = Color(0xFFFF5959)     // 休息日：明亮红

// Today Film Texture Tint
val TodayFilmLight = Color(0x35FFFFFF)  // 浅色模式暗底：白色微透薄膜
val TodayFilmDark = Color(0x30000000)   // 深色模式亮底：黑色微透薄膜

// Backward compatible aliases
val TodayBlueLight = TodayYellowLight
val TodayBlueDark = TodayYellowDark
val TodayBlackLight = TodayYellowLight
val TodayBlackDark = TodayYellowDark
val TodayHighlightLight = TodayYellowLight
val TodayHighlightDark = TodayYellowDark
val TodayTreeRingLight = TodayFilmLight
val TodayTreeRingDark = TodayFilmDark

// ==========================================
// Elderly Care Mode Dual-Color System
// Culturally respectful, eye-safe, high-contrast, avoiding harsh black/white blocks
// ==========================================

// Elderly Light: Warm Xuan paper + Warm dark ink chestnut + Auspicious cinnabar red
val ElderlyLightBackground = Color(0xFFF7F3EB)
val ElderlyLightSurface = Color(0xFFFDFBF7)
val ElderlyLightSurfaceVariant = Color(0xFFEDE4D6)
val ElderlyLightOnBackground = Color(0xFF2B211D) // Warm deep ink chestnut, highly readable
val ElderlyLightOnSurface = Color(0xFF2B211D)
val ElderlyLightOnSurfaceVariant = Color(0xFF5A4940)
val ElderlyLightPrimary = Color(0xFFB8251B) // Cinnabar Red
val ElderlyLightOnPrimary = Color(0xFFFDFBF7)
val ElderlyLightPrimaryContainer = Color(0xFFFCE5E2)
val ElderlyLightOnPrimaryContainer = Color(0xFF661009)
val ElderlyLightError = Color(0xFFC4281E)
val ElderlyLightOnError = Color(0xFFFDFBF7)
val ElderlyLightOutline = Color(0xFFD4C5B3)
val ElderlyLightOutlineVariant = Color(0xFFE4D7C7)

// Elderly Dark: Warm sandalwood dusk + Moon silk ivory + Warm coral cinnabar
val ElderlyDarkBackground = Color(0xFF1E1A17)
val ElderlyDarkSurface = Color(0xFF29231F)
val ElderlyDarkSurfaceVariant = Color(0xFF38302A)
val ElderlyDarkOnBackground = Color(0xFFFAF3E8) // Moon silk ivory, clear without glare
val ElderlyDarkOnSurface = Color(0xFFFAF3E8)
val ElderlyDarkOnSurfaceVariant = Color(0xFFD1C5B6)
val ElderlyDarkPrimary = Color(0xFFEE6359) // Warm coral cinnabar
val ElderlyDarkOnPrimary = Color(0xFF2B1210)
val ElderlyDarkPrimaryContainer = Color(0xFF5C2420)
val ElderlyDarkOnPrimaryContainer = Color(0xFFFFDAD6)
val ElderlyDarkError = Color(0xFFF06A60)
val ElderlyDarkOnError = Color(0xFF2B1210)
val ElderlyDarkOutline = Color(0xFF5C5046)
val ElderlyDarkOutlineVariant = Color(0xFF423830)

