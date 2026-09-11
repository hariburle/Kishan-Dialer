package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.components.ContactDetailsBottomSheet
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.MultiNumberCallDialog
import com.example.ui.components.WhatsAppIcon
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ContactSortBy { FIRST_NAME, LAST_NAME }
enum class ContactSortOrder { ASCENDING, DESCENDING }
enum class ContactSourceFilter { ALL, APP_ONLY, DEVICE }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    favorites: List<FavoriteContact>,
    onCallNumber: (String) -> Unit,
    onSelectNumber: (String) -> Unit,
    onToggleFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onDeleteFavorite: (FavoriteContact) -> Unit = {},
    onAddFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit = { _, _, _, _ -> },
    onEditFavorite: (FavoriteContact, String?) -> Unit = { _, _ -> },
    onUpdateFavoriteNumber: (FavoriteContact, String, String) -> Unit = { _, _, _ -> },
    onCreateRule: (String) -> Unit,
    onAddNewContact: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateContact: (oldNumber: String, name: String, number: String, label: String, nickname: String?) -> Unit = { _, _, _, _, _ -> },
    onSyncContactToPhone: (DeviceContact) -> Unit = {},
    onSyncAllAppContactsToDevice: () -> Unit = {},
    deviceContacts: List<DeviceContact> = emptyList(),
    onRefreshContacts: () -> Unit = {},
    onPlaceWhatsAppCall: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(ContactSortBy.FIRST_NAME) }
    var sortOrder by remember { mutableStateOf(ContactSortOrder.ASCENDING) }
    var sourceFilter by remember { mutableStateOf(ContactSourceFilter.ALL) }
    var showFavoritesSection by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var contactForDetailsSheet by remember { mutableStateOf<DeviceContact?>(null) }
    var contactForMultiCall by remember { mutableStateOf<DeviceContact?>(null) }
    var favoriteContactForMultiCall by remember { mutableStateOf<FavoriteContact?>(null) }

    var localContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    val effectiveContacts = if (deviceContacts.isNotEmpty()) deviceContacts else localContacts

    LaunchedEffect(Unit) {
        if (deviceContacts.isEmpty()) {
            withContext(Dispatchers.IO) {
                localContacts = ContactHelper.fetchDeviceContacts(context)
            }
        }
    }

    // Keep contactForDetailsSheet in sync if contact is edited in system contacts app
    LaunchedEffect(effectiveContacts) {
        val current = contactForDetailsSheet ?: return@LaunchedEffect
        val updated = effectiveContacts.firstOrNull { dc ->
            (current.contactId != null && dc.contactId == current.contactId) ||
            dc.phoneNumber == current.phoneNumber ||
            dc.phoneNumbers.any { pn -> current.phoneNumbers.any { cpn -> cpn.number == pn.number } } ||
            dc.name.equals(current.name, ignoreCase = true)
        }
        if (updated != null) {
            contactForDetailsSheet = updated
        }
    }

    // Filter by source and search query
    val filteredContacts = remember(effectiveContacts, searchQuery, sourceFilter) {
        var list = effectiveContacts

        // Source Filter: All, App Only, or Google / Device
        when (sourceFilter) {
            ContactSourceFilter.ALL -> {}
            ContactSourceFilter.APP_ONLY -> {
                list = list.filter { it.isAppOnly }
            }
            ContactSourceFilter.DEVICE -> {
                list = list.filter { !it.isAppOnly }
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            val qLower = q.lowercase()
            list = list.filter {
                it.name.lowercase().contains(qLower) ||
                (it.nickname != null && it.nickname.lowercase().contains(qLower)) ||
                ContactHelper.matchesNumberQuery(it.phoneNumber, q) ||
                it.phoneNumbers.any { pn ->
                    ContactHelper.matchesNumberQuery(pn.number, q) ||
                    pn.label.lowercase().contains(qLower)
                }
            }
        }
        list
    }

    // Sort contacts
    val sortedContacts = remember(filteredContacts, sortBy, sortOrder) {
        val sorted = filteredContacts.sortedWith(Comparator { c1, c2 ->
            val name1 = if (sortBy == ContactSortBy.FIRST_NAME) {
                c1.nickname?.ifBlank { null } ?: c1.name
            } else {
                val parts = (c1.nickname?.ifBlank { null } ?: c1.name).trim().split("\\s+".toRegex())
                parts.lastOrNull() ?: c1.name
            }
            val name2 = if (sortBy == ContactSortBy.FIRST_NAME) {
                c2.nickname?.ifBlank { null } ?: c2.name
            } else {
                val parts = (c2.nickname?.ifBlank { null } ?: c2.name).trim().split("\\s+".toRegex())
                parts.lastOrNull() ?: c2.name
            }
            val cmp = name1.compareTo(name2, ignoreCase = true)
            if (cmp != 0) cmp else c1.phoneNumber.compareTo(c2.phoneNumber)
        })
        if (sortOrder == ContactSortOrder.ASCENDING) sorted else sorted.reversed()
    }

    // Group contacts alphabetically by initial
    val groupedContacts = remember(sortedContacts) {
        sortedContacts.groupBy { contact ->
            val displayName = contact.nickname?.ifBlank { null } ?: contact.name
            val firstChar = displayName.trim().firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar in 'A'..'Z') firstChar else '#'
        }.let { groups ->
            if (sortOrder == ContactSortOrder.ASCENDING) {
                groups.toSortedMap(compareBy { if (it == '#') "ZZZ" else it.toString() })
            } else {
                groups.toSortedMap(compareByDescending { if (it == '#') "" else it.toString() })
            }
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
                placeholder = { Text("Search by name or number") },
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
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_contact_button"),
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

        val appOnlyCount = effectiveContacts.count { it.isAppOnly }
        val deviceCount = effectiveContacts.count { !it.isAppOnly }

        // Source Filter Chips: All, App Only, Phone Contacts
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
                label = { Text("All (${effectiveContacts.size})", fontSize = 12.sp) }
            )
            FilterChip(
                selected = sourceFilter == ContactSourceFilter.APP_ONLY,
                onClick = { sourceFilter = ContactSourceFilter.APP_ONLY },
                label = { Text("App Only ($appOnlyCount)", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
            FilterChip(
                selected = sourceFilter == ContactSourceFilter.DEVICE,
                onClick = { sourceFilter = ContactSourceFilter.DEVICE },
                label = { Text("Phone Contacts ($deviceCount)", fontSize = 12.sp) }
            )
        }

        if (sourceFilter == ContactSourceFilter.APP_ONLY && appOnlyCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync All App Contacts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Move $appOnlyCount contact${if (appOnlyCount > 1) "s" else ""} to phone's default contacts app",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = {
                            onSyncAllAppContactsToDevice()
                            Toast.makeText(context, "Synced $appOnlyCount contact${if (appOnlyCount > 1) "s" else ""} to Phone Contacts!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Sync All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Sync All", fontSize = 12.sp)
                    }
                }
            }
        }

        // Sorting controls bar
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
                            imageVector = Icons.AutoMirrored.Filled.Sort,
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
                            text = if (sortOrder == ContactSortOrder.ASCENDING) "A → Z" else "Z → A",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Contact List with Clean Group Headers and Vertical Right-Side A-Z Strip
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
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
                        .padding(end = 28.dp)
                        .testTag("contacts_list")
                ) {
                    // Collapsible Pinned Favorites on Top of Contacts Panel
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
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showFavoritesSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        stickyHeader(key = "header_$initial") {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = initial.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
                                    val normContactNum = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                                    val matchedFav = favorites.firstOrNull { f -> f.phoneNumber.replace(Regex("[^0-9+]"), "") == normContactNum }
                                    if (contact.phoneNumbers.size > 1) {
                                        contactForMultiCall = contact
                                        favoriteContactForMultiCall = matchedFav
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
                                onPlaceWhatsAppCall = onPlaceWhatsAppCall,
                                onSyncToPhone = {
                                    onSyncContactToPhone(contact)
                                }
                            )
                        }
                    }
                }
            }

            // Vertical A-Z Strip on Right Side
            if (searchQuery.isBlank() && groupedContacts.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .width(26.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        alphabet.forEach { char ->
                            val hasItems = groupedContacts.containsKey(char)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
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
                                    fontSize = 10.sp,
                                    fontWeight = if (hasItems) FontWeight.Bold else FontWeight.Normal,
                                    color = if (hasItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
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

    // Contact Details Bottom Sheet
    if (contactForDetailsSheet != null) {
        val detailContact = contactForDetailsSheet!!
        val matchedFav = favorites.firstOrNull { fav ->
            val favDigits = fav.phoneNumber.filter { it.isDigit() }.takeLast(10)
            val phoneMatch = if (favDigits.length >= 7) {
                detailContact.phoneNumbers.any { it.number.filter { c -> c.isDigit() }.takeLast(10) == favDigits } ||
                detailContact.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == favDigits
            } else false
            phoneMatch ||
            fav.name.equals(detailContact.name.trim(), ignoreCase = true) ||
            (!detailContact.nickname.isNullOrBlank() && fav.name.equals(detailContact.nickname!!.trim(), ignoreCase = true))
        }
        val isFav = matchedFav != null

        ContactDetailsBottomSheet(
            contact = detailContact,
            favoriteContact = matchedFav,
            isFavorite = isFav,
            onCallNumber = { num ->
                onCallNumber(num)
                contactForDetailsSheet = null
            },
            onSelectInDialer = { num ->
                onSelectNumber(num)
                contactForDetailsSheet = null
            },
            onToggleFavorite = {
                if (matchedFav != null) {
                    onDeleteFavorite(matchedFav)
                } else {
                    val favName = detailContact.nickname?.ifBlank { null } ?: detailContact.name
                    val defNum = detailContact.phoneNumber.ifBlank { detailContact.phoneNumbers.firstOrNull()?.number ?: "" }
                    val defLabel = detailContact.label.ifBlank { detailContact.phoneNumbers.firstOrNull()?.label ?: "Mobile" }
                    onAddFavorite(favName, defNum, defLabel, detailContact.photoUri)
                }
            },
            onSetAsDefaultNumber = { num, label ->
                if (matchedFav != null) {
                    onUpdateFavoriteNumber(matchedFav, num, label)
                } else {
                    val favName = detailContact.nickname?.ifBlank { null } ?: detailContact.name
                    onAddFavorite(favName, num, label, detailContact.photoUri)
                }
            },
            onClearDefaultNumber = {
                if (matchedFav != null) {
                    onDeleteFavorite(matchedFav)
                }
            },
            onCreateRule = { num ->
                onCreateRule(num)
                contactForDetailsSheet = null
            },
            onSyncToPhone = {
                onSyncContactToPhone(detailContact)
            },
            onEditContact = { name, number, label, nickname ->
                onUpdateContact(detailContact.phoneNumber, name, number, label, nickname)
                contactForDetailsSheet = null
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
    onPlaceWhatsAppCall: (String) -> Unit = {},
    onSyncToPhone: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }

    val matchedNumber = remember(searchQuery, contact) {
        if (searchQuery.isNotBlank() && searchQuery.any { it.isDigit() }) {
            val q = searchQuery.trim()
            contact.phoneNumbers.firstOrNull { ContactHelper.matchesNumberQuery(it.number, q) }?.number ?: if (ContactHelper.matchesNumberQuery(contact.phoneNumber, q)) contact.phoneNumber else null
        } else null
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onItemClick() }
            .testTag("contact_item_${contact.name}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (!contact.photoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = contact.photoUri,
                            contentDescription = contact.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    if (isFavorite) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorite",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }

                // Name & Phone / Subtitle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = contact.nickname?.ifBlank { null } ?: contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (contact.isAppOnly) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "App",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    val subtitleText = matchedNumber ?: if (contact.phoneNumbers.isNotEmpty()) {
                        "${contact.phoneNumbers.first().number} (${contact.phoneNumbers.first().label})"
                    } else {
                        contact.phoneNumber
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onRequestCall() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand numbers",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expanded Phone Numbers List (with inline actions per number)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    val numbersToDisplay = if (contact.phoneNumbers.isNotEmpty()) contact.phoneNumbers else listOf(ContactPhoneNumber(contact.phoneNumber, contact.label))

                    numbersToDisplay.forEach { pn ->
                        val isMobile = pn.label.equals("Mobile", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pn.number,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pn.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 1. Copy Number (Far Left)
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(pn.number))
                                        Toast.makeText(context, "Copied ${pn.number}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Number",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (isMobile) {
                                    // 2. WhatsApp Chat (2nd from Left)
                                    IconButton(
                                        onClick = {
                                            ContactHelper.launchWhatsAppMessage(context, pn.number)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "WhatsApp Chat",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // 3. WhatsApp Call (Middle)
                                    IconButton(
                                        onClick = {
                                            onPlaceWhatsAppCall(pn.number)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        WhatsAppIcon(modifier = Modifier.size(20.dp))
                                    }
                                }

                                // 4. SMS Message (2nd from Right)
                                IconButton(
                                    onClick = { onSmsClick(pn.number) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Message,
                                        contentDescription = "SMS Text",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // 5. Phone Call (Far Right - Closest to thumb resting position!)
                                IconButton(
                                    onClick = { onCallDirect(pn.number) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF16A34A).copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "Phone Call",
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(18.dp)
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
        }
    }
}
