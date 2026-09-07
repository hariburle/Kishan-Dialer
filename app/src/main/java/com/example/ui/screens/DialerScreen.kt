package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.WhatsAppIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.telecom.SimInfo
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.FavoritesSection
import com.example.ui.components.Keypad
import com.example.ui.components.MultiNumberCallDialog
import com.example.ui.components.RoleBanner

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChipDefaults
import com.example.util.ContactHelper
import com.example.util.DeviceContact
import com.example.util.T9Helper
import com.example.util.T9SearchResult

@Composable
fun DialerScreen(
    number: String,
    favorites: List<FavoriteContact>,
    recentCalls: List<RecentCall> = emptyList(),
    isDefaultDialer: Boolean,
    context: Context,
    simSlot: Int = 1,
    activeSims: List<SimInfo> = emptyList(),
    onToggleSim: () -> Unit = {},
    onRoleChanged: () -> Unit,
    onDigitPress: (Char) -> Unit,
    onDeleteDigit: () -> Unit,
    onClearDigits: () -> Unit,
    onSelectContactNumber: (String) -> Unit,
    onPlaceCall: (String, String?) -> Unit,
    onSimulateCall: (String, String) -> Unit,
    onCreateRuleForNumber: (String) -> Unit,
    onAddFavorite: (String, String, String, String?) -> Unit,
    onDeleteFavorite: (FavoriteContact) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showContactPicker by remember { mutableStateOf(false) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var matchedContact by remember { mutableStateOf<DeviceContact?>(null) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var speedDialToast by remember { mutableStateOf<String?>(null) }
    var selectedCallReason by remember { mutableStateOf<String?>(null) }
    var multiNumberContactToCall by remember { mutableStateOf<DeviceContact?>(null) }
    var multiNumberSpeedDialSlot by remember { mutableStateOf<Int?>(null) }
    var multiNumberFavoriteTarget by remember { mutableStateOf<FavoriteContact?>(null) }

    // Load device contacts once for T9 search
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = ContactHelper.fetchDeviceContacts(context)
            deviceContacts = list
        }
    }

    // Combine all contacts for T9
    val allSearchContacts = remember(favorites, deviceContacts) {
        val list = mutableListOf<DeviceContact>()
        favorites.forEach { list.add(DeviceContact(it.name, it.phoneNumber, it.label, it.photoUri)) }
        list.addAll(deviceContacts)
        list.distinctBy { it.phoneNumber }
    }

    // T9 search results
    val t9Matches = remember(number, allSearchContacts) {
        if (number.isNotBlank()) {
            T9Helper.search(allSearchContacts, number)
        } else emptyList()
    }

    androidx.compose.runtime.LaunchedEffect(number) {
        if (number.isNotBlank()) {
            val fav = favorites.firstOrNull { it.phoneNumber == number }
            if (fav != null) {
                matchedContact = DeviceContact(fav.name, fav.phoneNumber, fav.label, fav.photoUri)
            } else {
                matchedContact = ContactHelper.lookupContactByNumber(context, number)
            }
        } else {
            matchedContact = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("dialer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Default Dialer prompt banner
            RoleBanner(
                isDefaultDialer = isDefaultDialer,
                context = context,
                onRoleChanged = onRoleChanged
            )

            // FAVORITES SECTION - Shown prominently when app opens
            FavoritesSection(
                favorites = favorites,
                onSelectContact = { selectedNumber ->
                    onSelectContactNumber(selectedNumber)
                },
                onCallContact = { selectedNumber ->
                    val normNum = selectedNumber.replace(Regex("[^0-9+]"), "")
                    val matchedDevContact = deviceContacts.firstOrNull { dc ->
                        dc.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normNum } ||
                        dc.phoneNumber.replace(Regex("[^0-9+]"), "") == normNum
                    }
                    val matchedFav = favorites.firstOrNull { it.phoneNumber.replace(Regex("[^0-9+]"), "") == normNum }
                    if (matchedDevContact != null && matchedDevContact.phoneNumbers.size > 1) {
                        multiNumberContactToCall = matchedDevContact
                        multiNumberSpeedDialSlot = null
                        multiNumberFavoriteTarget = matchedFav
                    } else {
                        onSelectContactNumber(selectedNumber)
                        onPlaceCall(selectedNumber, null)
                    }
                },
                onCreateRule = { selectedNumber ->
                    onCreateRuleForNumber(selectedNumber)
                },
                onAddFavoriteClick = {
                    showAddFavoriteDialog = true
                },
                onDeleteFavorite = onDeleteFavorite,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Matched contact pill or Add to Favorites chip in fixed-height container to prevent keypad jumping
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (matchedContact != null) {
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (!matchedContact!!.photoUri.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = matchedContact!!.photoUri,
                                    contentDescription = matchedContact!!.name,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = matchedContact!!.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = matchedContact!!.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• ${matchedContact!!.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val comm = remember(number) { com.example.util.CommunityCallerIdService.lookup(number) }
                    if (comm != null) {
                        val isSpam = comm.spamScore > 50
                        Row(
                            modifier = Modifier
                                .background(
                                    if (isSpam) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpam) Icons.Default.Warning else Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSpam) Color(0xFFDC2626) else Color(0xFF0284C7)
                            )
                            Text(
                                text = "${comm.name} • ${comm.category}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpam) Color(0xFF991B1B) else Color(0xFF0369A1)
                            )
                        }
                    } else if (number.length >= 3) {
                        AssistChip(
                            onClick = { showAddFavoriteDialog = true },
                            label = { Text("Add to Favorites", fontSize = 11.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            // Dialed Number Display Area with Contact Picker & Backspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Contact Picker Button (select a contact phone number directly)
                    IconButton(
                        onClick = { showContactPicker = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pick_contact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "Select Contact",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    if (number.isEmpty()) {
                        Text(
                            text = "Enter number or pick contact",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialer_number_display")
                        )
                    }

                    // Backspace button
                    IconButton(
                        onClick = onDeleteDigit,
                        enabled = number.isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dialer_backspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Delete Digit",
                            tint = if (number.isNotEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // T9 Smart Search Matches Container - Fixed height to prevent dialer pad bouncing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (t9Matches.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "T9 MATCHES (${t9Matches.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tap to select",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            t9Matches.forEach { match ->
                                Card(
                                    onClick = {
                                        onSelectContactNumber(match.phoneNumber)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("t9_chip_${match.phoneNumber}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = match.name.take(1).uppercase(),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = match.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = match.phoneNumber,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Speed dial toast / feedback message container with fixed height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (speedDialToast != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.inverseSurface
                    ) {
                        Text(
                            text = speedDialToast ?: "",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            val speedDialMap = remember(favorites) {
                val map = mutableMapOf<Char, String>()
                map['1'] = "VM"
                (2..9).forEach { slot ->
                    val fav = favorites.firstOrNull { it.speedDialSlot == slot }
                    if (fav != null) {
                        val shortName = fav.nickname?.takeIf { it.isNotBlank() }
                            ?: fav.name.trim().split(" ").firstOrNull()
                            ?: fav.name
                        map[slot.digitToChar()] = shortName.take(8)
                    }
                }
                map
            }

            // Main Telephone Keypad with Speed Dial Long-Press
            Keypad(
                compact = true,
                speedDialMap = speedDialMap,
                onDigitPress = onDigitPress,
                onDigitLongPress = { digit ->
                    when (digit) {
                        '0' -> onDigitPress('+')
                        '1' -> {
                            val vmNumber = ContactHelper.getVoicemailNumber(context)
                            speedDialToast = "Voicemail ($vmNumber)"
                            onSelectContactNumber(vmNumber)
                            onPlaceCall(vmNumber, null)
                        }
                        in '2'..'9' -> {
                            val slotNum = digit.digitToInt()
                            val fav = favorites.firstOrNull { it.speedDialSlot == slotNum }
                            if (fav != null) {
                                val normNum = fav.phoneNumber.replace(Regex("[^0-9+]"), "")
                                val matchedDevContact = deviceContacts.firstOrNull { dc ->
                                    dc.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normNum } ||
                                    dc.phoneNumber.replace(Regex("[^0-9+]"), "") == normNum ||
                                    dc.name.equals(fav.name, ignoreCase = true)
                                }
                                if (matchedDevContact != null && matchedDevContact.phoneNumbers.size > 1) {
                                    multiNumberContactToCall = matchedDevContact
                                    multiNumberSpeedDialSlot = slotNum
                                    multiNumberFavoriteTarget = fav
                                } else {
                                    speedDialToast = "#$slotNum: Calling ${fav.name}..."
                                    onSelectContactNumber(fav.phoneNumber)
                                    onPlaceCall(fav.phoneNumber, null)
                                }
                            } else {
                                speedDialToast = "#$slotNum is unassigned"
                            }
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Dual-SIM Selector & Call Action Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Contextual Caller ID ("Call Reason") Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Call Reason Context:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (selectedCallReason != null) {
                        Text(
                            text = "Selected: $selectedCallReason",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                val reasons = listOf("Urgent", "Quick Question", "Work", "Personal", "Delivery")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { selectedCallReason = null },
                        label = { Text("None", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedCallReason == null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        )
                    )
                    reasons.forEach { reason ->
                        AssistChip(
                            onClick = {
                                selectedCallReason = if (selectedCallReason == reason) null else reason
                            },
                            label = { Text(reason, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCallReason == reason) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            // Dual-SIM Toggle Pill & WhatsApp Smart Suggestion
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onToggleSim,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("sim_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = "Active SIM",
                            tint = if (simSlot == 1) Color(0xFF2563EB) else Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        val currentSim = activeSims.firstOrNull { it.slotIndex + 1 == simSlot }
                        val simLabel = when {
                            currentSim != null -> "SIM $simSlot (${currentSim.displayName})"
                            simSlot == 1 -> "SIM 1 (Primary)"
                            else -> "SIM 2 (Work / Roaming)"
                        }
                        Text(
                            text = simLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (activeSims.size > 1) "• Switch" else "• Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Call Actions Row: Standard SIM Call Button + WhatsApp Voice Call Button (with dynamic preferred sizing)
            val normNum = number.replace(Regex("[^0-9+]"), "").takeLast(10)
            val relevantCalls = recentCalls.filter { rc ->
                val rcNorm = rc.phoneNumber.replace(Regex("[^0-9+]"), "").takeLast(10)
                if (normNum.isNotBlank() && rcNorm.isNotBlank()) {
                    rcNorm.endsWith(normNum) || normNum.endsWith(rcNorm)
                } else false
            }
            val waCallsCount = relevantCalls.count { it.callReason?.contains("WhatsApp", ignoreCase = true) == true }
            val gsmCallsCount = relevantCalls.size - waCallsCount
            val isWaPreferred = waCallsCount > gsmCallsCount && waCallsCount > 0
            val isGsmPreferred = gsmCallsCount > waCallsCount && gsmCallsCount > 0

            val waButtonSize = when {
                isWaPreferred -> 72.dp
                isGsmPreferred -> 48.dp
                else -> 60.dp
            }
            val gsmButtonSize = when {
                isGsmPreferred -> 72.dp
                isWaPreferred -> 48.dp
                else -> 60.dp
            }
            val waIconSize = if (isWaPreferred) 32.dp else if (isGsmPreferred) 20.dp else 24.dp
            val gsmIconSize = if (isGsmPreferred) 32.dp else if (isWaPreferred) 20.dp else 24.dp

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // WhatsApp Voice Call Shortcut
                FilledIconButton(
                    onClick = {
                        ContactHelper.launchWhatsAppCall(context, number.ifBlank { "+91" })
                    },
                    modifier = Modifier
                        .size(waButtonSize)
                        .testTag("whatsapp_call_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    WhatsAppIcon(
                        modifier = Modifier.size(waIconSize)
                    )
                }

                // Standard SIM GSM Call Button
                FilledIconButton(
                    onClick = { onPlaceCall(number, selectedCallReason) },
                    modifier = Modifier
                        .size(gsmButtonSize)
                        .testTag("dialer_call_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Place Call",
                        modifier = Modifier.size(gsmIconSize)
                    )
                }
            }
        }
    }

    // Contact Picker Dialog
    if (showContactPicker) {
        ContactPickerDialog(
            favorites = favorites,
            onContactSelected = { _, selectedNumber, _ ->
                onSelectContactNumber(selectedNumber)
            },
            onDismiss = { showContactPicker = false }
        )
    }

    // Add Favorite Dialog
    if (showAddFavoriteDialog) {
        AddFavoriteDialog(
            initialNumber = number,
            initialName = matchedContact?.name ?: "",
            initialPhotoUri = matchedContact?.photoUri,
            onDismiss = { showAddFavoriteDialog = false },
            onSave = { name, favNumber, label, photoUri ->
                onAddFavorite(name, favNumber, label, photoUri)
            },
            onPickFromContacts = {
                showContactPicker = true
            }
        )
    }

    // Multi-Number Confirmation Dialog for Speed Dial and Favorites
    if (multiNumberContactToCall != null) {
        val contact = multiNumberContactToCall!!
        MultiNumberCallDialog(
            contactName = contact.name,
            phoneNumbers = contact.phoneNumbers,
            defaultNumber = multiNumberFavoriteTarget?.phoneNumber ?: contact.phoneNumber,
            titlePrefix = if (multiNumberSpeedDialSlot != null) "Speed Dial #$multiNumberSpeedDialSlot" else "Favorite",
            onSelectNumberToCall = { chosenNumber ->
                onSelectContactNumber(chosenNumber)
                onPlaceCall(chosenNumber, null)
            },
            onSearchOtherContacts = {
                showContactPicker = true
            },
            onDismiss = {
                multiNumberContactToCall = null
                multiNumberSpeedDialSlot = null
                multiNumberFavoriteTarget = null
            }
        )
    }
}
