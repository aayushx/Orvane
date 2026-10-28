package com.orvane.app.ui.theme

import androidx.compose.ui.graphics.Color

// Baseline Material 3 palette (indigo-forward, TickTick-inspired but original)
val PrimaryLight = Color(0xFF4A5FDB)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFDFE1FF)
val OnPrimaryContainerLight = Color(0xFF00105C)
val SecondaryLight = Color(0xFF006C4C)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFF89F8C7)
val OnSecondaryContainerLight = Color(0xFF002115)
val TertiaryLight = Color(0xFF7A5466)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD9EA)
val OnTertiaryContainerLight = Color(0xFF301221)
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
val BackgroundLight = Color(0xFFFBF7FF)
val OnBackgroundLight = Color(0xFF1A1B23)
val SurfaceLight = Color(0xFFFBF8FE)
val OnSurfaceLight = Color(0xFF1A1B23)
val SurfaceVariantLight = Color(0xFFE3E1EC)
val OnSurfaceVariantLight = Color(0xFF46464F)
val OutlineLight = Color(0xFF76777F)
val OutlineVariantLight = Color(0xFFC7C5D0)
val InverseSurfaceLight = Color(0xFF2F3038)
val InverseOnSurfaceLight = Color(0xFFF1F0F7)
val SurfaceContainerLight = Color(0xFFF0EDF4)
val SurfaceContainerHighestLight = Color(0xFFEAE7F0)

val PrimaryDark = Color(0xFFBCC2FF)
val OnPrimaryDark = Color(0xFF1A2678)
val PrimaryContainerDark = Color(0xFF2E40A6)
val OnPrimaryContainerDark = Color(0xFFDFE1FF)
val SecondaryDark = Color(0xFF6CDCAC)
val OnSecondaryDark = Color(0xFF003825)
val SecondaryContainerDark = Color(0xFF005236)
val OnSecondaryContainerDark = Color(0xFF89F8C7)
val TertiaryDark = Color(0xFFEAB6CE)
val OnTertiaryDark = Color(0xFF482637)
val TertiaryContainerDark = Color(0xFF613C4F)
val OnTertiaryContainerDark = Color(0xFFFFD9EA)
val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)
val BackgroundDark = Color(0xFF121319)
val OnBackgroundDark = Color(0xFFE3E1EC)
val SurfaceDark = Color(0xFF121319)
val OnSurfaceDark = Color(0xFFE3E1EC)
val SurfaceVariantDark = Color(0xFF46464F)
val OnSurfaceVariantDark = Color(0xFFC7C5D0)
val OutlineDark = Color(0xFF90909A)
val OutlineVariantDark = Color(0xFF46464F)
val InverseSurfaceDark = Color(0xFFE3E1EC)
val InverseOnSurfaceDark = Color(0xFF2F3038)
val SurfaceContainerDark = Color(0xFF1E2027)
val SurfaceContainerHighestDark = Color(0xFF2A2C33)

// Priority colors (TickTick-style flags) — the single definition point;
// screens must consume these (Round 2 / B4)
val PriorityHigh = Color(0xFFE53935)
val PriorityMedium = Color(0xFFF6A623)
val PriorityLow = Color(0xFF4CA6E8)
val PriorityNone = Color(0xFF9DA3AE)

fun priorityColor(priority: Int): Color = when (priority) {
    com.orvane.app.data.db.TaskPriority.HIGH -> PriorityHigh
    com.orvane.app.data.db.TaskPriority.MEDIUM -> PriorityMedium
    com.orvane.app.data.db.TaskPriority.LOW -> PriorityLow
    else -> PriorityNone
}

