package com.example.ui.screens

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecom.ActiveCallInfo
import com.example.telecom.AutomationStep
import com.example.ui.components.Keypad
import com.example.util.ContactHelper
import kotlinx.coroutines.delay

enum class ScreeningSender {
    AI_ASSISTANT,
    CALLER
}

data class ScreeningMessage(
    val sender: ScreeningSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScreeningAnalysis(
    val verdict: String,
    val isSpam: Boolean,
    val confidence: Int,
    val category: String,
    val suggestedAction: String
)

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
    audioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    supportedAudioRoutes: Int = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER,
    bluetoothDeviceName: String? = null,
    onSelectAudioRoute: (Int) -> Unit = {},
    onPlayDtmf: (Char) -> Unit,
    onStopDtmf: (Char) -> Unit,
    onDeclineWithSms: (String) -> Unit = {},
    onSavePostCallNote: ((note: String?, reminderMinutes: Long?) -> Unit)? = null,
    onMarkSpam: ((String) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var callSeconds by remember { mutableLongStateOf(0L) }
    var enteredDtmfHistory by remember { mutableStateOf("") }
    var postCallNote by remember { mutableStateOf("") }
    var postCallReminderMins by remember { mutableStateOf<Long?>(null) }
    var noteSaved by remember { mutableStateOf(false) }
    var isUserInteractingWithNote by remember { mutableStateOf(false) }
    var autoCloseRemainingSeconds by remember { mutableIntStateOf(6) }
    var showAudioRouteSelector by remember { mutableStateOf(false) }

    // AI Call Screening State
    var isScreeningMode by remember { mutableStateOf(false) }
    var screeningMessages by remember { mutableStateOf<List<ScreeningMessage>>(emptyList()) }
    var screeningAnalysis by remember { mutableStateOf<ScreeningAnalysis?>(null) }
    var isCallerTranscribing by remember { mutableStateOf(false) }
    var isAiSpeaking by remember { mutableStateOf(false) }

    // Automated screening conversation when screening mode is engaged
    LaunchedEffect(isScreeningMode) {
        if (isScreeningMode && screeningMessages.isEmpty()) {
            isAiSpeaking = true
            delay(400)
            screeningMessages = listOf(
                ScreeningMessage(
                    sender = ScreeningSender.AI_ASSISTANT,
                    text = "Hi, I'm screening calls for Kishan. May I ask who is calling and what this is regarding?"
                )
            )
            delay(1200)
            isAiSpeaking = false
            isCallerTranscribing = true
            delay(1800)

            val isSpamNumber = (callInfo.communityInfo?.spamScore ?: 0) > 40 ||
                callInfo.displayName.contains("Spam", ignoreCase = true) ||
                callInfo.displayName.contains("Telemarket", ignoreCase = true) ||
                callInfo.displayName.contains("Credit", ignoreCase = true) ||
                callInfo.displayName.contains("Unknown", ignoreCase = true) ||
                callInfo.phoneNumber.contains("800")

            if (isSpamNumber) {
                screeningMessages = screeningMessages + ScreeningMessage(
                    sender = ScreeningSender.CALLER,
                    text = "Hello! This is Kevin from Cardholder Services. You have been pre-selected for an instant zero-fee loan offer valid today only."
                )
                screeningAnalysis = ScreeningAnalysis(
                    verdict = "Suspected Telemarketer / Robocall",
                    isSpam = true,
                    confidence = 97,
                    category = "Unsolicited Financial Offer",
                    suggestedAction = "Block & Report Spam"
                )
            } else {
                val callerName = if (callInfo.displayName.isNotBlank() && callInfo.displayName != "Incoming Caller" && callInfo.displayName != "Calling...") {
                    callInfo.displayName
                } else "Express Courier"
                screeningMessages = screeningMessages + ScreeningMessage(
                    sender = ScreeningSender.CALLER,
                    text = "Hello Kishan, this is $callerName calling regarding the scheduled package delivery at your address."
                )
                screeningAnalysis = ScreeningAnalysis(
                    verdict = "Verified Legitimate Caller",
                    isSpam = false,
                    confidence = 94,
                    category = "Delivery & Service",
                    suggestedAction = "Safe to Connect"
                )
            }
            isCallerTranscribing = false
        }
    }

    val onScreeningAskMore = {
        coroutineScope.launch {
            isAiSpeaking = true
            screeningMessages = screeningMessages + ScreeningMessage(
                sender = ScreeningSender.AI_ASSISTANT,
                text = "Could you please clarify your business name and reference order number?"
            )
            delay(1200)
            isAiSpeaking = false
            isCallerTranscribing = true
            delay(1600)
            val isSpam = screeningAnalysis?.isSpam == true
            screeningMessages = screeningMessages + ScreeningMessage(
                sender = ScreeningSender.CALLER,
                text = if (isSpam)
                    "We are an independent underwriting partner. Press 1 to connect with our senior loan advisor."
                else
                    "Certainly, this is Swift Logistics reference ID #94812 with the registered delivery."
            )
            isCallerTranscribing = false
        }
    }

    val onScreeningTellBusy = {
        coroutineScope.launch {
            isAiSpeaking = true
            screeningMessages = screeningMessages + ScreeningMessage(
                sender = ScreeningSender.AI_ASSISTANT,
                text = "Understood. Kishan is in a meeting right now and will follow up with you later. Thank you, goodbye."
            )
            delay(1200)
            isAiSpeaking = false
            delay(800)
            onDisconnect()
        }
    }

    LaunchedEffect(callInfo.state, isUserInteractingWithNote, noteSaved) {
        if (callInfo.state == Call.STATE_DISCONNECTED && !isUserInteractingWithNote && !noteSaved) {
            autoCloseRemainingSeconds = 6
            while (autoCloseRemainingSeconds > 0) {
                delay(1000)
                if (isUserInteractingWithNote || noteSaved) break
                autoCloseRemainingSeconds--
            }
            if (!isUserInteractingWithNote && !noteSaved) {
                onDismiss()
            }
        }
    }

    LaunchedEffect(noteSaved) {
        if (noteSaved) {
            delay(1000)
            onDismiss()
        }
    }

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

    val isVoicemail = callInfo.displayName.equals("Voicemail", ignoreCase = true) ||
        ContactHelper.isVoicemailNumber(context, callInfo.phoneNumber)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("in_call_screen"),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Minimize button, Status, Caller details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Top row with minimize button and status chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("minimize_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize call",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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

                    Spacer(modifier = Modifier.size(48.dp))
                }

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
                            if (isVoicemail) {
                                Icon(
                                    imageVector = Icons.Default.Voicemail,
                                    contentDescription = "Voicemail",
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else if (callInfo.displayName.isNotBlank() && callInfo.displayName != "Incoming Caller" && callInfo.displayName != "Calling...") {
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

                    // Contextual Caller ID ("Call Reason")
                    if (!callInfo.callReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Call Reason",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Reason: ${callInfo.callReason}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Live Community Caller ID Info
                    callInfo.communityInfo?.let { comm ->
                        Spacer(modifier = Modifier.height(6.dp))
                        val isSpam = comm.spamScore > 50
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSpam) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpam) Icons.Default.Warning else Icons.Default.Verified,
                                    contentDescription = "Community Verified",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSpam) Color(0xFFDC2626) else Color(0xFF0284C7)
                                )
                                Text(
                                    text = "${comm.verificationType} • ${comm.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpam) Color(0xFF991B1B) else Color(0xFF0369A1)
                                )
                            }
                        }
                    }
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .testTag("incall_dtmf_history_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dialpad,
                                    contentDescription = "Keypad input",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = enteredDtmfHistory,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
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

            // Middle Section: AI Call Screening Panel (When Screening is Active)
            AnimatedVisibility(
                visible = isScreeningMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("ai_screening_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "AI Call Screening",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isAiSpeaking) "AI Speaking..." else if (isCallerTranscribing) "Caller Speaking..." else "Live Transcript",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Chat Messages / Transcripts
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            screeningMessages.forEach { msg ->
                                val isAi = msg.sender == ScreeningSender.AI_ASSISTANT
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isAi) 2.dp else 14.dp,
                                            bottomEnd = if (isAi) 14.dp else 2.dp
                                        ),
                                        color = if (isAi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.widthIn(max = 260.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(
                                                text = if (isAi) "🤖 AI Assistant" else "👤 Caller",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isAi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            if (isCallerTranscribing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = "Transcribing caller speech...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Real-time AI Analysis Verdict
                        screeningAnalysis?.let { analysis ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (analysis.isSpam) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (analysis.isSpam) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (analysis.isSpam) Color(0xFFDC2626) else Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${analysis.verdict} (${analysis.confidence}%)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (analysis.isSpam) Color(0xFF991B1B) else Color(0xFF166534)
                                        )
                                        Text(
                                            text = "${analysis.category} • ${analysis.suggestedAction}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (analysis.isSpam) Color(0xFFB91C1C) else Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }

                        // Interactive Quick Prompts for User to instruct AI
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = { onScreeningAskMore() },
                                label = { Text("Ask More Details", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                            AssistChip(
                                onClick = { onScreeningTellBusy() },
                                label = { Text("Tell them I'll call back", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Screening Decisions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    onMarkSpam?.invoke(callInfo.phoneNumber)
                                    onDisconnect()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Block & End", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { isScreeningMode = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Take Call", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Bottom Section: Audio controls & Call End/Answer buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Secondary Controls Row: Mute, Keypad, Speaker, AI Screen
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

                    // AI Screen Toggle
                    InCallControlButton(
                        icon = Icons.Default.SmartToy,
                        label = if (isScreeningMode) "Screening" else "AI Screen",
                        isActive = isScreeningMode,
                        onClick = { isScreeningMode = !isScreeningMode },
                        testTag = "incall_screening_toggle_button"
                    )

                    // Audio Route (Speaker / Handset / Bluetooth)
                    val audioIcon = when (audioRoute) {
                        CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.BluetoothAudio
                        CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                        else -> Icons.Default.PhoneAndroid
                    }
                    val audioLabel = when (audioRoute) {
                        CallAudioState.ROUTE_BLUETOOTH -> bluetoothDeviceName?.take(9) ?: "Bluetooth"
                        CallAudioState.ROUTE_SPEAKER -> "Speaker"
                        else -> "Handset"
                    }
                    InCallControlButton(
                        icon = audioIcon,
                        label = audioLabel,
                        isActive = audioRoute == CallAudioState.ROUTE_SPEAKER || audioRoute == CallAudioState.ROUTE_BLUETOOTH,
                        onClick = {
                            showAudioRouteSelector = true
                        },
                        testTag = "incall_speaker_button"
                    )
                }

                // Primary Call Actions
                if (callInfo.state == Call.STATE_RINGING) {
                    // Quick-Decline SMS Chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Quick Decline with SMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val quickDeclineChips = listOf(
                            "Can't talk now. What's up?",
                            "In a meeting, text me.",
                            "I'll call you right back.",
                            "On my way."
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(quickDeclineChips) { chipText ->
                                SuggestionChip(
                                    onClick = { onDeclineWithSms(chipText) },
                                    label = {
                                        Text(
                                            text = chipText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Message,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_decline_chip")
                                )
                            }
                        }
                    }

                    // Incoming call: Answer (Green), AI Screen (Blue), & Decline (Red)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                        // AI Screen Button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledIconButton(
                                onClick = {
                                    isScreeningMode = true
                                    onAnswer()
                                },
                                modifier = Modifier
                                    .size(70.dp)
                                    .testTag("incall_ai_screen_button"),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "AI Screen Call",
                                    modifier = Modifier.size(34.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AI Screen",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
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
                } else if (callInfo.state == Call.STATE_DISCONNECTED) {
                    // Post-Call Follow-up Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("post_call_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NoteAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (noteSaved) "Note & Reminder Saved!" else "Add Post-Call Note / Reminder",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (!noteSaved && !isUserInteractingWithNote) {
                                    Text(
                                        text = "Closing in ${autoCloseRemainingSeconds}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            if (!noteSaved) {
                                OutlinedTextField(
                                    value = postCallNote,
                                    onValueChange = {
                                        postCallNote = it
                                        isUserInteractingWithNote = true
                                    },
                                    placeholder = {
                                        Text(
                                            "Call summary or action items (stays open while typing)...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                isUserInteractingWithNote = true
                                            }
                                        }
                                        .testTag("post_call_note_input"),
                                    singleLine = false,
                                    maxLines = 3
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(15L to "In 15m", 60L to "In 1h", 1440L to "Tomorrow").forEach { (mins, label) ->
                                        AssistChip(
                                            onClick = {
                                                postCallReminderMins = if (postCallReminderMins == mins) null else mins
                                                isUserInteractingWithNote = true
                                            },
                                            label = { Text(label, fontSize = 11.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = if (postCallReminderMins == mins) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                                labelColor = if (postCallReminderMins == mins) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (postCallNote.isNotBlank()) {
                                                onSavePostCallNote?.invoke(postCallNote, postCallReminderMins)
                                            }
                                            onDismiss()
                                        }
                                    ) {
                                        Text(
                                            text = if (isUserInteractingWithNote) "Discard / Close" else "Skip",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            noteSaved = true
                                            onSavePostCallNote?.invoke(postCallNote, postCallReminderMins)
                                        },
                                        modifier = Modifier.testTag("save_post_call_note_btn")
                                    ) {
                                        Text("Save & Close", fontSize = 12.sp)
                                    }
                                }
                            }
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

    if (showAudioRouteSelector) {
        com.example.ui.components.AudioOutputSelectorDialog(
            currentRoute = audioRoute,
            supportedRoutes = supportedAudioRoutes,
            bluetoothDeviceName = bluetoothDeviceName,
            onSelectRoute = { route ->
                onSelectAudioRoute(route)
                showAudioRouteSelector = false
            },
            onDismiss = { showAudioRouteSelector = false }
        )
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
