package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun WhatsAppIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Outer green background (rounded square / circle)
            drawRoundRect(
                color = Color(0xFF25D366),
                cornerRadius = CornerRadius(w * 0.3f, w * 0.3f),
                size = size
            )

            // 2. Inner white speech bubble with tail at bottom-left
            val bubblePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = w * 0.12f,
                        top = h * 0.12f,
                        right = w * 0.88f,
                        bottom = h * 0.88f,
                        radiusX = w * 0.38f,
                        radiusY = w * 0.38f
                    )
                )
                moveTo(w * 0.28f, h * 0.76f)
                lineTo(w * 0.08f, h * 0.95f)
                lineTo(w * 0.42f, h * 0.78f)
                close()
            }
            drawPath(bubblePath, color = Color.White)
        }

        // 3. Green phone icon inside the white speech bubble
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "WhatsApp",
            tint = Color(0xFF25D366),
            modifier = Modifier
                .size(18.dp)
                .rotate(225f)
        )
    }
}
