package com.assistive.launcher

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

val IOSBlue = Color(0xFF007AFF)
val IOSGreen = Color(0xFF34C759)
val IOSRed = Color(0xFFFF3B30)

class AColors(
    val homeBg: Brush,
    val tile: Color,
    val tileBorder: Color,
    val label: Color,
    val plain: Color,
    val listBg: Color,
    val row: Color,
    val text: Color,
    val secondary: Color,
    val separator: Color,
    val dark: Boolean
)

val LightColors = AColors(
    homeBg = Brush.verticalGradient(listOf(Color(0xFFEBDFF5), Color(0xFFD8C6EA))),
    tile = Color(0x99FFFFFF),
    tileBorder = Color(0x66FFFFFF),
    label = Color.Black,
    plain = Color.White,
    listBg = Color(0xFFF2F2F7),
    row = Color.White,
    text = Color.Black,
    secondary = Color(0xFF8E8E93),
    separator = Color(0x1F000000),
    dark = false
)

val DarkColors = AColors(
    homeBg = SolidColor(Color.Black),
    tile = Color(0xFF1C1C1E),
    tileBorder = Color(0xFF2C2C2E),
    label = Color.White,
    plain = Color.Black,
    listBg = Color.Black,
    row = Color(0xFF1C1C1E),
    text = Color.White,
    secondary = Color(0xFF8E8E93),
    separator = Color(0x33FFFFFF),
    dark = true
)

val LocalA = staticCompositionLocalOf { LightColors }
