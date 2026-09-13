package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalView
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
import com.example.ui.components.QuickRecentsSection
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalFoundationApi::class)
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
    confirmSpeedDialCall: Boolean = true,
    askToAssignUnassignedSpeedDial: Boolean = true,
    deviceContacts: List<DeviceContact> = emptyList(),
    getPreferredCallingMode: (String) -> String = { "cellular" },
    learnedCallModes: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var showContactPicker by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var assignSpeedDialSlotTarget by remember { mutableStateOf<Int?>(null) }
    var promptAssignSlotTarget by remember { mutableStateOf<Int?>(null) }
    var speedDialActionSlotTarget by remember { mutableStateOf<Pair<Int, FavoriteContact>?>(null) }
    var matchedContact by remember { mutableStateOf<DeviceContact?>(null) }
    val effectiveContacts = deviceContacts
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

    // Combine all contacts for T9, prioritizing official device contacts for accurate names
    val allSearchContacts = remember(favorites, effectiveContacts) {
        fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
        val list = mutableListOf<DeviceContact>()
        list.addAll(effectiveContacts)
        val knownDigits = effectiveContacts.flatMap { dc ->
            dc.phoneNumbers.map { normDigits(it.number) } + listOf(normDigits(dc.phoneNumber))
        }.filter { it.isNotBlank() }.toSet()

        favorites.forEach { fav ->
            val fDigits = normDigits(fav.phoneNumber)
            if (fDigits.isBlank() || !knownDigits.contains(fDigits)) {
                list.add(DeviceContact(fav.name, fav.phoneNumber, fav.label, fav.photoUri, nickname = fav.nickname, isStarred = true))
            }
        }
        list.distinctBy { dc ->
            val digits = normDigits(dc.phoneNumber)
            if (digits.isNotBlank()) digits else (dc.name.trim().lowercase() + "_" + (dc.contactId ?: 0L))
        }
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
                                    label = { Text("Add Contact", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
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
                                        text = "T9 Matches (${t9Matches.size})",
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
                .padding(top = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var selectionState by remember(number) {
                mutableStateOf(TextRange(number.length))
            }

            val localOnDigitPress: (Char) -> Unit = { digit ->
                val start = selectionState.start.coerceIn(0, number.length)
                val end = selectionState.end.coerceIn(0, number.length)
                val minSel = minOf(start, end)
                val maxSel = maxOf(start, end)
                val newText = number.substring(0, minSel) + digit + number.substring(maxSel)
                selectionState = TextRange(minSel + 1)
                onSelectContactNumber(newText)
            }

            val localOnDeleteDigit: () -> Unit = {
                val start = selectionState.start.coerceIn(0, number.length)
                val end = selectionState.end.coerceIn(0, number.length)
                if (start == end) {
                    if (start > 0) {
                        val newText = number.substring(0, start - 1) + number.substring(start)
                        selectionState = TextRange(start - 1)
                        onSelectContactNumber(newText)
                    }
                } else {
                    val minSel = minOf(start, end)
                    val maxSel = maxOf(start, end)
                    val newText = number.substring(0, minSel) + number.substring(maxSel)
                    selectionState = TextRange(minSel)
                    onSelectContactNumber(newText)
                }
            }

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
                                DropdownMenuItem(
                                    text = { Text("Send Text Message (SMS)") },
                                    onClick = {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                                        context.startActivity(smsIntent)
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Send WhatsApp Message") },
                                    onClick = {
                                        ContactHelper.launchWhatsAppMessage(context, number)
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
                        val tFV = TextFieldValue(text = number, selection = selectionState)
                        BasicTextField(
                            value = tFV,
                            onValueChange = { newValue ->
                                selectionState = newValue.selection
                                if (newValue.text != number) {
                                    onSelectContactNumber(newValue.text)
                                }
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialer_number_display"),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    innerTextField()
                                }
                            }
                        )
                    }

                    // Backspace button with click to delete single digit & long-press to clear entire field
                    val view = LocalView.current
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (number.isNotEmpty()) {
                                    Modifier.combinedClickable(
                                        onClick = localOnDeleteDigit,
                                        onLongClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            onClearDigits()
                                        }
                                    )
                                } else Modifier
                            )
                            .testTag("dialer_backspace_button"),
                        contentAlignment = Alignment.Center
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
                onDigitPress = localOnDigitPress,
                onDigitLongPress = { digit ->
                    when (digit) {
                        '0' -> localOnDigitPress('+')
                        '*' -> localOnDigitPress(',')
                        '#' -> localOnDigitPress(';')
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
                                if (confirmSpeedDialCall) {
                                    speedDialActionSlotTarget = Pair(slotNum, fav)
                                } else {
                                    val targetNum = fav.phoneNumber
                                    speedDialToast = "Calling ${fav.name} (#$slotNum)..."
                                    onSelectContactNumber(targetNum)
                                    onPlaceCall(targetNum, null)
                                }
                            } else {
                                if (askToAssignUnassignedSpeedDial) {
                                    promptAssignSlotTarget = slotNum
                                } else {
                                    speedDialToast = "Speed dial #$slotNum is unassigned"
                                }
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

            // Call Actions Grid (2x2): An elegant, non-interfering layout with high-fidelity buttons and situational intelligence highlights
            val callingMode = if (number.isNotBlank()) getPreferredCallingMode(number) else "none"
            val isWaPreferred = callingMode == "whatsapp"
            val isGsmPreferred = callingMode == "cellular"
            val isDark = isSystemInDarkTheme()
            val isNumEmpty = number.isBlank()

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Text Message (Left) | Phone (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // --- Text Message ---
                    val smsBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    Card(
                        onClick = {
                            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                            context.startActivity(smsIntent)
                        },
                        enabled = !isNumEmpty,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = smsBgColor),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .then(if (isNumEmpty) Modifier.graphicsLayer(alpha = 0.45f) else Modifier)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7).copy(alpha = if (isDark) 0.25f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Text Message",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Text Message",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // --- Phone ---
                    val phoneBorderColor = if (isGsmPreferred) Color(0xFF059669) else MaterialTheme.colorScheme.outlineVariant
                    val phoneBgColor = if (isGsmPreferred) {
                        Color(0xFF059669).copy(alpha = if (isDark) 0.15f else 0.08f)
                    } else {
                        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    }
                    val phoneIconBg = if (isGsmPreferred) Color(0xFF059669) else Color(0xFF059669).copy(alpha = if (isDark) 0.25f else 0.12f)
                    val phoneIconColor = if (isGsmPreferred) Color.White else Color(0xFF059669)

                    Card(
                        onClick = { onPlaceCall(number, selectedCallReason) },
                        enabled = !isNumEmpty,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = phoneBgColor),
                        border = BorderStroke(if (isGsmPreferred) 2.dp else 1.dp, phoneBorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .then(if (isNumEmpty) Modifier.graphicsLayer(alpha = 0.45f) else Modifier)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(phoneIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Phone Call",
                                    tint = phoneIconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Phone",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Row 2: WhatsApp - Msg (Left) | WhatsApp - Voice (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // --- WhatsApp - Msg ---
                    val waMsgBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    Card(
                        onClick = { ContactHelper.launchWhatsAppMessage(context, number) },
                        enabled = !isNumEmpty,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = waMsgBgColor),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .then(if (isNumEmpty) Modifier.graphicsLayer(alpha = 0.45f) else Modifier)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0D9488).copy(alpha = if (isDark) 0.25f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "WhatsApp Msg",
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "WhatsApp - Msg",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // --- WhatsApp - Voice ---
                    val waBorderColor = if (isWaPreferred) Color(0xFF25D366) else MaterialTheme.colorScheme.outlineVariant
                    val waBgColor = if (isWaPreferred) {
                        Color(0xFF25D366).copy(alpha = if (isDark) 0.15f else 0.08f)
                    } else {
                        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    }
                    val waIconBg = if (isWaPreferred) Color(0xFF25D366) else Color(0xFF25D366).copy(alpha = if (isDark) 0.25f else 0.12f)
                    val waIconColor = if (isWaPreferred) Color.White else Color(0xFF1E7E34)

                    Card(
                        onClick = { onPlaceWhatsAppCall(number) },
                        enabled = !isNumEmpty,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = waBgColor),
                        border = BorderStroke(if (isWaPreferred) 2.dp else 1.dp, waBorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .then(if (isNumEmpty) Modifier.graphicsLayer(alpha = 0.45f) else Modifier)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(waIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                WhatsAppIcon(
                                    modifier = Modifier.size(16.dp),
                                    tint = waIconColor
                                )
                            }
                            Text(
                                text = "WhatsApp - Voice",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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

    // Add Contact Dialog
    if (showAddFavoriteDialog) {
        CreateContactDialog(
            initialNumber = number,
            initialName = matchedContact?.name ?: "",
            dialogTitle = "Add Contact",
            initialAddToFavorites = false,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$slot",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Column {
                        Text(
                            text = fav.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Speed Dial Shortcut",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = fav.phoneNumber,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (fav.label.isNotBlank()) {
                                Text(
                                    text = fav.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Prominent Call Button
                    Button(
                        onClick = {
                            val targetNum = fav.phoneNumber
                            speedDialActionSlotTarget = null
                            onSelectContactNumber(targetNum)
                            onPlaceCall(targetNum, null)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("speed_dial_call_button")
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = "Call",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Call ${fav.name.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: fav.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                // Secondary options formatted as clean text links (do not look like buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reassign",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .testTag("speed_dial_reassign_action")
                            .clickable {
                                val targetSlot = slot
                                speedDialActionSlotTarget = null
                                assignSpeedDialSlotTarget = targetSlot
                            }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                    )
                    Text(
                        text = "Clear",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .testTag("speed_dial_clear_action")
                            .clickable {
                                val targetSlot = slot
                                speedDialActionSlotTarget = null
                                onClearSpeedDialSlot(targetSlot)
                                speedDialToast = "Cleared Speed Dial #$targetSlot"
                            }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                    )
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .testTag("speed_dial_cancel_action")
                            .clickable {
                                speedDialActionSlotTarget = null
                            }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                    )
                }
            }
        )
    }

    if (promptAssignSlotTarget != null) {
        val slot = promptAssignSlotTarget!!
        AlertDialog(
            onDismissRequest = { promptAssignSlotTarget = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$slot",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Text(
                        text = "Speed Dial #$slot",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Text(
                    text = "Key #$slot is not assigned. Would you like to assign a contact to this speed dial shortcut?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetSlot = slot
                        promptAssignSlotTarget = null
                        assignSpeedDialSlotTarget = targetSlot
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("assign_speed_dial_confirm_button")
                ) {
                    Text("Assign Contact", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .testTag("assign_speed_dial_cancel_action")
                        .clickable { promptAssignSlotTarget = null }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
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
