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
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .padding(horizontal = 8.dp)
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val maxDragPx = with(density) {
                ((maxWidth - 64.dp) / 2f - 4.dp).toPx().coerceAtLeast(100f)
            }
            val dragFraction = (dragOffset.value / maxDragPx).coerceIn(-1f, 1f)

            // Horizontal Slider Track
            Surface(
                shape = CircleShape,
                color = when {
                    dragFraction > 0.15f -> Color(0xFF16A34A).copy(alpha = 0.15f + dragFraction * 0.2f)
                    dragFraction < -0.15f -> Color(0xFFDC2626).copy(alpha = 0.15f + (-dragFraction) * 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                },
                border = BorderStroke(
                    1.5.dp,
                    when {
                        dragFraction > 0.3f -> Color(0xFF16A34A).copy(alpha = 0.8f)
                        dragFraction < -0.3f -> Color(0xFFDC2626).copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Left End: Decline indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .clickable { onDecline() }
                            .testTag("incall_decline_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Decline Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "‹ Decline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.alpha(if (dragFraction < -0.3f) 1f else 0.8f)
                        )
                    }

                    // Right End: Answer indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .clickable { onAnswer() }
                            .testTag("incall_answer_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Answer ›",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.alpha(if (dragFraction > 0.3f) 1f else 0.8f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Answer Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Draggable Center Slider Handle (Follows finger smoothly from center all the way to both ends)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                            .pointerInput(maxDragPx) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        val currentVal = dragOffset.value
                                        val triggerThreshold = maxDragPx * 0.55f
                                        if (currentVal >= triggerThreshold) {
                                            coroutineScope.launch {
                                                dragOffset.animateTo(maxDragPx, tween(150))
                                                onAnswer()
                                                dragOffset.snapTo(0f)
                                            }
                                        } else if (currentVal <= -triggerThreshold) {
                                            coroutineScope.launch {
                                                dragOffset.animateTo(-maxDragPx, tween(150))
                                                onDecline()
                                                dragOffset.snapTo(0f)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                dragOffset.animateTo(
                                                    0f,
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        coroutineScope.launch {
                                            dragOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        coroutineScope.launch {
                                            val nextVal = (dragOffset.value + dragAmount).coerceIn(-maxDragPx, maxDragPx)
                                            dragOffset.snapTo(nextVal)
                                        }
                                    }
                                )
                            }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                dragFraction > 0.25f -> Color(0xFF16A34A)
                                dragFraction < -0.25f -> Color(0xFFDC2626)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            shadowElevation = if (isDragging) 8.dp else 4.dp,
                            border = BorderStroke(
                                2.dp,
                                when {
                                    dragFraction > 0.25f -> Color(0xFF16A34A)
                                    dragFraction < -0.25f -> Color(0xFFDC2626)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        dragFraction < -0.25f -> Icons.Default.CallEnd
                                        else -> Icons.Default.Call
                                    },
                                    contentDescription = "Slide to Answer or Decline",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
