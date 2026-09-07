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
    val photoUri: String? = null,
    val callReason: String? = null,
    val communityInfo: com.example.util.CommunityCallerInfo? = null
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

    private val _currentAudioRoute = MutableStateFlow(CallAudioState.ROUTE_EARPIECE)
    val currentAudioRoute: StateFlow<Int> = _currentAudioRoute.asStateFlow()

    private val _supportedAudioRoutes = MutableStateFlow(CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
    val supportedAudioRoutes: StateFlow<Int> = _supportedAudioRoutes.asStateFlow()

    private val _bluetoothDeviceName = MutableStateFlow<String?>(null)
    val bluetoothDeviceName: StateFlow<String?> = _bluetoothDeviceName.asStateFlow()

    private val _automationState = MutableStateFlow<AutomationStep?>(null)
    val automationState: StateFlow<AutomationStep?> = _automationState.asStateFlow()

    private val _lastDtmfKey = MutableStateFlow<Char?>(null)
    val lastDtmfKey: StateFlow<Char?> = _lastDtmfKey.asStateFlow()

    @Volatile
    var lastInsertedCallId: Long? = null
        private set

    private var simulatedTimerJob: Job? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        this.appContext = context.applicationContext
        OngoingCallNotificationHelper.createNotificationChannel(context)
    }

    fun setTelecomService(service: TelecomCallService?) {
        this.telecomService = service
        if (service != null) {
            this.appContext = service.applicationContext
        }
    }

    fun onCallAdded(call: Call, context: Context) {
        this.nativeCall = call
        this.appContext = context.applicationContext
        val number = extractPhoneNumber(call)
        val isVoicemail = ContactHelper.isVoicemailNumber(context, number)
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val communityInfo = if (lookedUp == null && !isVoicemail) com.example.util.CommunityCallerIdService.lookup(number) else null
        val isIncoming = call.state == Call.STATE_RINGING

        val name = when {
            isVoicemail -> "Voicemail"
            lookedUp != null -> lookedUp.name
            communityInfo != null -> communityInfo.name
            !call.details?.callerDisplayName.isNullOrBlank() -> call.details!!.callerDisplayName
            isIncoming -> "Incoming Caller"
            number.isNotBlank() -> number
            else -> "Outgoing Call"
        }
        val photoUri = lookedUp?.photoUri

        val callInfo = ActiveCallInfo(
            id = call.hashCode().toString(),
            phoneNumber = number,
            displayName = name,
            state = call.state,
            isIncoming = isIncoming,
            connectTimeMillis = if (call.state == Call.STATE_ACTIVE) System.currentTimeMillis() else 0L,
            isSimulated = false,
            photoUri = photoUri,
            communityInfo = communityInfo
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
                    CallForegroundService.start(context)
                    OngoingCallNotificationHelper.showCallNotification(context, _activeCall.value!!)
                }

                if (state == Call.STATE_DISCONNECTED) {
                    handleCallEnded(context, current)
                }
            }
        })

        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, callInfo)

        // Check Do Not Disturb (DND) status
        try {
            val isFavoriteCaller = lookedUp?.isStarred == true
            if (FlipToShhhManager.isDndActive(context)) {
                val allowed = FlipToShhhManager.isCallerAllowedUnderCurrentDnd(context, isFavoriteCaller)
                if (!allowed) {
                    Log.d(TAG, "Incoming call from $number silenced by Do Not Disturb")
                    FlipToShhhManager.silenceIncomingCallIfRinging(context)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking DND policy for caller", e)
        }

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
        CallForegroundService.stop(context)
        OngoingCallNotificationHelper.cancelCallNotification(context)
        appContext?.let { ctx ->
            CallForegroundService.stop(ctx)
            OngoingCallNotificationHelper.cancelCallNotification(ctx)
        }

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
                    val insertedId = dao.insertRecentCall(
                        RecentCall(
                            phoneNumber = callInfo.phoneNumber,
                            callerName = callInfo.displayName,
                            photoUri = callInfo.photoUri,
                            callType = callType,
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = duration,
                            ruleMatched = _automationState.value?.ruleName,
                            callReason = callInfo.callReason,
                            communityTag = callInfo.communityInfo?.category
                        )
                    )
                    lastInsertedCallId = insertedId
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log recent call", e)
                }
            }
        }

        // Post-call state: keep in STATE_DISCONNECTED so InCallScreen note-taking panel can display.
        // It will be dismissed by the user or by InCallScreen's auto-close timer if not interacted with.
        val current = _activeCall.value
        if (current != null) {
            _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
        }
        _isMuted.value = false
        _isSpeakerOn.value = false
    }

    fun dismissActiveCall() {
        _activeCall.value = null
        _automationState.value = null
        _isMuted.value = false
        _isSpeakerOn.value = false
        automationJob?.cancel()
        automationJob = null
        simulatedTimerJob?.cancel()
        simulatedTimerJob = null
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
            val updated = current.copy(
                state = Call.STATE_ACTIVE,
                connectTimeMillis = System.currentTimeMillis()
            )
            _activeCall.value = updated
            appContext?.let { ctx ->
                CallForegroundService.start(ctx)
                OngoingCallNotificationHelper.showCallNotification(ctx, updated)
            }
            telecomService?.let {
                CallForegroundService.start(it)
                OngoingCallNotificationHelper.showCallNotification(it, updated)
            }
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
            appContext?.let { handleCallEnded(it, current) } ?: run {
                _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
            }
        } else {
            try {
                nativeCall?.reject(false, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting native call", e)
            }
        }
    }

    fun declineWithSms(context: Context, message: String) {
        val current = _activeCall.value
        val number = current?.phoneNumber ?: ""
        if (number.isNotBlank() && message.isNotBlank()) {
            sendSmsBackground(context, number, message)
        }
        declineCall()
    }

    fun disconnectCall() {
        val current = _activeCall.value ?: return
        if (current.isSimulated) {
            appContext?.let { handleCallEnded(it, current) } ?: run {
                _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
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

    fun onCallAudioStateChanged(audioState: CallAudioState) {
        _isMuted.value = audioState.isMuted
        _currentAudioRoute.value = audioState.route
        _supportedAudioRoutes.value = audioState.supportedRouteMask
        _isSpeakerOn.value = (audioState.route == CallAudioState.ROUTE_SPEAKER)

        val btDevice = audioState.activeBluetoothDevice
        _bluetoothDeviceName.value = btDevice?.let {
            try { it.name } catch (e: SecurityException) { "Bluetooth Device" }
        } ?: if ((audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0) "Bluetooth Device" else null
    }

    fun setAudioRoute(route: Int) {
        _currentAudioRoute.value = route
        _isSpeakerOn.value = (route == CallAudioState.ROUTE_SPEAKER)
        telecomService?.setAudioRoute(route)
        appContext?.let { ctx ->
            _activeCall.value?.let { OngoingCallNotificationHelper.showCallNotification(ctx, it) }
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        telecomService?.setMuted(newMuted)
        appContext?.let { ctx ->
            _activeCall.value?.let { OngoingCallNotificationHelper.showCallNotification(ctx, it) }
        }
    }

    fun toggleSpeaker() {
        val newRoute = if (_isSpeakerOn.value) {
            if ((_supportedAudioRoutes.value and CallAudioState.ROUTE_BLUETOOTH) != 0) {
                CallAudioState.ROUTE_BLUETOOTH
            } else {
                CallAudioState.ROUTE_EARPIECE
            }
        } else {
            CallAudioState.ROUTE_SPEAKER
        }
        setAudioRoute(newRoute)
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
    fun startSimulatedIncomingCall(context: Context, number: String, name: String, reason: String? = null) {
        appContext = context.applicationContext
        automationJob?.cancel()
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val communityInfo = if (lookedUp == null) com.example.util.CommunityCallerIdService.lookup(number) else null
        val resolvedName = lookedUp?.name ?: communityInfo?.name ?: name
        val photoUri = lookedUp?.photoUri
        val callInfo = ActiveCallInfo(
            id = "sim_${System.currentTimeMillis()}",
            phoneNumber = number,
            displayName = resolvedName,
            state = Call.STATE_RINGING,
            isIncoming = true,
            connectTimeMillis = 0L,
            isSimulated = true,
            photoUri = photoUri,
            callReason = reason ?: communityInfo?.defaultCallReason,
            communityInfo = communityInfo
        )
        _activeCall.value = callInfo
        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, callInfo)
        checkAndExecuteAutomation(context, number, true)
    }

    fun startSimulatedOutgoingCall(context: Context, number: String, reason: String? = null) {
        appContext = context.applicationContext
        automationJob?.cancel()
        val isVoicemail = ContactHelper.isVoicemailNumber(context, number)
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val communityInfo = if (lookedUp == null && !isVoicemail) com.example.util.CommunityCallerIdService.lookup(number) else null
        val resolvedName = when {
            isVoicemail -> "Voicemail"
            lookedUp != null -> lookedUp.name
            communityInfo != null -> communityInfo.name
            number.isNotBlank() -> number
            else -> "Outgoing Call"
        }
        val photoUri = lookedUp?.photoUri
        val callInfo = ActiveCallInfo(
            id = "sim_out_${System.currentTimeMillis()}",
            phoneNumber = number,
            displayName = resolvedName,
            state = Call.STATE_DIALING,
            isIncoming = false,
            connectTimeMillis = 0L,
            isSimulated = true,
            photoUri = photoUri,
            callReason = reason,
            communityInfo = communityInfo
        )
        _activeCall.value = callInfo
        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, callInfo)

        scope.launch {
            delay(1500)
            val current = _activeCall.value
            if (current != null && current.state == Call.STATE_DIALING) {
                val updated = current.copy(
                    state = Call.STATE_ACTIVE,
                    displayName = resolvedName,
                    connectTimeMillis = System.currentTimeMillis()
                )
                _activeCall.value = updated
                CallForegroundService.start(context)
                OngoingCallNotificationHelper.showCallNotification(context, updated)
            }
        }
    }

    private fun normalizePhoneNumber(raw: String): String {
        return raw.filter { it.isDigit() }
    }
}
