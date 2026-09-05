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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
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
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.FavoritesSection
import com.example.ui.components.Keypad
import com.example.ui.components.RoleBanner

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import com.example.util.ContactHelper
import com.example.util.DeviceContact
import com.example.util.T9Helper
import com.example.util.T9SearchResult

@Composable
fun DialerScreen(
    number: String,
    favorites: List<FavoriteContact>,
    isDefaultDialer: Boolean,
    context: Context,
    simSlot: Int = 1,
    onToggleSim: () -> Unit = {},
    onRoleChanged: () -> Unit,
    onDigitPress: (Char) -> Unit,
    onDeleteDigit: () -> Unit,
    onClearDigits: () -> Unit,
    onSelectContactNumber: (String) -> Unit,
    onPlaceCall: (String) -> Unit,
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
                    onSelectContactNumber(selectedNumber)
                    onPlaceCall(selectedNumber)
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
                } else if (number.length >= 3) {
                    AssistChip(
                        onClick = { showAddFavoriteDialog = true },
                        label = { Text("Add to Favorites", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
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

            // Main Telephone Keypad with Speed Dial Long-Press
            Keypad(
                compact = true,
                onDigitPress = onDigitPress,
                onDigitLongPress = { digit ->
                    when (digit) {
                        '0' -> onDigitPress('+')
                        '1' -> {
                            // Voicemail or VIP 1
                            speedDialToast = "Speed Dial: Voicemail (*123)"
                            onSelectContactNumber("*123")
                            onPlaceCall("*123")
                        }
                        in '2'..'9' -> {
                            val slotNum = digit.digitToInt()
                            // Look for favorite assigned to slot or fallback to indexed favorite
                            val fav = favorites.firstOrNull { it.speedDialSlot == slotNum }
                                ?: favorites.getOrNull(slotNum - 2)
                            if (fav != null) {
                                speedDialToast = "Speed Dial $slotNum: Calling ${fav.name}..."
                                onSelectContactNumber(fav.phoneNumber)
                                onPlaceCall(fav.phoneNumber)
                            } else {
                                speedDialToast = "Speed Dial $slotNum is empty. Assign in Favorites."
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
                        Text(
                            text = if (simSlot == 1) "SIM 1 (Primary)" else "SIM 2 (Work / Roaming)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Switch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Call Actions Row: Standard SIM Call Button + WhatsApp Voice Call Button
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
                        .size(54.dp)
                        .testTag("whatsapp_call_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "WA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }

                // Standard SIM GSM Call Button
                FilledIconButton(
                    onClick = { onPlaceCall(number) },
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("dialer_call_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Place Call",
                        modifier = Modifier.size(32.dp)
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
}
