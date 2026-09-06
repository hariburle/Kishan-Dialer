package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FavoriteContact
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactDetailsBottomSheet
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.EditFavoriteDialog
import com.example.ui.components.MultiNumberCallDialog
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteContact>,
    onSelectNumber: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onCreateRule: (String) -> Unit,
    onDeleteFavorite: (FavoriteContact) -> Unit,
    onAddFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onAssignSpeedDial: (FavoriteContact, Int) -> Unit,
    onMoveFavorite: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onEditFavorite: (FavoriteContact, String?) -> Unit = { _, _ -> },
    onUpdateFavoriteNumber: (FavoriteContact, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var speedDialTargetContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var editTargetContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var isConfigureMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var multiNumberContactToCall by remember { mutableStateOf<DeviceContact?>(null) }
    var favoriteContactToCall by remember { mutableStateOf<FavoriteContact?>(null) }
    var contactDetailsTarget by remember { mutableStateOf<Pair<DeviceContact, FavoriteContact?>?>(null) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            deviceContacts = ContactHelper.fetchDeviceContacts(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_screen")
    ) {
        // Header info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConfigureMode) "Configure" else "Favorites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isConfigureMode) "Reorder & manage" else "Quick dial",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Configure / Done Toggle Icon
                if (favorites.isNotEmpty()) {
                    FilledIconButton(
                        onClick = { isConfigureMode = !isConfigureMode },
                        modifier = Modifier.size(38.dp).testTag("fav_screen_configure_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isConfigureMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isConfigureMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (isConfigureMode) Icons.Default.Check else Icons.Default.Tune,
                            contentDescription = if (isConfigureMode) "Done Configuring" else "Configure Favorites",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isConfigureMode) {
                    // Search Contact Book toggle
                    FilledIconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        modifier = Modifier.size(38.dp).testTag("fav_screen_search_contacts_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (isSearchActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Contacts",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Secondary action: manual number entry dialog
                    FilledIconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.size(38.dp).testTag("fav_screen_add_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Number",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Real-time Search Field to search contacts and view all their numbers
        if (isSearchActive || searchQuery.isNotBlank()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search contacts...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = {
                        searchQuery = ""
                        isSearchActive = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close Search",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("favorites_search_field")
            )
        }

        if (searchQuery.isNotBlank()) {
            // Live Search Results across Contacts & Favorites showing all phone numbers
            val queryClean = searchQuery.trim().lowercase()
            val filteredContacts = remember(searchQuery, deviceContacts, favorites) {
                deviceContacts.filter { dc ->
                    dc.name.lowercase().contains(queryClean) ||
                    dc.phoneNumber.contains(queryClean) ||
                    dc.phoneNumbers.any { it.number.contains(queryClean) }
                }
            }

            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No contacts found matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredContacts, key = { "${it.contactId}_${it.phoneNumber}_${it.name}" }) { contact ->
                        val isFav = favorites.any { fav ->
                            fav.name.equals(contact.name, ignoreCase = true) ||
                            fav.phoneNumber == contact.phoneNumber
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = contact.name.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = contact.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isFav) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Favorite",
                                                        tint = Color(0xFFF59E0B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${contact.phoneNumbers.size.coerceAtLeast(1)} numbers available",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // List each phone number with direct call button
                                val numbersToShow = if (contact.phoneNumbers.isNotEmpty()) {
                                    contact.phoneNumbers
                                } else {
                                    listOf(com.example.util.ContactPhoneNumber(contact.phoneNumber, contact.label))
                                }

                                numbersToShow.forEach { pn ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onCallNumber(pn.number)
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
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
                                            onClick = { onCallNumber(pn.number) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "Call ${pn.number}",
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
        } else if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = "No Favorites Added Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Star contacts in your Contact Book or tap below to add instant VIP speed dial shortcuts.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = { showContactPicker = true },
                        modifier = Modifier.testTag("empty_add_favorite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick From Contacts")
                    }
                }
            }
        } else {
            // Dynamically calculate grid columns and card sizing
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_grid_container")
            ) {
                val availableWidth = maxWidth
                val columnsCount = if (availableWidth >= 600.dp) 3 else 2
                val isCompact = false

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("favorites_grid")
                ) {
                    itemsIndexed(favorites, key = { _, it -> it.id }) { index, contact ->
                        FavoriteGridCard(
                            contact = contact,
                            isCompact = isCompact,
                            isConfigureMode = isConfigureMode,
                            canMoveUp = index > 0,
                            canMoveDown = index < favorites.size - 1,
                            onMoveUp = { onMoveFavorite(index, index - 1) },
                            onMoveDown = { onMoveFavorite(index, index + 1) },
                            onCall = {
                                onCallNumber(contact.phoneNumber)
                            },
                            onLongClick = {
                                val normNum = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                                val matched = deviceContacts.firstOrNull { dc ->
                                    dc.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normNum } ||
                                    dc.phoneNumber.replace(Regex("[^0-9+]"), "") == normNum ||
                                    dc.name.equals(contact.name, ignoreCase = true)
                                } ?: DeviceContact(
                                    name = contact.name,
                                    phoneNumber = contact.phoneNumber,
                                    label = contact.label,
                                    photoUri = contact.photoUri,
                                    phoneNumbers = listOf(ContactPhoneNumber(contact.phoneNumber, contact.label))
                                )
                                contactDetailsTarget = Pair(matched, contact)
                            },
                            onSelect = { onSelectNumber(contact.phoneNumber) },
                            onCreateRule = { onCreateRule(contact.phoneNumber) },
                            onEdit = { editTargetContact = contact },
                            onDelete = { onDeleteFavorite(contact) },
                            onSpeedDialClick = { speedDialTargetContact = contact }
                        )
                    }
                }
            }
        }
    }

    // Edit Favorite dialog
    if (editTargetContact != null) {
        EditFavoriteDialog(
            contact = editTargetContact!!,
            onDismiss = { editTargetContact = null },
            onSave = { updatedContact, newNickname ->
                onEditFavorite(updatedContact, newNickname)
                editTargetContact = null
            }
        )
    }

    // Add Favorite dialog
    if (showAddDialog) {
        AddFavoriteDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, number, label, photoUri ->
                onAddFavorite(name, number, label, photoUri)
                showAddDialog = false
            },
            onPickFromContacts = {
                showAddDialog = false
                showContactPicker = true
            }
        )
    }

    // Multi-Number Call Confirmation Dialog
    if (multiNumberContactToCall != null) {
        val contact = multiNumberContactToCall!!
        MultiNumberCallDialog(
            contactName = contact.name,
            phoneNumbers = contact.phoneNumbers,
            defaultNumber = favoriteContactToCall?.phoneNumber ?: contact.phoneNumber,
            titlePrefix = "Favorite Contact Numbers",
            onSelectNumberToCall = { chosenNumber ->
                onCallNumber(chosenNumber)
            },
            onSetAsFavoriteNumber = { newNum, newLabel ->
                favoriteContactToCall?.let { fav ->
                    onUpdateFavoriteNumber(fav, newNum, newLabel)
                }
            },
            onSearchOtherContacts = {
                isSearchActive = true
            },
            onDismiss = {
                multiNumberContactToCall = null
                favoriteContactToCall = null
            }
        )
    }

    // Android Phone Dialer Contact Details Card Bottom Sheet
    if (contactDetailsTarget != null) {
        val (matchedContact, favContactInitial) = contactDetailsTarget!!
        val favContact = favContactInitial?.let { f ->
            favorites.find { it.id == f.id } ?: f
        }
        ContactDetailsBottomSheet(
            contact = matchedContact,
            favoriteContact = favContact,
            isFavorite = favContact != null,
            onCallNumber = { num ->
                onCallNumber(num)
            },
            onSelectInDialer = { num ->
                onSelectNumber(num)
            },
            onToggleFavorite = {
                if (favContact != null) {
                    onDeleteFavorite(favContact)
                } else {
                    onAddFavorite(matchedContact.name, matchedContact.phoneNumber, matchedContact.label, matchedContact.photoUri)
                }
            },
            onSetAsDefaultNumber = { newNum, newLabel ->
                if (favContact != null) {
                    onUpdateFavoriteNumber(favContact, newNum, newLabel)
                    contactDetailsTarget = Pair(matchedContact, favContact.copy(phoneNumber = newNum, label = newLabel))
                } else {
                    onAddFavorite(matchedContact.name, newNum, newLabel, matchedContact.photoUri)
                }
            },
            onClearDefaultNumber = {
                if (favContact != null) {
                    onDeleteFavorite(favContact)
                    contactDetailsTarget = Pair(matchedContact, null)
                }
            },
            onCreateRule = { num ->
                onCreateRule(num)
            },
            onDismiss = {
                contactDetailsTarget = null
            }
        )
    }

    // Contact picker dialog (Primary search flow)
    if (showContactPicker) {
        ContactPickerDialog(
            favorites = favorites,
            title = "Search Contacts to Favorite",
            onContactSelected = { name, number, photoUri ->
                onAddFavorite(name, number, "Mobile", photoUri)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false },
            onManualAddClick = {
                showAddDialog = true
            }
        )
    }

    // Speed Dial Selection & Explanation Dialog
    if (speedDialTargetContact != null) {
        SpeedDialAssignDialog(
            contact = speedDialTargetContact!!,
            allFavorites = favorites,
            onDismiss = { speedDialTargetContact = null },
            onAssign = { slot ->
                onAssignSpeedDial(speedDialTargetContact!!, slot)
                speedDialTargetContact = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteGridCard(
    contact: FavoriteContact,
    isCompact: Boolean,
    isConfigureMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onCall: () -> Unit,
    onLongClick: () -> Unit = {},
    onSelect: () -> Unit,
    onCreateRule: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onSpeedDialClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { if (!isConfigureMode) onCall() },
                onLongClick = { if (!isConfigureMode) onLongClick() }
            )
            .testTag("fav_grid_card_${contact.phoneNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfigureMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Main Top Row: Avatar + Name & Label + #number Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle
                Surface(
                    shape = CircleShape,
                    color = Color(contact.avatarColor),
                    modifier = Modifier.size(38.dp)
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
                            val initialChar = (contact.nickname?.takeIf { it.isNotBlank() } ?: contact.name).take(1).uppercase()
                            Text(
                                text = initialChar,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Name, Phone Number & Label
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = if (!contact.nickname.isNullOrBlank()) contact.nickname else contact.name
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.label.isNotBlank()) {
                        Text(
                            text = contact.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Speed Dial #slot Badge (#number instead of Key #number)
                if (contact.speedDialSlot != null && !isConfigureMode) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "#${contact.speedDialSlot}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Action Row
            if (isConfigureMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reorder controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(24.dp).testTag("fav_move_prev_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Move Left",
                                tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(24.dp).testTag("fav_move_next_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Move Right",
                                tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Speed Slot Button
                    Surface(
                        onClick = onSpeedDialClick,
                        shape = RoundedCornerShape(6.dp),
                        color = if (contact.speedDialSlot != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (contact.speedDialSlot != null) "#${contact.speedDialSlot}" else "+ Speed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (contact.speedDialSlot != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Edit & Delete Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(24.dp).testTag("fav_edit_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp).testTag("fav_delete_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                // Normal Mode: WA button on the far left end, Phone Call on the far right end
                // Spaced appropriately across opposite ends of the card to prevent accidental misclicks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WhatsApp button on the far other end of the card
                    FilledIconButton(
                        onClick = { ContactHelper.launchWhatsAppCall(context, contact.phoneNumber) },
                        modifier = Modifier.size(28.dp).testTag("fav_wa_btn_${contact.id}"),
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

                    // Phone Call button on the opposite end
                    FilledIconButton(
                        onClick = onCall,
                        modifier = Modifier.size(28.dp).testTag("fav_call_btn_${contact.id}"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call ${contact.name}",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedDialAssignDialog(
    contact: FavoriteContact,
    allFavorites: List<FavoriteContact>,
    onDismiss: () -> Unit,
    onAssign: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Speed Dial",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hold keys 2–9 on the dialpad to call. Key 1 is Voicemail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val targetDisplayName = contact.nickname?.takeIf { it.isNotBlank() } ?: contact.name
                Text(
                    text = "Select key for $targetDisplayName:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // 3x3 Keypad Layout matching physical dialpad
                val keypadMatrix = listOf(
                    listOf(1 to "VM", 2 to "ABC", 3 to "DEF"),
                    listOf(4 to "GHI", 5 to "JKL", 6 to "MNO"),
                    listOf(7 to "PQRS", 8 to "TUV", 9 to "WXYZ")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keypadMatrix.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { (slot, letters) ->
                                if (slot == 1) {
                                    // Key 1 is Voicemail (reserved/disabled)
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "1",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = "Voicemail",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    val assignedOther = allFavorites.firstOrNull { it.speedDialSlot == slot && it.id != contact.id }
                                    val isAssignedToThis = contact.speedDialSlot == slot

                                    Card(
                                        onClick = { onAssign(slot) },
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = when {
                                                isAssignedToThis -> MaterialTheme.colorScheme.primary
                                                assignedOther != null -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "$slot",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isAssignedToThis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = letters,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isAssignedToThis) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = when {
                                                    isAssignedToThis -> "Active"
                                                    assignedOther != null -> (assignedOther.nickname?.takeIf { it.isNotBlank() } ?: assignedOther.name.split(" ").firstOrNull())?.take(7) ?: ""
                                                    else -> "Available"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                color = if (isAssignedToThis) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

