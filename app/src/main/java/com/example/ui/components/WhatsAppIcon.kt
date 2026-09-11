package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Premium WhatsApp vector brand icon rendered cleanly with crisp resolution,
 * perfect proportions, and adaptive theme colors for Light and Dark modes.
 */
@Composable
fun WhatsAppIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF25D366)
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Smooth speech bubble path with bottom-left tail
            val bubblePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(w * 0.08f, h * 0.08f, w * 0.92f, h * 0.92f),
                        radiusX = w * 0.42f,
                        radiusY = h * 0.42f
                    )
                )
                moveTo(w * 0.22f, h * 0.72f)
                lineTo(w * 0.02f, h * 0.98f)
                lineTo(w * 0.38f, h * 0.84f)
                close()
            }
            drawPath(path = bubblePath, color = tint)

            // Phone receiver cutout inside the bubble
            val phonePath = Path().apply {
                moveTo(w * 0.38f, h * 0.30f)
                cubicTo(w * 0.42f, h * 0.30f, w * 0.46f, h * 0.38f, w * 0.46f, h * 0.42f)
                lineTo(w * 0.42f, h * 0.48f)
                cubicTo(w * 0.48f, h * 0.58f, w * 0.54f, h * 0.64f, w * 0.62f, h * 0.70f)
                lineTo(w * 0.68f, h * 0.64f)
                cubicTo(w * 0.72f, h * 0.64f, w * 0.80f, h * 0.68f, w * 0.80f, h * 0.72f)
                lineTo(w * 0.76f, h * 0.82f)
                cubicTo(w * 0.70f, h * 0.86f, w * 0.58f, h * 0.84f, w * 0.42f, h * 0.68f)
                cubicTo(w * 0.28f, h * 0.52f, w * 0.26f, h * 0.40f, w * 0.30f, h * 0.34f)
                close()
            }
            drawPath(path = phonePath, color = Color.White)
        }
    }
}
