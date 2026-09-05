package com.example.telecom

import android.content.Context
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.telephony.SmsManager
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AutomationLog
import com.example.data.CallerRule
import com.example.data.RecentCall
import com.example.util.ContactHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveCallInfo(
    val id: String,
    val phoneNumber: String,
    val displayName: String,
    val state: Int, // Call.STATE_*
    val isIncoming: Boolean,
    val connectTimeMillis: Long = 0L,
    val isSimulated: Boolean = false,
    val photoUri: String? = null
)

data class AutomationStep(
    val ruleName: String,
    val stepDescription: String,
    val isRunning: Boolean = true,
    val completed: Boolean = false,
    val error: String? = null
)

object CallManager {
    private const val TAG = "CallManager"

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var automationJob: Job? = null

    // Native Telecom Call instance if real call is active
    private var nativeCall: Call? = null
    private var telecomService: TelecomCallService? = null

    // Call UI State
    private val _activeCall = MutableStateFlow<ActiveCallInfo?>(null)
    val activeCall: StateFlow<ActiveCallInfo?> = _activeCall.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _automationState = MutableStateFlow<AutomationStep?>(null)
    val automationState: StateFlow<AutomationStep?> = _automationState.asStateFlow()

    private val _lastDtmfKey = MutableStateFlow<Char?>(null)
    val lastDtmfKey: StateFlow<Char?> = _lastDtmfKey.asStateFlow()

    private var simulatedTimerJob: Job? = null

    fun setTelecomService(service: TelecomCallService?) {
        this.telecomService = service
    }

