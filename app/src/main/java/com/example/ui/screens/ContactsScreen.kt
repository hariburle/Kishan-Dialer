package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FavoriteContact
import com.example.ui.components.AddFavoriteDialog
import com.example.util.ContactHelper
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

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Load device contacts on background thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = ContactHelper.fetchDeviceContacts(context)
            if (list.isEmpty()) {
                // Fallback default sample contacts if permissions or empty contact book
                val samples = listOf(
                    DeviceContact("Apartment Gate", "5550199", "Intercom"),
                    DeviceContact("Building Security", "+1 (555) 019-9000", "Security"),
                    DeviceContact("Delivery Courier", "+1 (555) 456-7890", "Work"),
                    DeviceContact("Dr. Robert Smith", "+1 (555) 345-6789", "Doctor"),
                    DeviceContact("Home Landline", "+1 (800) 555-0199", "Home"),
                    DeviceContact("Mom", "+1 (555) 234-5678", "Family"),
                    DeviceContact("Office IVR", "18005550100", "Work"),
                    DeviceContact("Sarah Jenkins", "+1 (555) 890-1234", "Mobile")
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
            deviceContacts.sortedBy { it.name }
        } else {
            val query = searchQuery.trim().lowercase()
            deviceContacts.filter {
                it.name.lowercase().contains(query) ||
                it.phoneNumber.contains(query) ||
                it.label.lowercase().contains(query)
            }.sortedBy { it.name }
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
                placeholder = { Text("Search contacts or dialer...") },
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
                                        // Find index of group
                                        var index = 0
                                        for ((key, items) in groupedContacts) {
                                            if (key == char) {
                                                listState.animateScrollToItem(index)
                                                break
                                            }
                                            index += items.size + 1 // +1 for header
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

        // Contact List with Group Headers
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

                    items(contactsInGroup, key = { it.phoneNumber + "_" + it.name }) { contact ->
                        val isFav = favorites.any { it.phoneNumber == contact.phoneNumber }
                        ContactRowItem(
                            contact = contact,
                            isFavorite = isFav,
                            onItemClick = { onSelectNumber(contact.phoneNumber) },
                            onCallClick = { onCallNumber(contact.phoneNumber) },
                            onSmsClick = {
                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phoneNumber}"))
                                context.startActivity(smsIntent)
                            },
                            onCreateRule = { onCreateRule(contact.phoneNumber) },
                            onToggleFavorite = {
                                onToggleFavorite(contact.name, contact.phoneNumber, contact.label, contact.photoUri)
                            }
                        )
                    }
                }
            }
        }
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
    isFavorite: Boolean,
    onItemClick: () -> Unit,
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit,
    onCreateRule: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val rowContext = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable { onItemClick() }
            .testTag("contact_item_${contact.phoneNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar & Name/Number Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = contact.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = contact.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Action Icons (Favorite Star, SMS, Call)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // One-tap Favorite Star (Android & iOS style)
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

                // Auto-Rule Creator Shortcut
                IconButton(
                    onClick = onCreateRule,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Create Rule",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // WhatsApp Call Button for international contacts (e.g. +91)
                if (ContactHelper.shouldSuggestWhatsApp(contact.phoneNumber)) {
                    FilledIconButton(
                        onClick = { ContactHelper.launchWhatsAppCall(rowContext, contact.phoneNumber) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "WA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // SMS Button
                IconButton(
                    onClick = onSmsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = "Send SMS",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Call Button
                FilledIconButton(
                    onClick = onCallClick,
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
            }
        }
    }
}
