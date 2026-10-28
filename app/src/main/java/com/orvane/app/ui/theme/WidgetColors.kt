package com.orvane.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Static widget palette (Round 2 / B4). Glance remote views cannot consume
 * MaterialTheme or dynamic color, so the home-screen widgets render on a
 * fixed dark surface; these constants are the SINGLE definition point —
 * widget code must consume these tokens and must not inline hex colors.
 *
 * Values are carried over verbatim from the original widget layouts.
 */
val WidgetSurface = Color(0xF2121319)
val WidgetOnSurface = Color.White
val WidgetRowBg = Color(0xFF333845)
val WidgetAccent = Color(0xFFB8C1FF)
val WidgetTextSecondary = Color(0xFFAAAAB4)
val WidgetMutedFg = Color(0xFF8A8A92)
val WidgetGrayFg = Color(0xFF9A9AA4)
val WidgetSuccess = Color(0xFF22B573)
val WidgetSkipped = Color(0xFF546A7B)
