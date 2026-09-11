package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SwipeUpAnswerView(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var answerDragY by remember { mutableFloatStateOf(0f) }
    val animatedAnswerY by animateFloatAsState(
        targetValue = answerDragY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "answer_y"
    )

    var declineDragY by remember { mutableFloatStateOf(0f) }
    val animatedDeclineY by animateFloatAsState(
        targetValue = declineDragY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "decline_y"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_chevrons")
    val chevronUpOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "chevron_up"
    )
    val chevronDownOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "chevron_down"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Decline Column (Swipe Down or Tap)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, animatedDeclineY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (declineDragY > 80f) {
                                    onDecline()
                                }
                                declineDragY = 0f
                            },
                            onDragCancel = { declineDragY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                declineDragY = (declineDragY + dragAmount).coerceIn(-10f, 160f)
                                if (declineDragY >= 120f) {
                                    onDecline()
                                    declineDragY = 0f
                                }
                            }
                        )
                    }
            ) {
                FilledIconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(70.dp)
                        .testTag("incall_decline_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Decline Call",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier
                    .size(20.dp)
                    .offset { IntOffset(0, chevronDownOffset.roundToInt()) }
            )
            Text(
                text = "Swipe down\nto decline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Answer Column (Swipe Up or Tap)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier
                    .size(20.dp)
                    .offset { IntOffset(0, chevronUpOffset.roundToInt()) }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, animatedAnswerY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (answerDragY < -80f) {
                                    onAnswer()
                                }
                                answerDragY = 0f
                            },
                            onDragCancel = { answerDragY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                answerDragY = (answerDragY + dragAmount).coerceIn(-160f, 10f)
                                if (answerDragY <= -120f) {
                                    onAnswer()
                                    answerDragY = 0f
                                }
                            }
                        )
                    }
            ) {
                FilledIconButton(
                    onClick = onAnswer,
                    modifier = Modifier
                        .size(70.dp)
                        .testTag("incall_answer_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Answer Call",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Swipe up\nto answer",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ButtonTapAnswerView(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decline Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledIconButton(
                onClick = onDecline,
                modifier = Modifier
                    .size(70.dp)
                    .testTag("incall_decline_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Decline Call",
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Decline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Answer Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledIconButton(
                onClick = onAnswer,
                modifier = Modifier
                    .size(70.dp)
                    .testTag("incall_answer_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Answer Call",
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Answer",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SwipeSliderAnswerView(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = sliderOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "slider_x"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Horizontal Slider Track
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .height(72.dp)
                .padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Left End: Decline indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .clickable { onDecline() }
                        .testTag("incall_decline_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "‹ Decline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }

                // Right End: Answer indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .clickable { onAnswer() }
                        .testTag("incall_answer_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Answer ›",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Answer Call",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Draggable Center Slider Handle
                Box(
                    modifier = Modifier
                        .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (sliderOffsetX > 90f) {
                                        onAnswer()
                                    } else if (sliderOffsetX < -90f) {
                                        onDecline()
                                    }
                                    sliderOffsetX = 0f
                                },
                                onDragCancel = { sliderOffsetX = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    sliderOffsetX = (sliderOffsetX + dragAmount).coerceIn(-140f, 140f)
                                    if (sliderOffsetX >= 115f) {
                                        onAnswer()
                                        sliderOffsetX = 0f
                                    } else if (sliderOffsetX <= -115f) {
                                        onDecline()
                                        sliderOffsetX = 0f
                                    }
                                }
                            )
                        }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            sliderOffsetX > 35f -> Color(0xFF16A34A)
                            sliderOffsetX < -35f -> Color(0xFFDC2626)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            2.dp,
                            when {
                                sliderOffsetX > 35f -> Color(0xFF16A34A)
                                sliderOffsetX < -35f -> Color(0xFFDC2626)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    sliderOffsetX < -35f -> Icons.Default.CallEnd
                                    else -> Icons.Default.Call
                                },
                                contentDescription = "Drag to Answer or Decline",
                                tint = when {
                                    sliderOffsetX > 35f || sliderOffsetX < -35f -> Color.White
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
