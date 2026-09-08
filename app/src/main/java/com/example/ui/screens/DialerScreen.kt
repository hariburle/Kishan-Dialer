package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.WhatsAppIcon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.telecom.SimInfo
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.CreateContactDialog
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
private fun QuickRecentsSection(
    recentCalls: List<RecentCall>,
    onSelectNumber: (String) -> Unit
) {
    if (recentCalls.isEmpty()) return
    val topRecent = remember(recentCalls) { recentCalls.take(6) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        Text(
            text = "QUICK RECENTS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topRecent.forEach { call ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectNumber(call.phoneNumber) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when (call.callType) {
                                1 -> Icons.AutoMirrored.Filled.CallReceived
                                2 -> Icons.AutoMirrored.Filled.CallMade
                                else -> Icons.AutoMirrored.Filled.CallMissed
                            },
                            contentDescription = null,
                            tint = when (call.callType) {
                                1 -> Color(0xFF16A34A)
                                2 -> MaterialTheme.colorScheme.primary
                                else -> Color(0xFFDC2626)
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Column {
                            Text(
                                text = call.callerName?.ifBlank { null } ?: call.phoneNumber,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = call.phoneNumber,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    onPlaceWhatsAppCall: (String) -> Unit = { ContactHelper.launchWhatsAppCall(context, it) },
    onSimulateCall: (String, String) -> Unit,
    onCreateRuleForNumber: (String) -> Unit,
    onAddFavorite: (String, String, String, String?) -> Unit,
    onAddNewContact: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteFavorite: (FavoriteContact) -> Unit,
    onAssignSpeedDial: (FavoriteContact, Int) -> Unit = { _, _ -> },
    onAssignSpeedDialSlot: (Int, String, String, String?) -> Unit = { _, _, _, _ -> },
    onClearSpeedDialSlot: (Int) -> Unit = {},
    deviceContacts: List<DeviceContact> = emptyList(),
    modifier: Modifier = Modifier
) {
    var showContactPicker by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var assignSpeedDialSlotTarget by remember { mutableStateOf<Int?>(null) }
    var speedDialActionSlotTarget by remember { mutableStateOf<Pair<Int, FavoriteContact>?>(null) }
    var matchedContact by remember { mutableStateOf<DeviceContact?>(null) }
    var localContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    val effectiveContacts = if (deviceContacts.isNotEmpty()) deviceContacts else localContacts
    var speedDialToast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(speedDialToast) {
        if (speedDialToast != null) {
            kotlinx.coroutines.delay(2000)
            speedDialToast = null
        }
    }
    var selectedCallReason by remember { mutableStateOf<String?>(null) }
    var multiNumberContactToCall by remember { mutableStateOf<DeviceContact?>(null) }
    var multiNumberSpeedDialSlot by remember { mutableStateOf<Int?>(null) }
    var multiNumberFavoriteTarget by remember { mutableStateOf<FavoriteContact?>(null) }

    // Load device contacts if not provided by parent
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (deviceContacts.isEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val list = ContactHelper.fetchDeviceContacts(context)
                localContacts = list
            }
        }
    }

    // Combine all contacts for T9
    val allSearchContacts = remember(favorites, effectiveContacts) {
        val list = mutableListOf<DeviceContact>()
        favorites.forEach { list.add(DeviceContact(it.name, it.phoneNumber, it.label, it.photoUri)) }
        list.addAll(effectiveContacts)
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
            .testTag("dialer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Default Dialer prompt banner
        RoleBanner(
            isDefaultDialer = isDefaultDialer,
            context = context,
            onRoleChanged = onRoleChanged
        )

        // Flexible top container absorbs all dynamic sizing so the edit box, keypad, and buttons stay strictly fixed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            // QUICK RECENTS & FAVORITES SECTION - Shown prominently when app opens and number is empty
            if (number.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (recentCalls.isNotEmpty()) {
                        QuickRecentsSection(
                            recentCalls = recentCalls,
                            onSelectNumber = { onSelectContactNumber(it) }
                        )
                    }
                    if (favorites.isNotEmpty()) {
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
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            } else if (number.isNotEmpty()) {
                // When number is entered, show matched contact info and T9 search matches above the edit box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Matched contact pill, spam indicator, or Add to Favorites chip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
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

                    // T9 Smart Search Matches Container in Top Container (ZERO bounce on keypad/edit box!)
                    if (t9Matches.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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

                                Spacer(modifier = Modifier.height(2.dp))

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
                }
            }
        }

        // Fixed-Position Keypad and Dialing Controls (zero bouncing/shifting)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dialed Number Display Area with Contact Picker / Overflow Menu & Backspace (Fixed 56.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        if (number.isEmpty()) {
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
                        } else {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dialer_overflow_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add 2-sec pause (,)") },
                                    onClick = {
                                        onDigitPress(',')
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add wait (;)") },
                                    onClick = {
                                        onDigitPress(';')
                                        showOverflowMenu = false
                                    }
                                )
                            }
                        }
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

            // Speed dial toast / feedback message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
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
                        '*' -> onDigitPress(',')
                        '#' -> onDigitPress(';')
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
                                speedDialActionSlotTarget = Pair(slotNum, fav)
                            } else {
                                speedDialToast = "Assign contact to #$slotNum"
                                assignSpeedDialSlotTarget = slotNum
                            }
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Dual-SIM Selector & Call Action Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
            // Reclaimed Space: Compact Row combining SIM Slot & Call Reason Dropdown
            var showCallReasonMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dual-SIM Toggle Pill
                Surface(
                    onClick = onToggleSim,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("sim_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = "Active SIM",
                            tint = if (simSlot == 1) Color(0xFF2563EB) else Color(0xFF16A34A),
                            modifier = Modifier.size(15.dp)
                        )
                        val currentSim = activeSims.firstOrNull { it.slotIndex + 1 == simSlot }
                        val simLabel = when {
                            currentSim != null -> "SIM $simSlot (${currentSim.displayName.take(8)})"
                            simSlot == 1 -> "SIM 1 (Primary)"
                            else -> "SIM 2"
                        }
                        Text(
                            text = simLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activeSims.size > 1) {
                            Text(
                                text = "• Switch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Call Reason Context Dropdown next to SIM
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedCallReason != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { showCallReasonMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (selectedCallReason != null) "Reason: $selectedCallReason" else "Reason: None",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedCallReason != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCallReason != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Call Reason",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showCallReasonMenu,
                        onDismissRequest = { showCallReasonMenu = false }
                    ) {
                        val reasons = listOf(null, "Urgent", "Quick Question", "Work", "Personal", "Delivery")
                        reasons.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason ?: "None (Clear)") },
                                onClick = {
                                    selectedCallReason = reason
                                    showCallReasonMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Call Actions Row: Standard SIM Call Button + WhatsApp Voice Call Button (fixed size with non-intrusive preference highlight)
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

            val waBtnSize = if (isWaPreferred) 56.dp else if (isGsmPreferred) 46.dp else 52.dp
            val gsmBtnSize = if (isGsmPreferred) 56.dp else if (isWaPreferred) 46.dp else 52.dp

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // WhatsApp Voice Call Button with clear accent border highlight when preferred
                FilledIconButton(
                    onClick = {
                        onPlaceWhatsAppCall(number.ifBlank { "+91" })
                    },
                    modifier = Modifier
                        .size(waBtnSize)
                        .then(
                            if (isWaPreferred) {
                                Modifier
                                    .border(2.5.dp, Color(0xFF16A34A), CircleShape)
                                    .padding(2.5.dp)
                            } else Modifier
                        )
                        .testTag("whatsapp_call_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isGsmPreferred) Color(0xFF25D366).copy(alpha = 0.85f) else Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    WhatsAppIcon(
                        modifier = Modifier.size(if (isWaPreferred) 28.dp else if (isGsmPreferred) 22.dp else 25.dp)
                    )
                }

                // Standard SIM GSM Call Button with clear accent border highlight when preferred
                FilledIconButton(
                    onClick = { onPlaceCall(number, selectedCallReason) },
                    modifier = Modifier
                        .size(gsmBtnSize)
                        .then(
                            if (isGsmPreferred) {
                                Modifier
                                    .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(2.5.dp)
                            } else Modifier
                        )
                        .testTag("dialer_call_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isWaPreferred) Color(0xFF16A34A).copy(alpha = 0.85f) else Color(0xFF16A34A),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Place Call",
                        modifier = Modifier.size(if (isGsmPreferred) 28.dp else if (isWaPreferred) 22.dp else 25.dp)
                    )
                }
            }
        }
    }

    // Contact Picker Dialog
    if (showContactPicker) {
        ContactPickerDialog(
            favorites = favorites,
            deviceContacts = deviceContacts,
            onContactSelected = { _, selectedNumber, _ ->
                onSelectContactNumber(selectedNumber)
            },
            onDismiss = { showContactPicker = false }
        )
    }

    // Add Favorite / New Contact Dialog
    if (showAddFavoriteDialog) {
        CreateContactDialog(
            initialNumber = number,
            initialName = matchedContact?.name ?: "",
            dialogTitle = "Add to Favorites",
            initialAddToFavorites = true,
            onDismiss = { showAddFavoriteDialog = false },
            onSave = { name, favNumber, label, destination, addToFavs ->
                onAddNewContact(name, favNumber, label, destination, addToFavs)
                showAddFavoriteDialog = false
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

    if (speedDialActionSlotTarget != null) {
        val (slot, fav) = speedDialActionSlotTarget!!
        AlertDialog(
            onDismissRequest = { speedDialActionSlotTarget = null },
            title = {
                Text("Speed Dial #$slot: ${fav.name}")
            },
            text = {
                Text("${fav.phoneNumber} (${fav.label})")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetNum = fav.phoneNumber
                        speedDialActionSlotTarget = null
                        onSelectContactNumber(targetNum)
                        onPlaceCall(targetNum, null)
                    }
                ) {
                    Text("Call")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { speedDialActionSlotTarget = null }
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val targetSlot = slot
                            speedDialActionSlotTarget = null
                            onClearSpeedDialSlot(targetSlot)
                            speedDialToast = "Cleared Speed Dial #$targetSlot"
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        onClick = {
                            val targetSlot = slot
                            speedDialActionSlotTarget = null
                            assignSpeedDialSlotTarget = targetSlot
                        }
                    ) {
                        Text("Reassign")
                    }
                }
            }
        )
    }

    if (assignSpeedDialSlotTarget != null) {
        val targetSlot = assignSpeedDialSlotTarget!!
        ContactPickerDialog(
            favorites = favorites,
            deviceContacts = deviceContacts,
            onContactSelected = { name, number, photoUri ->
                onAssignSpeedDialSlot(targetSlot, name, number, photoUri)
                val displayName = name.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: name
                speedDialToast = "Assigned $displayName to #$targetSlot"
                assignSpeedDialSlotTarget = null
            },
            onDismiss = { assignSpeedDialSlotTarget = null },
            title = "Assign Speed Dial #$targetSlot"
        )
    }
}
}
