package com.orvane.app.ui.theme

import androidx.compose.ui.graphics.Color

// F2 (v1.3.0): fallback custom palette redesigned toward muted, sophisticated
// tones — near-neutral surfaces (Things 3 style) with ONE restrained indigo
// accent (Linear style). Low-chroma everywhere; no saturated grass-green, no
// default blue. Material You dynamic color is untouched and overrides these
// on Android 12+. Quadrant/habit/priority colors derive from or harmonize
// with this scheme.

// ---- Light (cool near-whites, single indigo accent) ----
val PrimaryLight = Color(0xFF5058C0)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE0E1FA)
val OnPrimaryContainerLight = Color(0xFF181C66)
val SecondaryLight = Color(0xFF565E72)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFDBDDE9)
val OnSecondaryContainerLight = Color(0xFF191C28)
val TertiaryLight = Color(0xFF715563)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFF3D9E0)
val OnTertiaryContainerLight = Color(0xFF2E1119)
val ErrorLight = Color(0xFFAA3A38)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF6DBD9)
val OnErrorContainerLight = Color(0xFF400F0D)
val BackgroundLight = Color(0xFFF5F5F7)
val OnBackgroundLight = Color(0xFF1B1B1F)
val SurfaceLight = Color(0xFFFAFAFB)
val OnSurfaceLight = Color(0xFF1B1B1F)
val SurfaceVariantLight = Color(0xFFE2E2E7)
val OnSurfaceVariantLight = Color(0xFF585861)
val OutlineLight = Color(0xFF85858E)
val OutlineVariantLight = Color(0xFFD3D3D9)
val InverseSurfaceLight = Color(0xFF2E2F33)
val InverseOnSurfaceLight = Color(0xFFF1F0F4)
val SurfaceContainerLight = Color(0xFFEEEEF1)
val SurfaceContainerHighestLight = Color(0xFFE7E7EB)

// ---- Dark (near-black neutrals, the same indigo lifted periwinkle) ----
val PrimaryDark = Color(0xFFB7BEF0)
val OnPrimaryDark = Color(0xFF1B2158)
val PrimaryContainerDark = Color(0xFF3B418F)
val OnPrimaryContainerDark = Color(0xFFDEE0FA)
val SecondaryDark = Color(0xFFBFC5D4)
val OnSecondaryDark = Color(0xFF242A38)
val SecondaryContainerDark = Color(0xFF3A4052)
val OnSecondaryContainerDark = Color(0xFFD8DCE4)
val TertiaryDark = Color(0xFFE0B4C4)
val OnTertiaryDark = Color(0xFF40222D)
val TertiaryContainerDark = Color(0xFF583A47)
val OnTertiaryContainerDark = Color(0xFFF3D9E0)
val ErrorDark = Color(0xFFE0B3B0)
val OnErrorDark = Color(0xFF571C1A)
val ErrorContainerDark = Color(0xFF6E3330)
val OnErrorContainerDark = Color(0xFFF6DBD9)
val BackgroundDark = Color(0xFF101114)
val OnBackgroundDark = Color(0xFFE3E3E7)
val SurfaceDark = Color(0xFF141518)
val OnSurfaceDark = Color(0xFFE3E3E7)
val SurfaceVariantDark = Color(0xFF2C2D32)
val OnSurfaceVariantDark = Color(0xFFB4B5BC)
val OutlineDark = Color(0xFF83848D)
val OutlineVariantDark = Color(0xFF414249)
val InverseSurfaceDark = Color(0xFFE3E3E7)
val InverseOnSurfaceDark = Color(0xFF2E2F33)
val SurfaceContainerDark = Color(0xFF1C1D21)
val SurfaceContainerHighestDark = Color(0xFF27282D)

// Priority colors (TickTick-style flags), F2-muted to harmonize with the
// restrained palette — mid-lightness, low chroma so they read on both the
// light and dark surfaces; the single definition point; screens must consume
// these (Round 2 / B4)
val PriorityHigh = Color(0xFFBF4B47)
val PriorityMedium = Color(0xFFA87B22)
val PriorityLow = Color(0xFF5B8DB8)
val PriorityNone = Color(0xFF8E97A4)

fun priorityColor(priority: Int): Color = when (priority) {
    com.orvane.app.data.db.TaskPriority.HIGH -> PriorityHigh
    com.orvane.app.data.db.TaskPriority.MEDIUM -> PriorityMedium
    com.orvane.app.data.db.TaskPriority.LOW -> PriorityLow
    else -> PriorityNone
}