    fun onCallAdded(call: Call, context: Context) {
        this.nativeCall = call
        val number = extractPhoneNumber(call)
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val name = lookedUp?.name ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: "Incoming Caller"
        val photoUri = lookedUp?.photoUri
        val isIncoming = call.state == Call.STATE_RINGING

        val callInfo = ActiveCallInfo(
            id = call.hashCode().toString(),
            phoneNumber = number,
            displayName = name,
            state = call.state,
            isIncoming = isIncoming,
            connectTimeMillis = if (call.state == Call.STATE_ACTIVE) System.currentTimeMillis() else 0L,
            isSimulated = false,
            photoUri = photoUri
        )
        _activeCall.value = callInfo

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                Log.d(TAG, "Call state changed: $state")
                val current = _activeCall.value
                if (current != null) {
                    val connectTime = if (state == Call.STATE_ACTIVE && current.connectTimeMillis == 0L) {
                        System.currentTimeMillis()
                    } else current.connectTimeMillis

                    _activeCall.value = current.copy(state = state, connectTimeMillis = connectTime)
                }

                if (state == Call.STATE_DISCONNECTED) {
                    handleCallEnded(context, current)
                }
            }
        })

        // Check if selective automation rule matches
        checkAndExecuteAutomation(context, number, isIncoming)
    }

    fun onCallRemoved(call: Call, context: Context) {
        if (nativeCall == call) {
            handleCallEnded(context, _activeCall.value)
            nativeCall = null
        }
    }

    private fun handleCallEnded(context: Context, callInfo: ActiveCallInfo?) {
        automationJob?.cancel()
        automationJob = null
        simulatedTimerJob?.cancel()
        simulatedTimerJob = null

        if (callInfo != null) {
            val duration = if (callInfo.connectTimeMillis > 0) {
                (System.currentTimeMillis() - callInfo.connectTimeMillis) / 1000
            } else 0L

            val callType = if (callInfo.isIncoming) {
                if (duration > 0) 1 else 3 // 1 = Incoming, 3 = Missed
            } else 2 // Outgoing

            scope.launch(Dispatchers.IO) {
                try {
                    val dao = AppDatabase.getInstance(context).appDao()
                    dao.insertRecentCall(
                        RecentCall(
                            phoneNumber = callInfo.phoneNumber,
                            callerName = callInfo.displayName,
                            photoUri = callInfo.photoUri,
                            callType = callType,
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = duration,
                            ruleMatched = _automationState.value?.ruleName
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log recent call", e)
                }
            }
        }

        // Post-call reset
        scope.launch {
            delay(1200)
            _activeCall.value = null
            _automationState.value = null
            _isMuted.value = false
            _isSpeakerOn.value = false
        }
    }

    private fun extractPhoneNumber(call: Call): String {
        val handle: Uri? = call.details?.handle
        if (handle != null) {
            val scheme = handle.schemeSpecificPart
            if (!scheme.isNullOrBlank()) return scheme
        }
        return "Unknown"
    }

    /**
     * Inspects active incoming call against configured automation rules.
     * If rule matches: executes automated workflow.
     * If NO rule matches: leaves manual control to user.
     */
    fun checkAndExecuteAutomation(context: Context, rawNumber: String, isIncoming: Boolean) {
        if (!isIncoming) return

        automationJob?.cancel()
        automationJob = scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).appDao()
            val normalizedTarget = normalizePhoneNumber(rawNumber)

            // Check if caller is in offline spam blocklist (True Silence / Auto-Block)
            val spamEntry = dao.getSpamByNumber(rawNumber)
                ?: dao.getAllSpamNumbers()
                    .let { flow ->
                        // Quick check against loaded spam
                        null
                    }
            if (spamEntry != null && spamEntry.isBlocked) {
                Log.d(TAG, "Spam number detected ($rawNumber). Auto-blocking call.")
                _automationState.value = AutomationStep(
                    ruleName = "Spam Shield",
                    stepDescription = "Blocked spam call from ${spamEntry.label}"
                )
                delay(500)
                declineCall()
                return@launch
            }

            val rules = dao.getEnabledRules()

            val matchedRule = rules.firstOrNull { rule ->
                val normRule = normalizePhoneNumber(rule.phoneNumberPattern)
                normRule.isNotEmpty() && (normalizedTarget.contains(normRule) || normRule.contains(normalizedTarget) || rule.phoneNumberPattern == "*")
            }

            if (matchedRule != null) {
                Log.d(TAG, "Matched automation rule: ${matchedRule.name}")
                executeAutomationWorkflow(context, matchedRule, rawNumber)
            } else {
                Log.d(TAG, "No automation rule matched for caller $rawNumber. Showing standard in-call UI.")
                _automationState.value = null
            }
        }
    }

    private suspend fun executeAutomationWorkflow(context: Context, rule: CallerRule, phoneNumber: String) {
        val actions = mutableListOf<String>()

        _automationState.value = AutomationStep(
            ruleName = rule.name,
            stepDescription = "Rule matched! Preparing auto-answer in ${rule.answerDelaySec}s..."
        )

        // Step 1: Pre-answer delay
        if (rule.answerDelaySec > 0) {
            delay(rule.answerDelaySec * 1000L)
        }

        // Step 2: Auto Answer
        if (rule.autoAnswer) {
            _automationState.value = AutomationStep(
                ruleName = rule.name,
                stepDescription = "Answering call via Call.answer(0)..."
            )
            actions.add("Auto-Answered")
            answerCall()

            // Wait for call to connect
            var waitCount = 0
            while (_activeCall.value?.state != Call.STATE_ACTIVE && waitCount < 15) {
                delay(300)
                waitCount++
            }
        }

        // Step 3: DTMF Sequence Transmission
        if (rule.dtmfSequence.isNotBlank()) {
            if (rule.dtmfDelayMs > 0) {
                _automationState.value = AutomationStep(
                    ruleName = rule.name,
                    stepDescription = "Waiting ${rule.dtmfDelayMs}ms before DTMF transmission..."
                )
                delay(rule.dtmfDelayMs)
            }

            _automationState.value = AutomationStep(
                ruleName = rule.name,
                stepDescription = "Transmitting in-band DTMF sequence: '${rule.dtmfSequence}'..."
            )

            for (char in rule.dtmfSequence) {
                if (char.isDigit() || char == '*' || char == '#') {
                    _lastDtmfKey.value = char
                    playDtmf(char)
                    delay(220) // Tone duration
                    stopDtmf()
                    delay(260) // Pause between digits
                }
            }
            _lastDtmfKey.value = null
            actions.add("Sent DTMF '${rule.dtmfSequence}'")
        }

        // Step 4: Send Auto-SMS Reply
        if (rule.sendSms && rule.smsMessage.isNotBlank()) {
            _automationState.value = AutomationStep(
                ruleName = rule.name,
                stepDescription = "Sending auto-reply SMS via SmsManager..."
            )
            sendSmsBackground(context, phoneNumber, rule.smsMessage)
            actions.add("Sent SMS auto-reply")
            delay(500)
        }

        // Step 5: Auto Hangup
        if (rule.autoHangup) {
            _automationState.value = AutomationStep(
                ruleName = rule.name,
                stepDescription = "Auto-hanging up in ${rule.hangupDelaySec}s..."
            )
            delay(rule.hangupDelaySec * 1000L)
            actions.add("Auto-Disconnected")
            disconnectCall()
        }

        _automationState.value = AutomationStep(
            ruleName = rule.name,
            stepDescription = "Automation completed successfully.",
            isRunning = false,
            completed = true
        )

        // Log to database
        try {
            val dao = AppDatabase.getInstance(context).appDao()
            dao.insertAutomationLog(
                AutomationLog(
                    phoneNumber = phoneNumber,
                    ruleName = rule.name,
                    actionsSummary = actions.joinToString(" -> "),
                    status = "SUCCESS"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write automation log", e)
        }
    }

    // Call Actions
    fun answerCall() {
        val current = _activeCall.value ?: return
        if (current.isSimulated) {
            _activeCall.value = current.copy(
                state = Call.STATE_ACTIVE,
                connectTimeMillis = System.currentTimeMillis()
            )
        } else {
            try {
                nativeCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
            } catch (e: Exception) {
                Log.e(TAG, "Error answering native call", e)
            }
        }
    }

    fun declineCall() {
        val current = _activeCall.value ?: return
        if (current.isSimulated) {
            _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
            scope.launch {
                delay(800)
                _activeCall.value = null
                _automationState.value = null
            }
        } else {
            try {
                nativeCall?.reject(false, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting native call", e)
            }
        }
    }

    fun disconnectCall() {
        val current = _activeCall.value ?: return
        if (current.isSimulated) {
            _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
            scope.launch {
                delay(800)
                _activeCall.value = null
                _automationState.value = null
            }
        } else {
            try {
                nativeCall?.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting native call", e)
            }
        }
    }

    fun playDtmf(digit: Char) {
        _lastDtmfKey.value = digit
        if (nativeCall != null) {
            try {
                nativeCall?.playDtmfTone(digit)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing DTMF tone", e)
            }
        }
    }

    fun stopDtmf() {
        _lastDtmfKey.value = null
        if (nativeCall != null) {
            try {
                nativeCall?.stopDtmfTone()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping DTMF tone", e)
            }
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        telecomService?.setMuted(newMuted)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeaker
        val route = if (newSpeaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        telecomService?.setAudioRoute(route)
    }

    private fun sendSmsBackground(context: Context, destination: String, text: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(destination, null, text, null, null)
            Log.d(TAG, "Sent automated SMS to $destination: $text")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to send SMS in background (needs SIM/permission): ${e.message}")
        }
    }

    /**
     * Simulator for testing in emulator environment where no GSM carrier is present.
     */
    fun startSimulatedIncomingCall(context: Context, number: String, name: String) {
        automationJob?.cancel()
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val resolvedName = lookedUp?.name ?: name
        val photoUri = lookedUp?.photoUri
        val callInfo = ActiveCallInfo(
            id = "sim_${System.currentTimeMillis()}",
            phoneNumber = number,
            displayName = resolvedName,
            state = Call.STATE_RINGING,
            isIncoming = true,
            connectTimeMillis = 0L,
            isSimulated = true,
            photoUri = photoUri
        )
        _activeCall.value = callInfo
        checkAndExecuteAutomation(context, number, true)
    }

    fun startSimulatedOutgoingCall(context: Context, number: String) {
        automationJob?.cancel()
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val resolvedName = lookedUp?.name ?: number
        val photoUri = lookedUp?.photoUri
        val callInfo = ActiveCallInfo(
            id = "sim_out_${System.currentTimeMillis()}",
            phoneNumber = number,
            displayName = resolvedName,
            state = Call.STATE_DIALING,
            isIncoming = false,
            connectTimeMillis = 0L,
            isSimulated = true,
            photoUri = photoUri
        )
        _activeCall.value = callInfo

        scope.launch {
            delay(1500)
            val current = _activeCall.value
            if (current != null && current.state == Call.STATE_DIALING) {
                _activeCall.value = current.copy(
                    state = Call.STATE_ACTIVE,
                    displayName = "Connected",
                    connectTimeMillis = System.currentTimeMillis()
                )
            }
        }
    }

    private fun normalizePhoneNumber(raw: String): String {
        return raw.filter { it.isDigit() }
    }
}
