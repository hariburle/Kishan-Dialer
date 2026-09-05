package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FavoriteContact
import com.example.ui.components.AddFavoriteDialog
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactsScreen(
    favorites: List<FavoriteContact>,
    onSelectNumber: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onCreateRule: (String) -> Unit,
    onToggleFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onAddFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var contactForMultiCall by remember { mutableStateOf<DeviceContact?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Load device contacts on background thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = ContactHelper.fetchDeviceContacts(context)
            if (list.isEmpty()) {
                // Fallback default sample contacts if permissions or empty contact book
                val samples = listOf(
                    DeviceContact(
                        name = "Apartment Gate",
                        phoneNumber = "5550199",
                        label = "Intercom",
                        phoneNumbers = listOf(ContactPhoneNumber("5550199", "Intercom"))
                    ),
                    DeviceContact(
                        name = "Building Security",
                        phoneNumber = "+1 (555) 019-9000",
                        label = "Security",
                        phoneNumbers = listOf(
                            ContactPhoneNumber("+1 (555) 019-9000", "Security"),
                            ContactPhoneNumber("+1 (555) 019-9001", "Guard Desk")
                        )
                    ),
                    DeviceContact(
                        name = "Delivery Courier",
                        phoneNumber = "+1 (555) 456-7890",
                        label = "Work",
                        phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 456-7890", "Work"))
                    ),
                    DeviceContact(
                        name = "Dr. Robert Smith",
                        phoneNumber = "+1 (555) 345-6789",
                        label = "Office",
                        phoneNumbers = listOf(
                            ContactPhoneNumber("+1 (555) 345-6789", "Office"),
                            ContactPhoneNumber("+1 (555) 345-0000", "Mobile")
                        )
                    ),
                    DeviceContact(
                        name = "Home Landline",
                        phoneNumber = "+1 (800) 555-0199",
                        label = "Home",
                        phoneNumbers = listOf(ContactPhoneNumber("+1 (800) 555-0199", "Home"))
                    ),
                    DeviceContact(
                        name = "Mom",
                        phoneNumber = "+1 (555) 234-5678",
                        label = "Mobile",
                        phoneNumbers = listOf(
                            ContactPhoneNumber("+1 (555) 234-5678", "Mobile"),
                            ContactPhoneNumber("+1 (800) 555-0199", "Home")
                        )
                    ),
                    DeviceContact(
                        name = "Office IVR",
                        phoneNumber = "18005550100",
                        label = "Work",
                        phoneNumbers = listOf(ContactPhoneNumber("18005550100", "Work"))
                    ),
                    DeviceContact(
                        name = "Sarah Jenkins",
                        phoneNumber = "+1 (555) 890-1234",
                        label = "Mobile",
                        phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 890-1234", "Mobile"))
                    )
                )
                deviceContacts = samples
            } else {
                deviceContacts = list
            }
            isLoading = false
        }
    }

    // Filter contacts based on query
    val filteredContacts = remember(searchQuery, deviceContacts) {
        if (searchQuery.isBlank()) {
            deviceContacts.sortedBy { it.name.lowercase() }
        } else {
            val query = searchQuery.trim().lowercase()
            deviceContacts.filter { contact ->
                contact.name.lowercase().contains(query) ||
                contact.label.lowercase().contains(query) ||
                contact.phoneNumbers.any { it.number.contains(query) }
            }.sortedBy { it.name.lowercase() }
        }
    }

    // Group contacts by first letter A-Z, '#' for numbers/symbols
    val groupedContacts = remember(filteredContacts) {
        filteredContacts.groupBy { contact ->
            val firstChar = contact.name.trim().firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar in 'A'..'Z') firstChar else '#'
        }.toSortedMap()
    }

    val alphabet = remember { ('A'..'Z').toList() + listOf('#') }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("contacts_screen")
    ) {
        // Search bar & Add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("contacts_search_input"),
                placeholder = { Text("Search by name or number...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            FilledIconButton(
                onClick = { showAddCustomDialog = true },
                modifier = Modifier.size(48.dp).testTag("add_contact_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "New Contact",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Quick A-Z Alphabet Scroller Ribbon (iOS & Android Style)
        if (searchQuery.isBlank() && groupedContacts.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    alphabet.forEach { char ->
                        val hasItems = groupedContacts.containsKey(char)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable(enabled = hasItems) {
                                    scope.launch {
                                        var index = 0
                                        for ((key, items) in groupedContacts) {
                                            if (key == char) {
                                                listState.animateScrollToItem(index)
                                                break
                                            }
                                            index += items.size + 1
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char.toString(),
                                fontSize = 11.sp,
                                fontWeight = if (hasItems) FontWeight.Bold else FontWeight.Normal,
                                color = if (hasItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }

        // Contact List with Clean Group Headers
        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "No contacts found on device" else "No contacts match '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("contacts_list")
            ) {
                groupedContacts.forEach { (initial, contactsInGroup) ->
                    // Sticky Letter Header
                    item(key = "header_$initial") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = initial.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items(contactsInGroup, key = { it.contactId?.toString() ?: (it.name + "_" + it.phoneNumber) }) { contact ->
                        val isFav = favorites.any { fav ->
                            contact.phoneNumbers.any { it.number == fav.phoneNumber } || contact.phoneNumber == fav.phoneNumber
                        }

                        ContactRowItem(
                            contact = contact,
                            searchQuery = searchQuery,
                            isFavorite = isFav,
                            onItemClick = { onSelectNumber(contact.phoneNumber) },
                            onRequestCall = {
                                if (contact.phoneNumbers.size > 1) {
                                    contactForMultiCall = contact
                                } else {
                                    onCallNumber(contact.phoneNumber)
                                }
                            },
                            onCallDirect = onCallNumber,
                            onSelectNumber = onSelectNumber,
                            onSmsClick = { num ->
                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num"))
                                context.startActivity(smsIntent)
                            },
                            onCreateRule = { num -> onCreateRule(num) },
                            onToggleFavorite = {
                                onToggleFavorite(contact.name, contact.phoneNumber, contact.label, contact.photoUri)
                            }
                        )
                    }
                }
            }
        }
    }

    // Multi-Number Quick Call Dialog
    if (contactForMultiCall != null) {
        val currentContact = contactForMultiCall!!
        AlertDialog(
            onDismissRequest = { contactForMultiCall = null },
            title = {
                Text(
                    text = "Call ${currentContact.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose a number to call:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    currentContact.phoneNumbers.forEach { pn ->
                        Card(
                            onClick = {
                                val targetNum = pn.number
                                contactForMultiCall = null
                                onCallNumber(targetNum)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = pn.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = pn.number,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                FilledIconButton(
                                    onClick = {
                                        val targetNum = pn.number
                                        contactForMultiCall = null
                                        onCallNumber(targetNum)
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFF16A34A),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call ${pn.label}",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { contactForMultiCall = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddCustomDialog) {
        AddFavoriteDialog(
            initialNumber = searchQuery.filter { it.isDigit() || it == '+' },
            initialName = if (searchQuery.any { it.isLetter() }) searchQuery else "",
            onDismiss = { showAddCustomDialog = false },
            onSave = { name, number, label, photoUri ->
                onAddFavorite(name, number, label, photoUri)
                showAddCustomDialog = false
            },
            onPickFromContacts = { /* Already on contacts */ }
        )
    }
}

@Composable
private fun ContactRowItem(
    contact: DeviceContact,
    searchQuery: String,
    isFavorite: Boolean,
    onItemClick: () -> Unit,
    onRequestCall: () -> Unit,
    onCallDirect: (String) -> Unit,
    onSelectNumber: (String) -> Unit,
    onSmsClick: (String) -> Unit,
    onCreateRule: (String) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val rowContext = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }

    // Check if search query matches any specific number
    val matchedNumber = remember(searchQuery, contact) {
        if (searchQuery.isNotBlank() && searchQuery.any { it.isDigit() }) {
            val q = searchQuery.trim()
            contact.phoneNumbers.firstOrNull { it.number.contains(q) }?.number
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable { isExpanded = !isExpanded }
            .testTag("contact_item_${contact.phoneNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Contact Summary Row (Clean, High Density, No phone number clutter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Avatar + Contact Name + Clean Label Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (!contact.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = contact.photoUri,
                                contentDescription = contact.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Concise Subtitle: clean badge only, no raw number list
                        if (matchedNumber != null) {
                            Text(
                                text = "Match: $matchedNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (contact.phoneNumbers.size > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "${contact.phoneNumbers.size} numbers",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = "• " + contact.phoneNumbers.joinToString(", ") { it.label }.take(22),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = contact.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Compact Row Actions (Star, Quick Call, Expand/Collapse)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Star Favorite Toggle
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp).testTag("fav_toggle_${contact.phoneNumber}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Remove Favorite" else "Add Favorite",
                            tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Direct Quick Call (if 1 number calls directly, if >1 prompts user)
                    FilledIconButton(
                        onClick = onRequestCall,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Expand / Collapse Chevron
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expandable Multi-Number & Action Detail Panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // List all phone numbers with discrete action controls
                    contact.phoneNumbers.forEach { pn ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = pn.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = pn.number,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Send SMS
                                    IconButton(
                                        onClick = { onSmsClick(pn.number) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Message,
                                            contentDescription = "SMS",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // WhatsApp Call / Message (if international e.g. +91)
                                    if (ContactHelper.shouldSuggestWhatsApp(pn.number)) {
                                        FilledIconButton(
                                            onClick = { ContactHelper.launchWhatsAppCall(rowContext, pn.number) },
                                            modifier = Modifier.size(30.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = Color(0xFF25D366),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text(
                                                text = "WA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Copy number
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(pn.number))
                                            Toast.makeText(rowContext, "Copied ${pn.number}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy number",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Automation Rule shortcut
                                    IconButton(
                                        onClick = { onCreateRule(pn.number) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SmartToy,
                                            contentDescription = "Create Rule",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Direct Call
                                    FilledIconButton(
                                        onClick = { onCallDirect(pn.number) },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF16A34A),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call number",
                                            modifier = Modifier.size(16.dp)
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
