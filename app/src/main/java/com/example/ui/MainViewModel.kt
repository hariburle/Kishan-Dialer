package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.TelecomManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AutomationLog
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.data.IgnoredContact
import com.example.data.RecentCall
import com.example.telecom.ActiveCallInfo
import com.example.telecom.AutomationStep
import com.example.telecom.CallManager
import com.example.telecom.RoleHelper
import com.example.telecom.SimHelper
import com.example.telecom.SimInfo
import com.example.util.ContactHelper
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CloudContactConfirmation(
    val title: String,
    val message: String,
    val contactName: String,
    val confirmButtonText: String = "Update Google Contacts",
    val secondaryButtonText: String? = null,
    val dismissButtonText: String = "Cancel",
    val onConfirmCloudAction: () -> Unit,
    val onSecondaryAction: (() -> Unit)? = null,
    val onDismissOrCancel: () -> Unit = {}
)

data class CallMethodChoicePrompt(
    val number: String,
    val contactName: String?,
    val reason: String? = null,
    val isLearnMode: Boolean = false
)

class MainViewModel(
    private val repository: AppRepository,
    private val appContext: Context
) : ViewModel() {

    // Preferences for Theme and WhatsApp calls
    private val prefs = appContext.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    private val _whatsAppCallMode = MutableStateFlow(prefs.getString("whatsapp_call_mode", "ask_learn") ?: "ask_learn")
    val whatsAppCallMode: StateFlow<String> = _whatsAppCallMode.asStateFlow()

    fun setWhatsAppCallMode(mode: String) {
        _whatsAppCallMode.value = mode
        prefs.edit().putString("whatsapp_call_mode", mode).apply()
    }

    // Learned Calling Choices for Contacts (Map of normalized number -> "cellular" | "whatsapp")
    private val _learnedCallModes = MutableStateFlow<Map<String, String>>(loadLearnedCallModes())
    val learnedCallModes: StateFlow<Map<String, String>> = _learnedCallModes.asStateFlow()

    private fun loadLearnedCallModes(): Map<String, String> {
        val rawSet = prefs.getStringSet("whatsapp_learned_choices", emptySet()) ?: emptySet()
        val map = mutableMapOf<String, String>()
        rawSet.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    fun saveLearnedCallMode(phoneNumber: String, mode: String) {
        val digits = phoneNumber.filter { it.isDigit() }.takeLast(10)
        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (digits.isBlank() && clean.isBlank()) return
        val current = _learnedCallModes.value.toMutableMap()
        if (digits.isNotBlank()) current[digits] = mode
        if (clean.isNotBlank()) current[clean] = mode
        _learnedCallModes.value = current

        val set = current.map { "${it.key}:${it.value}" }.toSet()
        prefs.edit().putStringSet("whatsapp_learned_choices", HashSet(set)).apply()
    }

    fun resetWhatsAppChoices() {
        _learnedCallModes.value = emptyMap()
        prefs.edit().remove("whatsapp_learned_choices").apply()
    }

    // Call method selection dialog state
    private val _pendingCallMethodChoice = MutableStateFlow<CallMethodChoicePrompt?>(null)
    val pendingCallMethodChoice: StateFlow<CallMethodChoicePrompt?> = _pendingCallMethodChoice.asStateFlow()

    fun dismissCallMethodChoice() {
        _pendingCallMethodChoice.value = null
    }

    fun chooseCallMethod(context: Context, method: String, remember: Boolean) {
        val prompt = _pendingCallMethodChoice.value ?: return
        _pendingCallMethodChoice.value = null

        if (remember || _whatsAppCallMode.value == "ask_learn") {
            saveLearnedCallMode(prompt.number, method)
        }

        if (method == "whatsapp") {
            placeWhatsAppCall(context, prompt.number)
        } else {
            placeCall(context, prompt.number, prompt.reason)
        }
    }

    // Explicit Not-Spam Whitelist (numbers explicitly unmarked as spam)
    private val _notSpamWhitelist = MutableStateFlow<Set<String>>(
        prefs.getStringSet("not_spam_whitelist", emptySet()) ?: emptySet()
    )
    val notSpamWhitelist: StateFlow<Set<String>> = _notSpamWhitelist.asStateFlow()

    // Accidental touch protection: Ask confirmation before calling favorites
    private val _confirmFavoritesCall = MutableStateFlow(prefs.getBoolean("confirm_fav_calls", true))
    val confirmFavoritesCall: StateFlow<Boolean> = _confirmFavoritesCall.asStateFlow()

    fun setConfirmFavoritesCall(enabled: Boolean) {
        _confirmFavoritesCall.value = enabled
        prefs.edit().putBoolean("confirm_fav_calls", enabled).apply()
    }

    // Default start tab: 2 (Keypad) to prevent accidental calls when opening app
    private val _defaultStartTab = MutableStateFlow(prefs.getInt("default_start_tab", 2))
    val defaultStartTab: StateFlow<Int> = _defaultStartTab.asStateFlow()

    fun setDefaultStartTab(tabIndex: Int) {
        _defaultStartTab.value = tabIndex
        prefs.edit().putInt("default_start_tab", tabIndex).apply()
    }

    fun isNumberWhitelistedNotSpam(phoneNumber: String): Boolean {
        val clean = phoneNumber.filter { it.isDigit() }.takeLast(10)
        return _notSpamWhitelist.value.any { wl ->
            wl == phoneNumber || (clean.length >= 7 && wl.filter { it.isDigit() }.takeLast(10) == clean)
        }
    }

    fun isSpamNumber(phoneNumber: String): Boolean {
        if (isNumberWhitelistedNotSpam(phoneNumber)) return false
        val clean = phoneNumber.filter { it.isDigit() }.takeLast(10)
        return spamNumbers.value.any { sp ->
            val spClean = sp.phoneNumber.filter { it.isDigit() }.takeLast(10)
            sp.phoneNumber == phoneNumber || (clean.length >= 7 && spClean == clean)
        }
    }

    // Dialer Input
    private val _dialerNumber = MutableStateFlow("")
    val dialerNumber: StateFlow<String> = _dialerNumber.asStateFlow()

    // Default Dialer Status
    private val _isDefaultDialer = MutableStateFlow(RoleHelper.isDefaultDialer(appContext))
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    // Confirmation dialog before any changes to Google Account Contacts
    private val _pendingCloudConfirmation = MutableStateFlow<CloudContactConfirmation?>(null)
    val pendingCloudConfirmation: StateFlow<CloudContactConfirmation?> = _pendingCloudConfirmation.asStateFlow()

    fun clearCloudConfirmation() {
        _pendingCloudConfirmation.value = null
    }

    // Device Contacts Flow & Observer for live synchronization with system contacts app
    private val _deviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContact>> = _deviceContacts.asStateFlow()

    private var contactsObserver: android.database.ContentObserver? = null

    // Active Call forwarded from CallManager
    val activeCall: StateFlow<ActiveCallInfo?> = CallManager.activeCall
    val isMuted: StateFlow<Boolean> = CallManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = CallManager.isSpeakerOn
    val automationState: StateFlow<AutomationStep?> = CallManager.automationState
    val lastDtmfKey: StateFlow<Char?> = CallManager.lastDtmfKey

    // In-Call keypad toggle state
    private val _showInCallKeypad = MutableStateFlow(false)
    val showInCallKeypad: StateFlow<Boolean> = _showInCallKeypad.asStateFlow()

    // Call minimization state (so user can browse the app during active call)
    private val _isCallScreenMinimized = MutableStateFlow(false)
    val isCallScreenMinimized: StateFlow<Boolean> = _isCallScreenMinimized.asStateFlow()

    fun minimizeCall() {
        _isCallScreenMinimized.value = true
    }

    fun maximizeCall() {
        _isCallScreenMinimized.value = false
    }

    // Flip to Shhh (DND status)
    val isFlipToShhhEnabled: StateFlow<Boolean> = com.example.telecom.FlipToShhhManager.isFlipToShhhEnabled
    val isShhhActive: StateFlow<Boolean> = com.example.telecom.FlipToShhhManager.isShhhActive

    fun toggleFlipToShhh() {
        com.example.telecom.FlipToShhhManager.setEnabled(appContext, !isFlipToShhhEnabled.value)
    }

    // Dual-SIM State (Slot Index + 1, e.g., 1 or 2)
    private val _selectedSimSlot = MutableStateFlow(1)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    // Real SIM cards read from device's SubscriptionManager
    private val _activeSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val activeSims: StateFlow<List<SimInfo>> = _activeSims.asStateFlow()

    // Database Flows
    val rules: StateFlow<List<CallerRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCalls: StateFlow<List<RecentCall>> = repository.recentCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteContact>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spamNumbers: StateFlow<List<com.example.data.SpamNumber>> = repository.spamNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ignoredContacts: StateFlow<List<IgnoredContact>> = repository.ignoredContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automationLogs: StateFlow<List<AutomationLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshContacts()
        refreshSimCards()
        registerContactsObserver()
    }

    fun refreshContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = ContactHelper.fetchDeviceContacts(appContext)
                _deviceContacts.value = list
                syncWithDeviceContacts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun registerContactsObserver() {
        try {
            contactsObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    refreshContacts()
                }
            }
            appContext.contentResolver.registerContentObserver(
                android.provider.ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        contactsObserver?.let {
            try {
                appContext.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
    }

    /**
     * Synchronizes favorites with Android device Contacts database as the single source of truth.
     * Uses logical deduplication (matching phone digits or name) to identify existing contacts/favorites.
     * Keeps Room database clean and unique without creating duplicate entries on launch or re-install.
     */
    fun syncWithDeviceContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
                fun normName(name: String): String = name.trim().lowercase()

                val currentDbFavorites = repository.getAllFavoritesList()

                // Deduplicate any existing duplicate favorites in Room (one card per contact person)
                val seenNames = mutableMapOf<String, com.example.data.FavoriteContact>()
                for (fav in currentDbFavorites) {
                    val key = normName(fav.name)
                    val existing = seenNames[key]
                    if (existing == null) {
                        seenNames[key] = fav
                    } else {
                        // Duplicate found for the same contact person: retain the one with speed dial slot, or first one
                        if (fav.speedDialSlot != null && existing.speedDialSlot == null) {
                            repository.deleteFavorite(existing)
                            seenNames[key] = fav
                        } else {
                            repository.deleteFavorite(fav)
                        }
                    }
                }

                val activeDbFavorites = repository.getAllFavoritesList()
                val activeByName = activeDbFavorites.associateBy { normName(it.name) }.toMutableMap()

                // Starred device contacts: returns at most one entry per contact holding their default number
                val starredOnDevice = ContactHelper.fetchStarredContacts(appContext)
                val colors = listOf(0xFF2563EBL, 0xFF16A34AL, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL, 0xFF0891B2L)
                var maxOrder = activeDbFavorites.maxOfOrNull { it.sortOrder } ?: -1

                // Merge starred device contacts without duplicating the contact
                for (deviceContact in starredOnDevice) {
                    val nameKey = normName(deviceContact.name)
                    val existing = activeByName[nameKey] ?: activeByName.values.firstOrNull {
                        val d1 = normDigits(it.phoneNumber)
                        val d2 = normDigits(deviceContact.phoneNumber)
                        d1.length >= 7 && d1 == d2
                    }

                    if (existing != null) {
                        // Keep the user's chosen favorite phoneNumber intact; only update display name or photo
                        if (existing.name != deviceContact.name ||
                            existing.photoUri != deviceContact.photoUri) {
                            val updated = existing.copy(
                                name = deviceContact.name,
                                nickname = deviceContact.nickname ?: existing.nickname,
                                photoUri = deviceContact.photoUri ?: existing.photoUri
                            )
                            repository.updateFavorite(updated)
                            activeByName[nameKey] = updated
                        }
                    } else {
                        // Genuinely new contact
                        maxOrder++
                        val color = colors[kotlin.math.abs(deviceContact.name.hashCode()) % colors.size]
                        val newFav = FavoriteContact(
                            name = deviceContact.name,
                            nickname = deviceContact.nickname,
                            phoneNumber = deviceContact.phoneNumber,
                            label = deviceContact.label,
                            avatarColor = color,
                            photoUri = deviceContact.photoUri,
                            sortOrder = maxOrder
                        )
                        val insertedId = repository.insertFavorite(newFav)
                        activeByName[nameKey] = newFav.copy(id = insertedId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshSimCards() {
        viewModelScope.launch(Dispatchers.IO) {
            val detected = SimHelper.getActiveSimCards(appContext)
            _activeSims.value = detected
            if (detected.isNotEmpty()) {
                val currentSlotValid = detected.any { it.slotIndex + 1 == _selectedSimSlot.value }
                if (!currentSlotValid) {
                    val defaultSim = detected.firstOrNull { it.isDefault } ?: detected.first()
                    _selectedSimSlot.value = defaultSim.slotIndex + 1
                }
            }
        }
    }

    fun selectSimSlot(slot: Int) {
        _selectedSimSlot.value = slot
    }

    fun toggleSimSlot() {
        val detected = _activeSims.value
        if (detected.size > 1) {
            val currentIdx = detected.indexOfFirst { it.slotIndex + 1 == _selectedSimSlot.value }
            val nextIdx = (if (currentIdx >= 0) currentIdx + 1 else 0) % detected.size
            _selectedSimSlot.value = detected[nextIdx].slotIndex + 1
        } else {
            _selectedSimSlot.value = if (_selectedSimSlot.value == 1) 2 else 1
        }
    }

    fun refreshDefaultDialerStatus() {
        _isDefaultDialer.value = RoleHelper.isDefaultDialer(appContext)
    }

    fun appendDigit(digit: Char) {
        _dialerNumber.value += digit
    }

    fun deleteLastDigit() {
        if (_dialerNumber.value.isNotEmpty()) {
            _dialerNumber.value = _dialerNumber.value.dropLast(1)
        }
    }

    fun clearDigits() {
        _dialerNumber.value = ""
    }

    fun setDialerNumber(number: String) {
        _dialerNumber.value = number
    }

    fun toggleInCallKeypad() {
        _showInCallKeypad.value = !_showInCallKeypad.value
    }

    private val _selectedCallReason = MutableStateFlow<String?>(null)
    val selectedCallReason: StateFlow<String?> = _selectedCallReason.asStateFlow()

    fun selectCallReason(reason: String?) {
        _selectedCallReason.value = reason
    }

    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, number: String, reason: String? = null) {
        val cleanNumber = number.ifBlank { _dialerNumber.value }
        if (cleanNumber.isBlank()) return
        val effectiveReason = reason ?: _selectedCallReason.value
        maximizeCall()

        // If in ask_learn mode and user explicitly triggered cellular call, learn the choice directly
        if (_whatsAppCallMode.value == "ask_learn") {
            saveLearnedCallMode(cleanNumber, "cellular")
        }

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val uri = Uri.fromParts("tel", cleanNumber, null)

            val extras = Bundle().apply {
                putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                if (!effectiveReason.isNullOrBlank()) {
                    putString("CALL_REASON", effectiveReason)
                }
            }

            // Ensure Android routes to cellular SIM carrier, using the selected SIM slot
            if (telecomManager != null) {
                try {
                    val simAccount = SimHelper.getPhoneAccountForSimSlot(context, _selectedSimSlot.value - 1)
                    val defaultAccount = simAccount
                        ?: telecomManager.getDefaultOutgoingPhoneAccount(uri.scheme)
                        ?: telecomManager.callCapablePhoneAccounts.firstOrNull { handle ->
                            handle.componentName.packageName.contains("telephony", ignoreCase = true) ||
                            handle.componentName.packageName.contains("phone", ignoreCase = true)
                        } ?: telecomManager.callCapablePhoneAccounts.firstOrNull()

                    if (defaultAccount != null) {
                        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, defaultAccount)
                    }
                } catch (_: SecurityException) {
                    // Ignore if permission not yet granted
                }
            }

            // If running on actual device/dialer role
            if (telecomManager != null && RoleHelper.isDefaultDialer(context)) {
                telecomManager.placeCall(uri, extras)
            } else {
                // Fallback to ACTION_CALL or start simulated outgoing call for preview
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtras(extras)
                }
                if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    context.startActivity(callIntent)
                } else {
                    // Start simulated call so user can test the in-call interface & DTMF keypad
                    CallManager.startSimulatedOutgoingCall(context, cleanNumber, effectiveReason)
                }
            }
        } catch (e: Exception) {
            // Simulator fallback if hardware telephony is unavailable
            CallManager.startSimulatedOutgoingCall(context, cleanNumber, effectiveReason)
        }
    }

    fun placeWhatsAppCall(context: Context, number: String) {
        val cleanNumber = number.ifBlank { _dialerNumber.value }
        if (cleanNumber.isBlank()) return

        // If in ask_learn mode and user explicitly triggered WhatsApp call, learn the choice directly
        if (_whatsAppCallMode.value == "ask_learn") {
            saveLearnedCallMode(cleanNumber, "whatsapp")
        }

        ContactHelper.launchWhatsAppCall(context, cleanNumber)

        // Log outgoing WhatsApp call so frequency learning & preferred calling mode work
        viewModelScope.launch(Dispatchers.IO) {
            val contactName = _deviceContacts.value.firstOrNull { dc ->
                dc.phoneNumber.contains(cleanNumber) || dc.phoneNumbers.any { it.number.contains(cleanNumber) }
            }?.name ?: favorites.value.firstOrNull { it.phoneNumber.contains(cleanNumber) }?.name

            repository.insertRecentCall(
                RecentCall(
                    phoneNumber = cleanNumber,
                    callerName = contactName ?: cleanNumber,
                    callType = android.provider.CallLog.Calls.OUTGOING_TYPE,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 0,
                    callReason = "WhatsApp Call"
                )
            )
        }
    }

    /**
     * Determines whether cellular or WhatsApp calling is preferred for this contact/number,
     * checking explicitly learned choices, international rules, or recent call history.
     */
    fun getPreferredCallingMode(phoneNumber: String): String {
        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        val digits = clean.filter { it.isDigit() }.takeLast(10)

        // 1. Check explicitly learned choice
        val learned = _learnedCallModes.value[clean]
            ?: if (digits.isNotBlank()) _learnedCallModes.value[digits] else null
        if (learned != null) return learned

        // 2. If user configured all international to WhatsApp, check international based on phone location
        if (_whatsAppCallMode.value == "all_international" && ContactHelper.isInternationalNumber(appContext, clean)) {
            return "whatsapp"
        }

        // 3. Count past WhatsApp calls vs regular cellular calls in recent calls
        val calls = recentCalls.value.filter { call ->
            val callDigits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
            call.phoneNumber == clean || (digits.length >= 7 && callDigits == digits)
        }
        val waCount = calls.count { it.callReason?.contains("WhatsApp", ignoreCase = true) == true }
        val gsmCount = calls.count { it.callReason?.contains("WhatsApp", ignoreCase = true) != true }

        return if (waCount > gsmCount && waCount > 0) {
            "whatsapp"
        } else {
            "cellular"
        }
    }

    fun lookupContactByNumber(phoneNumber: String): DeviceContact? {
        val fav = favorites.value.firstOrNull {
            ContactHelper.matchesNumberQuery(it.phoneNumber, phoneNumber)
        }
        if (fav != null) return DeviceContact(fav.name, fav.phoneNumber, fav.label, fav.photoUri)
        return deviceContacts.value.firstOrNull { dc ->
            ContactHelper.matchesNumberQuery(dc.phoneNumber, phoneNumber) ||
            dc.phoneNumbers.any { ContactHelper.matchesNumberQuery(it.number, phoneNumber) }
        }
    }

    /**
     * Places a call honoring the configured WhatsApp calling mode (All International,
     * Ask Always prompt, Ask & Learn memory, or cellular).
     */
    fun initiateCall(context: Context, number: String, reason: String? = null) {
        val cleanNumber = number.ifBlank { _dialerNumber.value }
        if (cleanNumber.isBlank()) return

        val isInternational = ContactHelper.isInternationalNumber(context, cleanNumber)
        val mode = _whatsAppCallMode.value

        // 1. All International Mode
        if (mode == "all_international" && isInternational) {
            placeWhatsAppCall(context, cleanNumber)
            return
        }

        // 2. Ask Always Mode
        if (mode == "ask_always") {
            val contact = lookupContactByNumber(cleanNumber)
            _pendingCallMethodChoice.value = CallMethodChoicePrompt(
                number = cleanNumber,
                contactName = contact?.name,
                reason = reason,
                isLearnMode = false
            )
            return
        }

        // 3. Ask & Learn Mode
        if (mode == "ask_learn") {
            val clean = cleanNumber.replace(Regex("[^0-9+]"), "")
            val digits = cleanNumber.filter { it.isDigit() }.takeLast(10)
            val learnedChoice = _learnedCallModes.value[clean]
                ?: if (digits.isNotBlank()) _learnedCallModes.value[digits] else null
            if (learnedChoice != null) {
                if (learnedChoice == "whatsapp") {
                    placeWhatsAppCall(context, cleanNumber)
                } else {
                    placeCall(context, cleanNumber, reason)
                }
                return
            }

            // Not yet learned: show prompt so user can choose and learn
            val contact = lookupContactByNumber(cleanNumber)
            _pendingCallMethodChoice.value = CallMethodChoicePrompt(
                number = cleanNumber,
                contactName = contact?.name,
                reason = reason,
                isLearnMode = true
            )
            return
        }

        // 4. Default / Never
        placeCall(context, cleanNumber, reason)
    }

    fun simulateIncomingCall(context: Context, number: String, name: String = "Incoming Caller", reason: String? = null) {
        CallManager.startSimulatedIncomingCall(context, number, name, reason)
    }

    fun answerCall() {
        CallManager.answerCall()
    }

    fun declineCall() {
        CallManager.declineCall()
    }

    fun declineWithSms(message: String) {
        CallManager.declineWithSms(appContext, message)
    }

    fun updateRecentCallNoteAndReminder(recentCall: RecentCall, note: String?, reminderTime: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = recentCall.copy(
                note = note?.trim()?.takeIf { it.isNotBlank() },
                reminderTime = reminderTime
            )
            repository.updateRecentCall(updated)
        }
    }

    fun deleteRecentCall(call: RecentCall) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecentCall(call)
        }
    }

    fun deleteRecentCallsForNumber(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecentCallsForNumber(phoneNumber)
        }
    }

    fun disconnectCall() {
        CallManager.disconnectCall()
    }

    fun dismissCall() {
        CallManager.dismissActiveCall()
    }

    fun playDtmf(digit: Char) {
        CallManager.playDtmf(digit)
    }

    fun stopDtmf() {
        CallManager.stopDtmf()
    }

    fun toggleMute() {
        CallManager.toggleMute()
    }

    fun toggleSpeaker() {
        CallManager.toggleSpeaker()
    }

    val currentAudioRoute: StateFlow<Int> = CallManager.currentAudioRoute
    val supportedAudioRoutes: StateFlow<Int> = CallManager.supportedAudioRoutes
    val bluetoothDeviceName: StateFlow<String?> = CallManager.bluetoothDeviceName

    fun setAudioRoute(route: Int) {
        CallManager.setAudioRoute(route)
    }

    fun savePostCallNote(phoneNumber: String, note: String?, reminderTime: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }
            val lastId = CallManager.lastInsertedCallId
            val existing = if (lastId != null) {
                repository.getLatestRecentCallForNumber(phoneNumber)?.takeIf { it.id == lastId }
                    ?: repository.getLatestRecentCallForNumber(phoneNumber)
            } else {
                repository.getLatestRecentCallForNumber(phoneNumber)
            }

            if (existing != null) {
                repository.updateRecentCall(
                    existing.copy(
                        note = cleanNote,
                        reminderTime = reminderTime
                    )
                )
            } else {
                repository.insertRecentCall(
                    RecentCall(
                        phoneNumber = phoneNumber,
                        callerName = phoneNumber,
                        callType = 2,
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = 0,
                        note = cleanNote,
                        reminderTime = reminderTime
                    )
                )
            }
        }
    }

    fun createNewContact(name: String, phoneNumber: String, label: String, saveToDevice: Boolean, addToFavorites: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (saveToDevice) {
                ContactHelper.saveContactToDevice(appContext, name, phoneNumber, label)
            }
            if (addToFavorites || !saveToDevice) {
                val fav = FavoriteContact(
                    name = name,
                    phoneNumber = phoneNumber,
                    label = label,
                    sortOrder = 999
                )
                repository.insertFavorite(fav)
            }
        }
    }

    fun syncAppContactToGoogle(contact: DeviceContact) {
        viewModelScope.launch(Dispatchers.IO) {
            ContactHelper.saveContactToDevice(appContext, contact.name, contact.phoneNumber, contact.label)
        }
    }

    fun saveRule(rule: CallerRule) {
        viewModelScope.launch {
            if (rule.id == 0L) {
                repository.insertRule(rule)
            } else {
                repository.updateRule(rule)
            }
        }
    }

    fun toggleRuleEnabled(rule: CallerRule) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteRule(rule: CallerRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAutomationLogs()
        }
    }

    fun addFavorite(name: String, phoneNumber: String, label: String = "Mobile", photoUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
            val cleanDigits = normDigits(phoneNumber)
            val currentList = repository.getAllFavoritesList()
            val existing = currentList.firstOrNull {
                (cleanDigits.length >= 7 && normDigits(it.phoneNumber) == cleanDigits) ||
                it.name.equals(name.trim(), ignoreCase = true)
            }
            if (existing != null) {
                // Update existing rather than creating duplicate
                repository.updateFavorite(
                    existing.copy(
                        name = name.trim(),
                        phoneNumber = phoneNumber.trim(),
                        label = label,
                        photoUri = photoUri ?: existing.photoUri
                    )
                )
                return@launch
            }

            val colors = listOf(0xFF2563EBL, 0xFF16A34AL, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL, 0xFF0891B2L)
            val color = colors[kotlin.math.abs(name.hashCode()) % colors.size]
            val maxOrder = currentList.maxOfOrNull { it.sortOrder } ?: -1
            repository.insertFavorite(
                com.example.data.FavoriteContact(
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    label = label,
                    avatarColor = color,
                    photoUri = photoUri,
                    sortOrder = maxOrder + 1
                )
            )
        }

        // Ask explicit user confirmation before starring in Google Account Contacts in the cloud
        _pendingCloudConfirmation.value = CloudContactConfirmation(
            title = "Star in Google Account Contacts?",
            message = "Added '$name' to favorites in this app.\n\nWould you like to also star this contact in your Google Account Contacts in the cloud?",
            contactName = name,
            confirmButtonText = "Star in Google Contacts",
            secondaryButtonText = null,
            dismissButtonText = "Keep in App Only",
            onConfirmCloudAction = {
                viewModelScope.launch(Dispatchers.IO) {
                    ContactHelper.setContactStarred(appContext, phoneNumber, true)
                }
            },
            onSecondaryAction = null,
            onDismissOrCancel = {
                // Kept in app only; Google Account Contacts remains unchanged
            }
        )
    }

    fun updateFavorite(contact: com.example.data.FavoriteContact, newNickname: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = contact.copy(
                name = contact.name.trim(),
                nickname = newNickname?.trim()?.takeIf { it.isNotBlank() } ?: contact.nickname
            )
            repository.updateFavorite(updated)
        }

        // Only update nickname in cloud Google Contacts if explicitly confirmed
        if (!newNickname.isNullOrBlank()) {
            _pendingCloudConfirmation.value = CloudContactConfirmation(
                title = "Update Google Contacts Nickname?",
                message = "You set a custom nickname '$newNickname' for ${contact.name}.\n\nDo you want to update this nickname in your Google Account Contacts in the cloud as well?",
                contactName = contact.name,
                confirmButtonText = "Update Google Contacts",
                secondaryButtonText = null,
                dismissButtonText = "Save in App Only",
                onConfirmCloudAction = {
                    viewModelScope.launch(Dispatchers.IO) {
                        ContactHelper.updateContactNickname(appContext, contact.phoneNumber, newNickname.trim())
                    }
                },
                onSecondaryAction = null,
                onDismissOrCancel = {
                    // Saved in app only
                }
            )
        }
    }

    fun updateFavoritePhoneNumber(contact: com.example.data.FavoriteContact, newPhoneNumber: String, newLabel: String = "Mobile") {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = contact.copy(
                phoneNumber = newPhoneNumber.trim(),
                label = newLabel
            )
            repository.updateFavorite(updated)
        }
    }

    fun reorderFavorites(newOrderedList: List<com.example.data.FavoriteContact>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = newOrderedList.mapIndexed { index, item ->
                item.copy(sortOrder = index)
            }
            repository.updateFavorites(updated)
        }
    }

    fun moveFavorite(fromIndex: Int, toIndex: Int) {
        val current = favorites.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            reorderFavorites(current)
        }
    }

    fun toggleFavorite(name: String, phoneNumber: String, label: String = "Mobile", photoUri: String? = null) {
        val cleanDigits = phoneNumber.filter { it.isDigit() }.takeLast(10)
        val existing = favorites.value.firstOrNull { fav ->
            val favDigits = fav.phoneNumber.filter { it.isDigit() }.takeLast(10)
            (cleanDigits.length >= 7 && favDigits == cleanDigits) ||
            fav.name.equals(name.trim(), ignoreCase = true)
        }
        if (existing != null) {
            // Confirm before removing favorite or modifying Google Contacts
            _pendingCloudConfirmation.value = CloudContactConfirmation(
                title = "Remove '$name' from Favorites?",
                message = "Do you want to remove '$name' from your favorites?\n\nWould you like to also unfavorite/unstar this contact in your Google Account Contacts in the cloud, or only remove it from this app?",
                contactName = name,
                confirmButtonText = "Remove & Unstar in Google",
                secondaryButtonText = "Remove from App Only",
                dismissButtonText = "Cancel",
                onConfirmCloudAction = {
                    viewModelScope.launch(Dispatchers.IO) {
                        ContactHelper.setContactStarred(appContext, existing.phoneNumber, false)
                        repository.deleteFavorite(existing)
                    }
                },
                onSecondaryAction = {
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.deleteFavorite(existing)
                    }
                },
                onDismissOrCancel = {
                    // Cancelled, do not remove
                }
            )
        } else {
            addFavorite(name, phoneNumber, label, photoUri)
        }
    }

    fun deleteFavorite(contact: com.example.data.FavoriteContact) {
        // Prompt for confirmation before removing or modifying Google Contacts
        _pendingCloudConfirmation.value = CloudContactConfirmation(
            title = "Remove '${contact.name}' from Favorites?",
            message = "Do you want to remove '${contact.name}' from your favorites?\n\nWould you like to also unfavorite/unstar this contact in your Google Account Contacts in the cloud, or only remove it from this app?",
            contactName = contact.name,
            confirmButtonText = "Remove & Unstar in Google",
            secondaryButtonText = "Remove from App Only",
            dismissButtonText = "Cancel",
            onConfirmCloudAction = {
                viewModelScope.launch(Dispatchers.IO) {
                    ContactHelper.setContactStarred(appContext, contact.phoneNumber, false)
                    repository.deleteFavorite(contact)
                }
            },
            onSecondaryAction = {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.deleteFavorite(contact)
                }
            },
            onDismissOrCancel = {
                // Cancelled, do not remove
            }
        )
    }

    fun markAsSpam(phoneNumber: String, label: String = "Reported Spam") {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanDigits = phoneNumber.filter { it.isDigit() }.takeLast(10)

            // Remove from whitelist if previously whitelisted
            val currentWl = _notSpamWhitelist.value.toMutableSet()
            val removedWl = currentWl.removeAll {
                it == phoneNumber || (cleanDigits.length >= 7 && it.filter { c -> c.isDigit() }.takeLast(10) == cleanDigits)
            }
            if (removedWl) {
                _notSpamWhitelist.value = currentWl
                prefs.edit().putStringSet("not_spam_whitelist", currentWl).apply()
            }

            repository.insertSpamNumber(
                com.example.data.SpamNumber(
                    phoneNumber = phoneNumber,
                    label = label,
                    reportCount = 1,
                    isBlocked = true
                )
            )
            repository.updateRecentCallSpamStatus(phoneNumber, true)
            // Also update any recent calls matching normalized digits
            val allCalls = repository.getAllRecentCallsList()
            for (call in allCalls) {
                val callDigits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
                if (call.phoneNumber == phoneNumber || (callDigits.length >= 7 && callDigits == cleanDigits)) {
                    if (!call.isSpam) {
                        repository.updateRecentCall(call.copy(isSpam = true))
                    }
                }
            }
        }
    }

    fun removeSpam(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanDigits = phoneNumber.filter { it.isDigit() }.takeLast(10)

            // Permanently add to not-spam whitelist
            val currentWl = _notSpamWhitelist.value.toMutableSet()
            currentWl.add(phoneNumber)
            if (cleanDigits.isNotBlank()) currentWl.add(cleanDigits)
            _notSpamWhitelist.value = currentWl
            prefs.edit().putStringSet("not_spam_whitelist", currentWl).apply()

            // Delete exact number match
            repository.deleteSpamByNumber(phoneNumber)
            // Also delete any entries matching normalized digits
            val allSpam = repository.getAllSpamNumbersList()
            for (sp in allSpam) {
                val spDigits = sp.phoneNumber.filter { it.isDigit() }.takeLast(10)
                if (sp.phoneNumber == phoneNumber || (spDigits.length >= 7 && spDigits == cleanDigits)) {
                    repository.deleteSpamNumber(sp)
                }
            }
            // Update recent_calls table to mark isSpam = false
            repository.updateRecentCallSpamStatus(phoneNumber, false)
            val allCalls = repository.getAllRecentCallsList()
            for (call in allCalls) {
                val callDigits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
                if (call.phoneNumber == phoneNumber || (callDigits.length >= 7 && callDigits == cleanDigits)) {
                    if (call.isSpam) {
                        repository.updateRecentCall(call.copy(isSpam = false))
                    }
                }
            }
        }
    }

    fun ignorePopularContact(phoneNumber: String, name: String, category: String, tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertIgnoredContact(
                IgnoredContact(
                    phoneNumber = phoneNumber,
                    name = name,
                    category = category,
                    tag = tag,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun unignorePopularContact(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteIgnoredContactByNumber(phoneNumber)
        }
    }

    fun updateIgnoredContactTag(phoneNumber: String, newTag: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getIgnoredContactByNumber(phoneNumber)
            if (existing != null) {
                repository.insertIgnoredContact(
                    existing.copy(tag = newTag, name = newName)
                )
            } else {
                repository.insertIgnoredContact(
                    IgnoredContact(phoneNumber = phoneNumber, name = newName, tag = newTag)
                )
            }
        }
    }

    fun assignSpeedDial(contact: com.example.data.FavoriteContact, slot: Int) {
        viewModelScope.launch {
            // If contact already has this slot, toggle it off (unassign)
            if (contact.speedDialSlot == slot) {
                repository.updateFavorite(contact.copy(speedDialSlot = null))
                return@launch
            }
            // Clear this slot from any other favorite contact that has it
            val currentFavorites = favorites.value
            currentFavorites.filter { it.speedDialSlot == slot && it.id != contact.id }.forEach { other ->
                repository.updateFavorite(other.copy(speedDialSlot = null))
            }
            // Assign slot to target contact
            repository.updateFavorite(contact.copy(speedDialSlot = slot))
        }
    }

    fun assignSpeedDialSlot(slot: Int, name: String, phoneNumber: String, photoUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFavorites = repository.getAllFavoritesList()
            // Clear this slot from any other contact
            currentFavorites.filter { it.speedDialSlot == slot }.forEach { other ->
                repository.updateFavorite(other.copy(speedDialSlot = null))
            }

            val cleanDigits = phoneNumber.filter { it.isDigit() }.takeLast(10)
            val existing = currentFavorites.firstOrNull {
                it.phoneNumber == phoneNumber || (cleanDigits.length >= 7 && it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == cleanDigits) || it.name.equals(name, ignoreCase = true)
            }

            if (existing != null) {
                repository.updateFavorite(existing.copy(speedDialSlot = slot, photoUri = photoUri ?: existing.photoUri))
            } else {
                val colors = listOf(0xFF2563EBL, 0xFF16A34AL, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL, 0xFF0891B2L)
                val color = colors[kotlin.math.abs(name.hashCode()) % colors.size]
                val maxOrder = currentFavorites.maxOfOrNull { it.sortOrder } ?: -1
                repository.insertFavorite(
                    com.example.data.FavoriteContact(
                        name = name.trim(),
                        phoneNumber = phoneNumber.trim(),
                        label = "Mobile",
                        avatarColor = color,
                        photoUri = photoUri,
                        speedDialSlot = slot,
                        sortOrder = maxOrder + 1
                    )
                )
            }
        }
    }

    fun clearSpeedDialSlot(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFavorites = repository.getAllFavoritesList()
            currentFavorites.filter { it.speedDialSlot == slot }.forEach { fav ->
                repository.updateFavorite(fav.copy(speedDialSlot = null))
            }
        }
    }

    fun updateContact(oldNumber: String, newName: String, newNumber: String, newLabel: String, newNickname: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            ContactHelper.updateContactDetails(appContext, oldNumber, newName, newNumber, newLabel, newNickname)
            val fav = favorites.value.firstOrNull { it.phoneNumber == oldNumber }
            if (fav != null) {
                repository.updateFavorite(fav.copy(name = newName, phoneNumber = newNumber, label = newLabel))
            }
            syncWithDeviceContacts()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val repo = AppRepository(db.appDao())
                    return MainViewModel(repo, context.applicationContext) as T
                }
            }
    }
}
