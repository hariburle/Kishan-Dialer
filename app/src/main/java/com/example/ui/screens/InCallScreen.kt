package com.example.ui.screens

import android.telecom.Call
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecom.ActiveCallInfo
import com.example.telecom.AutomationStep
import com.example.ui.components.Keypad
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(
    callInfo: ActiveCallInfo,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    automationStep: AutomationStep?,
    lastDtmfKey: Char?,
    showKeypad: Boolean,
    onToggleKeypad: () -> Unit,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onPlayDtmf: (Char) -> Unit,
    onStopDtmf: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var callSeconds by remember { mutableLongStateOf(0L) }
    var enteredDtmfHistory by remember { mutableStateOf("") }

    LaunchedEffect(callInfo.state, callInfo.connectTimeMillis) {
        if (callInfo.state == Call.STATE_ACTIVE) {
            while (true) {
                val start = callInfo.connectTimeMillis
                if (start > 0) {
                    callSeconds = (System.currentTimeMillis() - start) / 1000
                }
                delay(1000)
            }
        } else {
            callSeconds = 0L
        }
    }

    val stateLabel = when (callInfo.state) {
        Call.STATE_RINGING -> "Incoming Call..."
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Connecting..."
        Call.STATE_ACTIVE -> {
            val mins = callSeconds / 60
            val secs = callSeconds % 60
            String.format("%02d:%02d", mins, secs)
        }
        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> "Call Ended"
        else -> "In Call"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("in_call_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Status, Caller details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Status Chip
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = stateLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (callInfo.state == Call.STATE_ACTIVE)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    leadingIcon = {
                        if (automationStep?.isRunning == true) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )

                // Caller Avatar (shows contact photo if available)
                Surface(
                    modifier = Modifier
                        .size(112.dp)
                        .testTag("in_call_avatar"),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shadowElevation = 4.dp
                ) {
                    if (!callInfo.photoUri.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = callInfo.photoUri,
                            contentDescription = "Caller Photo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            if (callInfo.displayName.isNotBlank() && callInfo.displayName != "Incoming Caller" && callInfo.displayName != "Calling...") {
                                Text(
                                    text = callInfo.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Caller",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Caller Name & Number
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = callInfo.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = callInfo.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Automation Step Banner (if rule matched and running)
                if (automationStep != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("automation_step_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Automation",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Auto-Engine: ${automationStep.ruleName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = automationStep.stepDescription,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (automationStep.isRunning) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }

                // DTMF feedback badge
                if (lastDtmfKey != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "DTMF Tone: $lastDtmfKey",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Middle Section: Optional DTMF Keypad
            AnimatedVisibility(
                visible = showKeypad,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    if (enteredDtmfHistory.isNotEmpty()) {
                        Text(
                            text = enteredDtmfHistory,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Keypad(
                        compact = true,
                        onDigitPress = { char ->
                            enteredDtmfHistory += char
                            onPlayDtmf(char)
                        },
                        onDigitRelease = { char ->
                            onStopDtmf(char)
                        }
                    )
                }
            }

            // Bottom Section: Audio controls & Call End/Answer buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Secondary Controls Row: Mute, Keypad, Speaker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    InCallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        isActive = isMuted,
                        onClick = onToggleMute,
                        testTag = "incall_mute_button"
                    )

                    // Keypad
                    InCallControlButton(
                        icon = Icons.Default.Dialpad,
                        label = if (showKeypad) "Hide Keypad" else "Keypad",
                        isActive = showKeypad,
                        onClick = onToggleKeypad,
                        testTag = "incall_keypad_button"
                    )

                    // Speaker
                    InCallControlButton(
                        icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                        label = if (isSpeakerOn) "Speaker" else "Earpiece",
                        isActive = isSpeakerOn,
                        onClick = onToggleSpeaker,
                        testTag = "incall_speaker_button"
                    )
                }

                // Primary Call Actions
                if (callInfo.state == Call.STATE_RINGING) {
                    // Incoming call: Answer (Green) & Decline (Red)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decline Button
                        FilledIconButton(
                            onClick = onDecline,
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("incall_decline_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Answer Button
                        FilledIconButton(
                            onClick = onAnswer,
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("incall_answer_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Answer Call",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                } else {
                    // Active or Outgoing Call: End Call (Red)
                    FilledIconButton(
                        onClick = onDisconnect,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("incall_end_call_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InCallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .testTag(testTag),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
