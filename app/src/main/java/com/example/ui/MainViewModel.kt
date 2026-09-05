package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.example.data.RecentCall
import com.example.telecom.ActiveCallInfo
import com.example.telecom.AutomationStep
import com.example.telecom.CallManager
import com.example.telecom.RoleHelper
import com.example.util.ContactHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AppRepository,
    private val appContext: Context
) : ViewModel() {

    // Dialer Input
    private val _dialerNumber = MutableStateFlow("")
    val dialerNumber: StateFlow<String> = _dialerNumber.asStateFlow()

    // Default Dialer Status
    private val _isDefaultDialer = MutableStateFlow(RoleHelper.isDefaultDialer(appContext))
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    // Active Call forwarded from CallManager
    val activeCall: StateFlow<ActiveCallInfo?> = CallManager.activeCall
    val isMuted: StateFlow<Boolean> = CallManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = CallManager.isSpeakerOn
    val automationState: StateFlow<AutomationStep?> = CallManager.automationState
    val lastDtmfKey: StateFlow<Char?> = CallManager.lastDtmfKey

    // In-Call keypad toggle state
    private val _showInCallKeypad = MutableStateFlow(false)
    val showInCallKeypad: StateFlow<Boolean> = _showInCallKeypad.asStateFlow()

    // Dual-SIM State (1 = Primary, 2 = Secondary/Roaming)
    private val _selectedSimSlot = MutableStateFlow(1)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    // Database Flows
    val rules: StateFlow<List<CallerRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCalls: StateFlow<List<RecentCall>> = repository.recentCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteContact>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spamNumbers: StateFlow<List<com.example.data.SpamNumber>> = repository.spamNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automationLogs: StateFlow<List<AutomationLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncWithDeviceContacts()
    }

    /**
     * Synchronizes favorites with Android device Contacts database as the single source of truth.
     * Prioritizes contact nickname over full display name.
     * Keeps Room database updated while preserving speed dial slot assignments and custom sort order.
     */
    fun syncWithDeviceContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val starredOnDevice = ContactHelper.fetchStarredContacts(appContext)
                val currentDbFavorites = favorites.value
                val colors = listOf(0xFF2563EBL, 0xFF16A34AL, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL, 0xFF0891B2L)

                fun norm(num: String) = num.replace(Regex("[^0-9+]"), "").trimStart('0')

                val currentByNorm = currentDbFavorites.associateBy { norm(it.phoneNumber) }
                val deviceByNorm = starredOnDevice.associateBy { norm(it.phoneNumber) }

                var maxOrder = currentDbFavorites.maxOfOrNull { it.sortOrder } ?: -1

                // 1. Merge all starred device contacts into Room
                for (deviceContact in starredOnDevice) {
                    val key = norm(deviceContact.phoneNumber)
                    val existing = currentByNorm[key]
                    if (existing != null) {
                        // Update name (which has nickname prioritized) and photoUri if changed
                        if (existing.name != deviceContact.name || existing.photoUri != deviceContact.photoUri) {
                            repository.updateFavorite(
                                existing.copy(
                                    name = deviceContact.name,
                                    photoUri = deviceContact.photoUri ?: existing.photoUri
                                )
                            )
                        }
                    } else {
                        // Brand new starred contact from device contacts
                        maxOrder++
                        val color = colors[kotlin.math.abs(deviceContact.name.hashCode()) % colors.size]
                        repository.insertFavorite(
                            FavoriteContact(
                                name = deviceContact.name,
                                phoneNumber = deviceContact.phoneNumber,
                                label = deviceContact.label,
                                avatarColor = color,
                                photoUri = deviceContact.photoUri,
                                sortOrder = maxOrder
                            )
                        )
                    }
                }

                // 2. If a contact was previously synced from device contacts but is no longer starred on device:
                // Check if this contact exists in the device contacts phonebook
                val allDeviceContacts = ContactHelper.fetchDeviceContacts(appContext)
                val allDeviceByNorm = allDeviceContacts.associateBy { norm(it.phoneNumber) }

                for (dbFav in currentDbFavorites) {
                    val key = norm(dbFav.phoneNumber)
                    // If it is in the device phonebook but NOT starred on device, remove from favorites
                    if (allDeviceByNorm.containsKey(key) && !deviceByNorm.containsKey(key)) {
                        repository.deleteFavorite(dbFav)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectSimSlot(slot: Int) {
        _selectedSimSlot.value = slot
    }

    fun toggleSimSlot() {
        _selectedSimSlot.value = if (_selectedSimSlot.value == 1) 2 else 1
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

    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, number: String) {
        val cleanNumber = number.ifBlank { _dialerNumber.value }
        if (cleanNumber.isBlank()) return

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val uri = Uri.fromParts("tel", cleanNumber, null)

            // If running on actual device/dialer role
            if (telecomManager != null && RoleHelper.isDefaultDialer(context)) {
                telecomManager.placeCall(uri, null)
            } else {
                // Fallback to ACTION_CALL or start simulated outgoing call for preview
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    context.startActivity(callIntent)
                } else {
                    // Start simulated call so user can test the in-call interface & DTMF keypad
                    CallManager.startSimulatedOutgoingCall(context, cleanNumber)
                }
            }
        } catch (e: Exception) {
            // Simulator fallback if hardware telephony is unavailable
            CallManager.startSimulatedOutgoingCall(context, cleanNumber)
        }
    }

    fun simulateIncomingCall(context: Context, number: String, name: String = "Incoming Caller") {
        CallManager.startSimulatedIncomingCall(context, number, name)
    }

    fun answerCall() {
        CallManager.answerCall()
    }

    fun declineCall() {
        CallManager.declineCall()
    }

    fun disconnectCall() {
        CallManager.disconnectCall()
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
            // Update device Contacts database to star this contact (single source of truth)
            ContactHelper.setContactStarred(appContext, phoneNumber, true)

            val colors = listOf(0xFF2563EBL, 0xFF16A34AL, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL, 0xFF0891B2L)
            val color = colors[kotlin.math.abs(name.hashCode()) % colors.size]
            val maxOrder = favorites.value.maxOfOrNull { it.sortOrder } ?: -1
            repository.insertFavorite(
                com.example.data.FavoriteContact(
                    name = name,
                    phoneNumber = phoneNumber,
                    label = label,
                    avatarColor = color,
                    photoUri = photoUri,
                    sortOrder = maxOrder + 1
                )
            )
        }
    }

    fun updateFavorite(contact: com.example.data.FavoriteContact, newNickname: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val effectiveName = if (!newNickname.isNullOrBlank()) newNickname.trim() else contact.name
            val updated = contact.copy(name = effectiveName)
            repository.updateFavorite(updated)

            // If user updated nickname/name, sync to device Contacts database as well
            if (!newNickname.isNullOrBlank()) {
                ContactHelper.updateContactNickname(appContext, contact.phoneNumber, newNickname.trim())
            }
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
        viewModelScope.launch(Dispatchers.IO) {
            val existing = favorites.value.firstOrNull { it.phoneNumber == phoneNumber }
            if (existing != null) {
                ContactHelper.setContactStarred(appContext, phoneNumber, false)
                repository.deleteFavorite(existing)
            } else {
                ContactHelper.setContactStarred(appContext, phoneNumber, true)
                addFavorite(name, phoneNumber, label, photoUri)
            }
        }
    }

    fun deleteFavorite(contact: com.example.data.FavoriteContact) {
        viewModelScope.launch(Dispatchers.IO) {
            // Unstar in device contacts database (single source of truth)
            ContactHelper.setContactStarred(appContext, contact.phoneNumber, false)
            repository.deleteFavorite(contact)
        }
    }

    fun markAsSpam(phoneNumber: String, label: String = "Reported Spam") {
        viewModelScope.launch {
            repository.insertSpamNumber(
                com.example.data.SpamNumber(
                    phoneNumber = phoneNumber,
                    label = label,
                    reportCount = 1,
                    isBlocked = true
                )
            )
        }
    }

    fun removeSpam(phoneNumber: String) {
        viewModelScope.launch {
            repository.deleteSpamByNumber(phoneNumber)
        }
    }

    fun assignSpeedDial(contact: com.example.data.FavoriteContact, slot: Int) {
        viewModelScope.launch {
            repository.updateFavorite(contact.copy(speedDialSlot = slot))
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
