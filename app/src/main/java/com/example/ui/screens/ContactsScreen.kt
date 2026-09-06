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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FavoriteContact
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactDetailsBottomSheet
import com.example.ui.components.MultiNumberCallDialog
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ContactSortBy { FIRST_NAME, LAST_NAME }
enum class ContactSortOrder { ASCENDING, DESCENDING }
enum class ContactSourceFilter { ALL, APP_ONLY, DEVICE }

private fun getContactFirstName(name: String): String {
    val trimmed = name.trim()
    return trimmed.split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotBlank() } ?: trimmed
}

private fun getContactLastName(name: String): String {
    val trimmed = name.trim()
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    return if (parts.size > 1) parts.last() else trimmed
}

@Composable
fun ContactsScreen(
    favorites: List<FavoriteContact>,
    onSelectNumber: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onCreateRule: (String) -> Unit,
    onToggleFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onAddFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onUpdateFavoriteNumber: (FavoriteContact, String, String) -> Unit = { _, _, _ -> },
    onAddNewContact: (name: String, number: String, label: String, destination: com.example.ui.components.ContactSaveDestination, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onSyncContactToGoogle: (DeviceContact) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var contactForMultiCall by remember { mutableStateOf<DeviceContact?>(null) }
    var favoriteContactForMultiCall by remember { mutableStateOf<FavoriteContact?>(null) }
    var contactForDetailsSheet by remember { mutableStateOf<DeviceContact?>(null) }
    var sortBy by remember { mutableStateOf(ContactSortBy.FIRST_NAME) }
    var sortOrder by remember { mutableStateOf(ContactSortOrder.ASCENDING) }
    var sourceFilter by remember { mutableStateOf(ContactSourceFilter.ALL) }
    var showFavoritesSection by rememberSaveable { mutableStateOf(false) }

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

    // Merge contacts: Include any favorites saved as App-Only (not present on device)
    val allCombinedContacts = remember(deviceContacts, favorites) {
        val list = deviceContacts.toMutableList()
        favorites.forEach { fav ->
            val normF = fav.phoneNumber.replace(Regex("[^0-9+]"), "")
            val alreadyInDevice = list.any { dc ->
                dc.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normF } ||
                dc.phoneNumber.replace(Regex("[^0-9+]"), "") == normF ||
                dc.name.equals(fav.name, ignoreCase = true)
            }
            if (!alreadyInDevice) {
                list.add(
                    DeviceContact(
                        name = fav.name,
                        phoneNumber = fav.phoneNumber,
                        label = fav.label,
                        photoUri = fav.photoUri,
                        phoneNumbers = listOf(ContactPhoneNumber(fav.phoneNumber, fav.label)),
                        isAppOnly = true
                    )
                )
            }
        }
        list
    }

    // Filter contacts based on sourceFilter and search query
    val filteredContacts = remember(searchQuery, allCombinedContacts, sourceFilter) {
        val baseList = when (sourceFilter) {
            ContactSourceFilter.ALL -> allCombinedContacts
            ContactSourceFilter.APP_ONLY -> allCombinedContacts.filter { it.isAppOnly }
            ContactSourceFilter.DEVICE -> allCombinedContacts.filter { !it.isAppOnly }
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val query = searchQuery.trim().lowercase()
            baseList.filter { contact ->
                contact.name.lowercase().contains(query) ||
                (contact.nickname?.lowercase()?.contains(query) == true) ||
                contact.label.lowercase().contains(query) ||
                contact.phoneNumbers.any { it.number.contains(query) }
            }
        }
    }

    // Sort contacts by First Name or Last Name, Ascending or Descending
    val sortedContacts = remember(filteredContacts, sortBy, sortOrder) {
        val comparator = when (sortBy) {
            ContactSortBy.FIRST_NAME -> compareBy<DeviceContact> { getContactFirstName(it.name).lowercase() }
                .thenBy { it.name.lowercase() }
            ContactSortBy.LAST_NAME -> compareBy<DeviceContact> { getContactLastName(it.name).lowercase() }
                .thenBy { it.name.lowercase() }
        }
        if (sortOrder == ContactSortOrder.ASCENDING) {
            filteredContacts.sortedWith(comparator)
        } else {
            filteredContacts.sortedWith(comparator.reversed())
        }
    }

    // Group contacts by initial letter according to current sort criteria
    val groupedContacts = remember(sortedContacts, sortBy, sortOrder) {
        val groups = sortedContacts.groupBy { contact ->
            val key = when (sortBy) {
                ContactSortBy.FIRST_NAME -> getContactFirstName(contact.name)
                ContactSortBy.LAST_NAME -> getContactLastName(contact.name)
            }
            val firstChar = key.trim().firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar in 'A'..'Z') firstChar else '#'
        }
        if (sortOrder == ContactSortOrder.ASCENDING) {
            groups.toSortedMap(compareBy { if (it == '#') "ZZZ" else it.toString() })
        } else {
            groups.toSortedMap(compareByDescending { if (it == '#') "" else it.toString() })
        }
    }

    val alphabet = remember(sortOrder) {
        if (sortOrder == ContactSortOrder.ASCENDING) {
            ('A'..'Z').toList() + listOf('#')
        } else {
            listOf('#') + ('Z' downTo 'A').toList()
        }
    }

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

        // Source Filter Chips: All, App Only, Google / Device
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = sourceFilter == ContactSourceFilter.ALL,
                onClick = { sourceFilter = ContactSourceFilter.ALL },
                label = { Text("All (${allCombinedContacts.size})", fontSize = 12.sp) }
            )
            val appOnlyCount = allCombinedContacts.count { it.isAppOnly }
            FilterChip(
                selected = sourceFilter == ContactSourceFilter.APP_ONLY,
                onClick = { sourceFilter = ContactSourceFilter.APP_ONLY },
                label = { Text("App Only ($appOnlyCount)", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
            val deviceCount = allCombinedContacts.count { !it.isAppOnly }
            FilterChip(
                selected = sourceFilter == ContactSourceFilter.DEVICE,
                onClick = { sourceFilter = ContactSourceFilter.DEVICE },
                label = { Text("Google / Device ($deviceCount)", fontSize = 12.sp) }
            )
        }

        // Sorting controls bar: First Name / Last Name & Up / Down
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${sortedContacts.size} contacts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sort By: First Name vs Last Name toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            sortBy = if (sortBy == ContactSortBy.FIRST_NAME) ContactSortBy.LAST_NAME else ContactSortBy.FIRST_NAME
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (sortBy == ContactSortBy.FIRST_NAME) "Sort: First Name" else "Sort: Last Name",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Sort Order: Ascending (Up) vs Descending (Down)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            sortOrder = if (sortOrder == ContactSortOrder.ASCENDING) ContactSortOrder.DESCENDING else ContactSortOrder.ASCENDING
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (sortOrder == ContactSortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (sortOrder == ContactSortOrder.ASCENDING) "A → Z (Up)" else "Z → A (Down)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
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
                // Collapsible Pinned Favorites on Top of Contacts Panel (Closed by Default)
                if (favorites.isNotEmpty() && searchQuery.isBlank()) {
                    item(key = "top_favorites_section") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFavoritesSection = !showFavoritesSection }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Favorites (${favorites.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = if (showFavoritesSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (showFavoritesSection) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = if (showFavoritesSection) "Tap to call • Long-press numbers" else "Tap to expand",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(
                                visible = showFavoritesSection,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(favorites, key = { "top_fav_${it.id}" }) { fav ->
                                            FavoriteTopChip(
                                                favorite = fav,
                                                onTap = {
                                                    val normNum = fav.phoneNumber.replace(Regex("[^0-9+]"), "")
                                                    val matched = deviceContacts.firstOrNull { dc ->
                                                        dc.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normNum } ||
                                                        dc.phoneNumber.replace(Regex("[^0-9+]"), "") == normNum ||
                                                        dc.name.equals(fav.name, ignoreCase = true) ||
                                                        (!dc.nickname.isNullOrBlank() && dc.nickname.equals(fav.name, ignoreCase = true))
                                                    } ?: DeviceContact(
                                                        name = fav.name,
                                                        phoneNumber = fav.phoneNumber,
                                                        label = fav.label,
                                                        photoUri = fav.photoUri,
                                                        phoneNumbers = listOf(ContactPhoneNumber(fav.phoneNumber, fav.label))
                                                    )
                                                    contactForDetailsSheet = matched
                                                },
                                                onLongPress = {
                                                    val normNum = fav.phoneNumber.replace(Regex("[^0-9+]"), "")
                                                    val matched = deviceContacts.firstOrNull { dc ->
                                                        dc.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normNum } ||
                                                        dc.phoneNumber.replace(Regex("[^0-9+]"), "") == normNum ||
                                                        dc.name.equals(fav.name, ignoreCase = true) ||
                                                        (!dc.nickname.isNullOrBlank() && dc.nickname.equals(fav.name, ignoreCase = true))
                                                    } ?: DeviceContact(
                                                        name = fav.name,
                                                        phoneNumber = fav.phoneNumber,
                                                        label = fav.label,
                                                        photoUri = fav.photoUri,
                                                        phoneNumbers = listOf(ContactPhoneNumber(fav.phoneNumber, fav.label))
                                                    )
                                                    contactForDetailsSheet = matched
                                                }
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

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
                            val normF = fav.phoneNumber.replace(Regex("[^0-9+]"), "")
                            contact.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normF } ||
                            contact.phoneNumber.replace(Regex("[^0-9+]"), "") == normF ||
                            fav.name.equals(contact.name, ignoreCase = true) ||
                            (!contact.nickname.isNullOrBlank() && fav.name.equals(contact.nickname, ignoreCase = true))
                        }

                        ContactRowItem(
                            contact = contact,
                            searchQuery = searchQuery,
                            isFavorite = isFav,
                            onItemClick = { contactForDetailsSheet = contact },
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
                                val favName = contact.nickname?.ifBlank { null } ?: contact.name
                                onToggleFavorite(favName, contact.phoneNumber, contact.label, contact.photoUri)
                            },
                            onSyncToGoogle = {
                                onSyncContactToGoogle(contact)
                            }
                        )
                    }
                }
            }
        }
    }

    // Multi-Number Quick Call & Favorite Selection Dialog
    if (contactForMultiCall != null) {
        val currentContact = contactForMultiCall!!
        MultiNumberCallDialog(
            contactName = currentContact.name,
            phoneNumbers = currentContact.phoneNumbers,
            defaultNumber = favoriteContactForMultiCall?.phoneNumber ?: currentContact.phoneNumber,
            titlePrefix = if (favoriteContactForMultiCall != null) "Favorite Contact Numbers" else "Select Number to Call",
            onSelectNumberToCall = { chosenNumber ->
                onCallNumber(chosenNumber)
                contactForMultiCall = null
                favoriteContactForMultiCall = null
            },
            onSetAsFavoriteNumber = { newNum, newLabel ->
                val fav = favoriteContactForMultiCall ?: favorites.firstOrNull { f ->
                    val normF = f.phoneNumber.replace(Regex("[^0-9+]"), "")
                    currentContact.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normF } ||
                    f.name.equals(currentContact.name, ignoreCase = true) ||
                    (!currentContact.nickname.isNullOrBlank() && f.name.equals(currentContact.nickname, ignoreCase = true))
                }
                val favName = currentContact.nickname?.ifBlank { null } ?: currentContact.name
                if (fav != null) {
                    onUpdateFavoriteNumber(fav, newNum, newLabel)
                } else {
                    onAddFavorite(favName, newNum, newLabel, currentContact.photoUri)
                }
                contactForMultiCall = null
                favoriteContactForMultiCall = null
            },
            onDismiss = {
                contactForMultiCall = null
                favoriteContactForMultiCall = null
            }
        )
    }

    // Android Phone Dialer Contact Details Card Bottom Sheet
    if (contactForDetailsSheet != null) {
        val detailContact = contactForDetailsSheet!!
        val matchedFav = favorites.firstOrNull { fav ->
            val normF = fav.phoneNumber.replace(Regex("[^0-9+]"), "")
            detailContact.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normF } ||
            fav.name.equals(detailContact.name, ignoreCase = true) ||
            (!detailContact.nickname.isNullOrBlank() && fav.name.equals(detailContact.nickname, ignoreCase = true))
        }
        val isFav = matchedFav != null

        ContactDetailsBottomSheet(
            contact = detailContact,
            favoriteContact = matchedFav,
            isFavorite = isFav,
            onCallNumber = { num ->
                onCallNumber(num)
            },
            onSelectInDialer = { num ->
                onSelectNumber(num)
            },
            onToggleFavorite = {
                val favName = detailContact.nickname?.ifBlank { null } ?: detailContact.name
                onToggleFavorite(favName, matchedFav?.phoneNumber ?: detailContact.phoneNumber, matchedFav?.label ?: detailContact.label, detailContact.photoUri)
            },
            onSetAsDefaultNumber = { newNum, newLabel ->
                if (matchedFav != null) {
                    onUpdateFavoriteNumber(matchedFav, newNum, newLabel)
                } else {
                    val favName = detailContact.nickname?.ifBlank { null } ?: detailContact.name
                    onAddFavorite(favName, newNum, newLabel, detailContact.photoUri)
                }
            },
            onClearDefaultNumber = {
                if (matchedFav != null) {
                    onToggleFavorite(matchedFav.name, matchedFav.phoneNumber, matchedFav.label, matchedFav.photoUri)
                }
            },
            onCreateRule = { num ->
                onCreateRule(num)
            },
            onSyncToGoogle = {
                onSyncContactToGoogle(detailContact)
            },
            onDismiss = {
                contactForDetailsSheet = null
            }
        )
    }

    if (showAddCustomDialog) {
        com.example.ui.components.CreateContactDialog(
            initialName = if (searchQuery.any { it.isLetter() }) searchQuery else "",
            initialNumber = searchQuery.filter { it.isDigit() || it == '+' },
            onDismiss = { showAddCustomDialog = false },
            onSave = { name, number, label, destination, addToFav ->
                onAddNewContact(name, number, label, destination, addToFav)
                showAddCustomDialog = false
                scope.launch(Dispatchers.IO) {
                    val list = ContactHelper.fetchDeviceContacts(context)
                    if (list.isNotEmpty()) {
                        deviceContacts = list
                    }
                }
            }
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
    onToggleFavorite: () -> Unit,
    onSyncToGoogle: () -> Unit = {}
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
            .clickable { onItemClick() }
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

                        if (!contact.nickname.isNullOrBlank()) {
                            Text(
                                text = contact.nickname,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

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

                        if (contact.isAppOnly) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "App Only",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Compact Row Actions (Sync, Star, Quick Call, Expand/Collapse)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (contact.isAppOnly) {
                        IconButton(
                            onClick = onSyncToGoogle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Sync to Google Contacts",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

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

                                    // WhatsApp Call / Message
                                    FilledIconButton(
                                        onClick = { ContactHelper.launchWhatsAppCall(rowContext, pn.number) },
                                        modifier = Modifier.size(30.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF25D366),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "WhatsApp",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
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

                    if (contact.isAppOnly) {
                        FilledTonalButton(
                            onClick = onSyncToGoogle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sync to Google Contacts")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteTopChip(
    favorite: FavoriteContact,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (!favorite.photoUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = favorite.photoUri,
                        contentDescription = favorite.name,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = favorite.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                // Star badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Text(
                text = favorite.nickname ?: favorite.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = favorite.phoneNumber,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (favorite.speedDialSlot != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = "#${favorite.speedDialSlot}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF16A34A).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(9.dp),
                            tint = Color(0xFF16A34A)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Call",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
        }
    }
}

