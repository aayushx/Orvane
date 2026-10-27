package com.orvane.app.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Widget color factory. The official [androidx.glance.unit.ColorProvider] companion factory
 * is annotated @RestrictTo(LIBRARY_GROUP), so apps cannot call it; the [ColorProvider]
 * interface itself is public, so we implement our own fixed color provider here.
 */
fun widgetColor(color: Color): ColorProvider = FixedWidgetColorProvider(color)

private class FixedWidgetColorProvider(private val color: Color) : ColorProvider {
    override fun getColor(context: Context): Color = color
}
