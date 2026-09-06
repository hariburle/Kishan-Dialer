package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KeypadKey(val digit: Char, val subText: String = "")

val standardDialpadKeys = listOf(
    listOf(KeypadKey('1', ""), KeypadKey('2', "ABC"), KeypadKey('3', "DEF")),
    listOf(KeypadKey('4', "GHI"), KeypadKey('5', "JKL"), KeypadKey('6', "MNO")),
    listOf(KeypadKey('7', "PQRS"), KeypadKey('8', "TUV"), KeypadKey('9', "WXYZ")),
    listOf(KeypadKey('*', ""), KeypadKey('0', "+"), KeypadKey('#', ""))
)

@Composable
fun Keypad(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    speedDialMap: Map<Char, String> = emptyMap(),
    onDigitPress: (Char) -> Unit,
    onDigitRelease: (Char) -> Unit = {},
    onDigitLongPress: ((Char) -> Unit)? = null
) {
    val view = LocalView.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        standardDialpadKeys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    KeypadButton(
                        key = key,
                        compact = compact,
                        speedDialLabel = speedDialMap[key.digit],
                        onPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDigitPress(key.digit)
                        },
                        onRelease = {
                            onDigitRelease(key.digit)
                        },
                        onLongPress = onDigitLongPress?.let { callback ->
                            {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                callback(key.digit)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun KeypadButton(
    key: KeypadKey,
    compact: Boolean,
    speedDialLabel: String? = null,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Release, is PressInteraction.Cancel -> onRelease()
                else -> {}
            }
        }
    }

    val buttonSize = if (compact) 56.dp else 72.dp
    val primaryTextSize = if (compact) 22.sp else 28.sp
    val keyShape = RoundedCornerShape(if (compact) 10.dp else 14.dp)

    @OptIn(ExperimentalFoundationApi::class)
    Surface(
        modifier = Modifier
            .size(buttonSize)
            .clip(keyShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onPress,
                onLongClick = onLongPress
            )
            .testTag("keypad_digit_${key.digit}"),
        shape = keyShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = key.digit.toString(),
                    fontSize = primaryTextSize,
                    fontWeight = FontWeight.Medium,
                    lineHeight = primaryTextSize,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!speedDialLabel.isNullOrBlank()) {
                    Text(
                        text = speedDialLabel,
                        fontSize = if (compact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (key.subText.isNotEmpty()) {
                    Text(
                        text = key.subText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
