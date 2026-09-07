package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun WhatsAppIcon(
    modifier: Modifier = Modifier,
    useCircleShape: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(if (useCircleShape) CircleShape else androidx.compose.foundation.shape.RoundedCornerShape(28))
            .background(Color(0xFF25D366)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Large, clear white speech bubble with prominent bottom-left tail
            val bubblePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = w * 0.12f,
                        top = h * 0.12f,
                        right = w * 0.88f,
                        bottom = h * 0.88f,
                        radiusX = w * 0.38f,
                        radiusY = h * 0.38f
                    )
                )
                moveTo(w * 0.28f, h * 0.74f)
                lineTo(w * 0.08f, h * 0.92f)
                lineTo(w * 0.40f, h * 0.82f)
                close()
            }
            drawPath(bubblePath, color = Color.White)
        }

        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "WhatsApp",
            tint = Color(0xFF25D366),
            modifier = Modifier
                .fillMaxSize(0.58f)
                .rotate(225f)
        )
    }
}

