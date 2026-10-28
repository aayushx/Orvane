package com.orvane.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Round 2 / C0 design-system tokens.
 *
 * Spacing, corners and motion are structural (static across schemes).
 * Colors are DERIVED from the active [ColorScheme] (including dynamic color)
 * so every surface stays correct in light, dark, and Material You.
 * Consume via [orvaneTokens]; never hardcode values in screens.
 */
@Immutable
data class OrvaneSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

/** Corner radii: 12 cards · 16 dialogs · 28 sheets (hero elements go larger per use). */
@Immutable
data class OrvaneCorners(
    val card: Dp = 12.dp,
    val dialog: Dp = 16.dp,
    val sheet: Dp = 28.dp,
    val small: Dp = 8.dp,
)

/** Motion: 200–300ms emphasized easing (M3 curve), for expand/collapse and transitions. */
@Immutable
data class OrvaneMotion(
    val shortMs: Int = 200,
    val mediumMs: Int = 250,
    val longMs: Int = 300,
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)

@Immutable
data class OrvaneTokens(
    val spacing: OrvaneSpacing = OrvaneSpacing(),
    val corners: OrvaneCorners = OrvaneCorners(),
    val motion: OrvaneMotion = OrvaneMotion(),
    val priorityHigh: Color = PriorityHigh,
    val priorityMedium: Color = PriorityMedium,
    val priorityLow: Color = PriorityLow,
    val priorityNone: Color = PriorityNone,
    val quadrantDoFirst: Color,
    val quadrantSchedule: Color,
    val quadrantDelegate: Color,
    val quadrantLater: Color,
    val success: Color,
    val warning: Color,
    val cardBorder: Color,
)

/** Marker-free success/warning pairs, tuned for AA contrast per scheme stepped brightness. */
private val SuccessLight = Color(0xFF0F7A44)
private val SuccessDark = Color(0xFF6FD694)
private val WarningLight = Color(0xFFB25E00)
private val WarningDark = Color(0xFFFFB86D)

/**
 * Pure derivation so unit tests pin the mapping; the composable provider
 * (Theme.kt) passes the live [ColorScheme] — dynamic color flows through.
 */
fun orvaneTokens(
    scheme: ColorScheme,
    dark: Boolean,
    spacing: OrvaneSpacing = OrvaneSpacing(),
    corners: OrvaneCorners = OrvaneCorners(),
    motion: OrvaneMotion = OrvaneMotion(),
): OrvaneTokens = OrvaneTokens(
    spacing = spacing,
    corners = corners,
    motion = motion,
    quadrantDoFirst = scheme.errorContainer,
    quadrantSchedule = scheme.primaryContainer,
    quadrantDelegate = scheme.tertiaryContainer,
    quadrantLater = scheme.surfaceContainerHigh,
    success = if (dark) SuccessDark else SuccessLight,
    warning = if (dark) WarningDark else WarningLight,
    cardBorder = scheme.outlineVariant,
)

val LocalOrvaneTokens = staticCompositionLocalOf<OrvaneTokens> {
    error("OrvaneTheme must provide LocalOrvaneTokens")
}

/** Token accessor for screens/components; never construct directly. */
@Composable
fun orvaneTokens(): OrvaneTokens = LocalOrvaneTokens.current
