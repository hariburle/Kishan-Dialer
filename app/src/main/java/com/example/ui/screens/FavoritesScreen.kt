package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import com.example.ui.components.WhatsAppIcon
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.IgnoredContact
import com.example.data.RecentCall
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.CreateContactDialog
import com.example.ui.components.ContactDetailsBottomSheet
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.EditFavoriteDialog
import com.example.ui.components.MultiNumberCallDialog
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import java.util.Collections

data class PopularContactItem(
    val name: String,
    val phoneNumber: String,
    val label: String,
    val photoUri: String?,
    val callCount: Int,
    val deviceContact: DeviceContact?
)

enum class FavCardDesign(val label: String, val styleKey: String) {
    MODERN_BENTO("Bento", "bento"),
    QUICK_ACTION("Grid", "quick_action"),
    MATERIAL_YOU("Material", "material_you");

    companion object {
        fun fromKey(key: String): FavCardDesign {
            return entries.find { it.styleKey == key } ?: MODERN_BENTO
        }
    }
}

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteContact>,
    recentCalls: List<RecentCall> = emptyList(),
    ignoredContacts: List<IgnoredContact> = emptyList(),
    onSelectNumber: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onCallWhatsApp: (String) -> Unit = {},
    onCreateRule: (String) -> Unit,
    onDeleteFavorite: (FavoriteContact) -> Unit,
    onAddFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onAddNewContact: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onAssignSpeedDial: (FavoriteContact, Int) -> Unit,
    onMoveFavorite: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onReorderFavorites: (List<FavoriteContact>) -> Unit = {},
    onEditFavorite: (FavoriteContact, String?) -> Unit = { _, _ -> },
    onUpdateFavoriteNumber: (FavoriteContact, String, String) -> Unit = { _, _, _ -> },
    onIgnoreContact: (phoneNumber: String, name: String, category: String, tag: String) -> Unit = { _, _, _, _ -> },
    onUnignoreContact: (phoneNumber: String) -> Unit = {},
    onUpdateIgnoredContactTag: (phoneNumber: String, newTag: String, newName: String) -> Unit = { _, _, _ -> },
    getPreferredCallingMode: (String) -> String = { "cellular" },
    onSaveLearnedCallMode: (String, String) -> Unit = { _, _ -> },
    confirmFavoritesCall: Boolean = true,
    favoriteCardStyle: String = "bento",
    onSetFavoriteCardStyle: (String) -> Unit = {},
    isFlipToShhhEnabled: Boolean = true,
    isShhhActive: Boolean = false,
    onToggleFlipToShhh: () -> Unit = {},
    onUpdateContact: (oldNum: String, name: String, number: String, label: String, nickname: String?) -> Unit = { _, _, _, _, _ -> },
    deviceContacts: List<DeviceContact> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var speedDialTargetContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var editTargetContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var editTargetIgnored by remember { mutableStateOf<IgnoredContact?>(null) }
    var isConfigureMode by remember { mutableStateOf(false) }
    val cardDesign = FavCardDesign.fromKey(favoriteCardStyle)
    var searchQuery by remember { mutableStateOf("") }
    var localFavorites by remember { mutableStateOf(favorites) }
    var draggingContactId by remember { mutableStateOf<Long?>(null) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dragTotalOffset by remember { mutableStateOf(Offset.Zero) }
    var dragItemSize by remember { mutableStateOf(IntSize.Zero) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(favorites) {
        if (draggingContactId == null) {
            localFavorites = favorites
        }
    }
    var isSearchActive by remember { mutableStateOf(false) }
    var pendingCallConfirmation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var multiNumberContactToCall by remember { mutableStateOf<DeviceContact?>(null) }
    var favoriteContactToCall by remember { mutableStateOf<FavoriteContact?>(null) }
    var contactDetailsTarget by remember { mutableStateOf<Pair<DeviceContact, FavoriteContact?>?>(null) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            deviceContacts = ContactHelper.fetchDeviceContacts(context)
        }
    }

    val ignoredNorms = remember(ignoredContacts) {
        ignoredContacts.map { it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) }.toSet()
    }
    val ignoredNames = remember(ignoredContacts) {
        ignoredContacts.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    val popularContacts = remember(deviceContacts, favorites, recentCalls, ignoredContacts) {
        val favNumbers = favorites.map { it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) }.filter { it.isNotBlank() }.toSet()
        val favNames = favorites.map { it.name.trim().lowercase() }.toSet()

        val excludedNames = setOf("voicemail", "spam", "gate", "intercom", "unknown")

        val callCounts = mutableMapOf<String, Int>()
        recentCalls.forEach { call ->
            val norm = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
            if (norm.isNotBlank()) {
                callCounts[norm] = (callCounts[norm] ?: 0) + 1
            }
        }

        val list = mutableListOf<PopularContactItem>()
        val seenNorms = mutableSetOf<String>()

        fun isIgnored(norm: String, name: String): Boolean {
            val nameLower = name.trim().lowercase()
            return ignoredNorms.contains(norm) || ignoredNames.any { nameLower.contains(it) } || ignoredContacts.any { ic ->
                val icNorm = ic.phoneNumber.filter { c -> c.isDigit() }.takeLast(10)
                icNorm == norm || (ic.tag.isNotBlank() && nameLower.contains(ic.tag.trim().lowercase()))
            }
        }

        // 1. Device contacts that have call counts
        deviceContacts.forEach { dc ->
            val norm = dc.phoneNumber.filter { it.isDigit() }.takeLast(10)
            val nameLower = dc.name.trim().lowercase()
            val isExcluded = excludedNames.any { nameLower.contains(it) }
            if (norm.isNotBlank() && !favNumbers.contains(norm) && !favNames.contains(nameLower) && !isExcluded && !isIgnored(norm, dc.name)) {
                val count = callCounts[norm] ?: 0
                if (count > 0 && seenNorms.add(norm)) {
                    list.add(
                        PopularContactItem(
                            name = dc.name,
                            phoneNumber = dc.phoneNumber,
                            label = dc.label,
                            photoUri = dc.photoUri,
                            callCount = count,
                            deviceContact = dc
                        )
                    )
                }
            }
        }

        // 2. Recent calls not in favorites or already added
        recentCalls.forEach { rc ->
            val norm = rc.phoneNumber.filter { it.isDigit() }.takeLast(10)
            val callerNameStr = rc.callerName ?: ""
            val nameLower = callerNameStr.trim().lowercase()
            val isExcluded = excludedNames.any { nameLower.contains(it) }
            if (norm.isNotBlank() && !favNumbers.contains(norm) && !favNames.contains(nameLower) && !isExcluded && !isIgnored(norm, callerNameStr) && seenNorms.add(norm)) {
                val count = callCounts[norm] ?: 1
                list.add(
                    PopularContactItem(
                        name = callerNameStr.ifBlank { rc.phoneNumber },
                        phoneNumber = rc.phoneNumber,
                        label = "Frequent",
                        photoUri = rc.photoUri,
                        callCount = count,
                        deviceContact = null
                    )
                )
            }
        }

        // 3. (Removed random fallback to contacts never called)

        list.sortedByDescending { it.callCount }.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_screen")
    ) {
        // Search bar & action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isConfigureMode) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Reorder Favorites",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Drag grip to rearrange • Arrows for 1-step moves",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("favorites_search_input"),
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
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }

            if (favorites.isNotEmpty()) {
                FilledIconButton(
                    onClick = { isConfigureMode = !isConfigureMode },
                    modifier = Modifier.size(48.dp).testTag("fav_screen_configure_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isConfigureMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isConfigureMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isConfigureMode) Icons.Default.Check else Icons.Default.Tune,
                        contentDescription = if (isConfigureMode) "Done" else "Configure"
                    )
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            // Live Search Results across Contacts & Favorites showing all phone numbers
            val queryClean = searchQuery.trim().lowercase()
            val filteredContacts = remember(searchQuery, deviceContacts, favorites) {
                deviceContacts.filter { dc ->
                    dc.name.lowercase().contains(queryClean) ||
                    (dc.nickname != null && dc.nickname.lowercase().contains(queryClean)) ||
                    ContactHelper.matchesNumberQuery(dc.phoneNumber, searchQuery) ||
                    dc.phoneNumbers.any { ContactHelper.matchesNumberQuery(it.number, searchQuery) }
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
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

                                    if (!isFav) {
                                        TextButton(
                                            onClick = {
                                                onAddFavorite(contact.name, contact.phoneNumber, contact.label, contact.photoUri)
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // List each phone number with WhatsApp and Phone call buttons
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
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
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
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledIconButton(
                                                onClick = { onCallWhatsApp(pn.number) },
                                                modifier = Modifier.size(34.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = Color(0xFF25D366),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                                    contentDescription = "WhatsApp ${pn.number}",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            FilledIconButton(
                                                onClick = { onCallNumber(pn.number) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Call ${pn.number}",
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
                    state = gridState,
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("favorites_grid")
                ) {
                    if (favorites.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "No Favorites Starred Yet",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Star frequent callers below or tap + to add VIPs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { showContactPicker = true }) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
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
                                        text = "Favorites",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "${favorites.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                // Interactive Card Design Selector
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FavCardDesign.values().forEach { design ->
                                        val isSelected = (cardDesign == design)
                                        Surface(
                                            onClick = {
                                                onSetFavoriteCardStyle(design.styleKey)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = design.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        itemsIndexed(localFavorites, key = { _, it -> it.id }) { index, contact ->
                            val preferredMode = getPreferredCallingMode(contact.phoneNumber)
                            val isBeingDragged = (draggingContactId == contact.id)

                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .alpha(if (isBeingDragged) 0.15f else 1.0f)
                            ) {
                                FavoriteGridCard(
                                    contact = contact,
                                    cardDesign = cardDesign,
                                    isCompact = isCompact,
                                    isConfigureMode = isConfigureMode,
                                    isDraggingActive = draggingContactId != null,
                                    preferredCallingMode = preferredMode,
                                    canMoveUpRow = index >= columnsCount,
                                    canMoveDownRow = index + columnsCount < localFavorites.size,
                                    canMoveLeftCol = index % columnsCount > 0,
                                    canMoveRightCol = (index % columnsCount < columnsCount - 1) && (index + 1 < localFavorites.size),
                                    onMoveUpRow = {
                                        val next = localFavorites.toMutableList()
                                        Collections.swap(next, index, index - columnsCount)
                                        localFavorites = next
                                        onReorderFavorites(next)
                                    },
                                    onMoveDownRow = {
                                        val next = localFavorites.toMutableList()
                                        Collections.swap(next, index, index + columnsCount)
                                        localFavorites = next
                                        onReorderFavorites(next)
                                    },
                                    onMoveLeftCol = {
                                        val next = localFavorites.toMutableList()
                                        Collections.swap(next, index, index - 1)
                                        localFavorites = next
                                        onReorderFavorites(next)
                                    },
                                    onMoveRightCol = {
                                        val next = localFavorites.toMutableList()
                                        Collections.swap(next, index, index + 1)
                                        localFavorites = next
                                        onReorderFavorites(next)
                                    },
                                    onDragStart = {
                                        val itemInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == contact.id }
                                        if (itemInfo != null) {
                                            draggingContactId = contact.id
                                            dragStartOffset = Offset(itemInfo.offset.x.toFloat(), itemInfo.offset.y.toFloat())
                                            dragTotalOffset = Offset.Zero
                                            dragItemSize = itemInfo.size
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    onDragEnd = {
                                        if (draggingContactId != null) {
                                            draggingContactId = null
                                            dragTotalOffset = Offset.Zero
                                            dragItemSize = IntSize.Zero
                                            onReorderFavorites(localFavorites)
                                        }
                                    },
                                    onDragDelta = { delta ->
                                        dragTotalOffset += delta
                                        val currentX = dragStartOffset.x + dragTotalOffset.x
                                        val currentY = dragStartOffset.y + dragTotalOffset.y
                                        val currentCenter = Offset(
                                            currentX + dragItemSize.width / 2f,
                                            currentY + dragItemSize.height / 2f
                                        )

                                        // Auto-scroll when near top or bottom
                                        val viewportHeight = gridState.layoutInfo.viewportSize.height
                                        if (viewportHeight > 0) {
                                            val scrollZone = 100f
                                            if (currentY < scrollZone && gridState.canScrollBackward) {
                                                val speed = -((scrollZone - currentY) / 4f).coerceIn(4f, 25f)
                                                coroutineScope.launch { gridState.scrollBy(speed) }
                                                dragStartOffset -= Offset(0f, speed)
                                            } else if (currentY + dragItemSize.height > viewportHeight - scrollZone && gridState.canScrollForward) {
                                                val diff = (currentY + dragItemSize.height) - (viewportHeight - scrollZone)
                                                val speed = (diff / 4f).coerceIn(4f, 25f)
                                                coroutineScope.launch { gridState.scrollBy(speed) }
                                                dragStartOffset -= Offset(0f, speed)
                                            }
                                        }

                                        // Find if hovered over another favorite item
                                        val targetItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                            val key = info.key
                                            if (key !is Long || key == draggingContactId) return@firstOrNull false
                                            val left = info.offset.x.toFloat()
                                            val top = info.offset.y.toFloat()
                                            val right = left + info.size.width
                                            val bottom = top + info.size.height
                                            currentCenter.x in left..right && currentCenter.y in top..bottom
                                        }

                                        if (targetItem != null) {
                                            val fromIdx = localFavorites.indexOfFirst { it.id == draggingContactId }
                                            val toIdx = localFavorites.indexOfFirst { it.id == targetItem.key }
                                            if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                                val next = localFavorites.toMutableList()
                                                val item = next.removeAt(fromIdx)
                                                next.add(toIdx, item)
                                                localFavorites = next
                                                try {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    },
                                    onCall = {
                                        if (confirmFavoritesCall) {
                                            pendingCallConfirmation = Pair(contact.name, contact.phoneNumber)
                                        } else {
                                            onCallNumber(contact.phoneNumber)
                                        }
                                    },
                                    onCallWhatsApp = { onCallWhatsApp(contact.phoneNumber) },
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
                                    onDelete = {
                                        localFavorites = localFavorites.filter { it.id != contact.id }
                                        onDeleteFavorite(contact)
                                    },
                                    onSpeedDialClick = { speedDialTargetContact = contact }
                                )
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // POPULAR (FREQUENTLY CONTACTED) SECTION
                    // ---------------------------------------------------------
                    if (popularContacts.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = if (favorites.isEmpty()) 6.dp else 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Popular",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "• Frequently Contacted",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "${popularContacts.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        items(popularContacts, key = { "pop_${it.phoneNumber}_${it.name}" }) { popItem ->
                            PopularGridCard(
                                item = popItem,
                                isConfigureMode = isConfigureMode,
                                onCall = { onCallNumber(popItem.phoneNumber) },
                                onAddFavorite = {
                                    onAddFavorite(popItem.name, popItem.phoneNumber, popItem.label, popItem.photoUri)
                                },
                                onIgnore = {
                                    onIgnoreContact(popItem.phoneNumber, popItem.name, popItem.label, popItem.name)
                                },
                                onClick = {
                                    val dc = popItem.deviceContact ?: DeviceContact(
                                        name = popItem.name,
                                        phoneNumber = popItem.phoneNumber,
                                        label = popItem.label,
                                        photoUri = popItem.photoUri,
                                        phoneNumbers = listOf(ContactPhoneNumber(popItem.phoneNumber, popItem.label))
                                    )
                                    val matchedFav = favorites.firstOrNull { f ->
                                        val fNum = f.phoneNumber.filter { it.isDigit() }.takeLast(10)
                                        val dcNum = dc.phoneNumber.filter { it.isDigit() }.takeLast(10)
                                        (fNum.isNotBlank() && fNum == dcNum) || f.name.equals(dc.name, ignoreCase = true)
                                    }
                                    contactDetailsTarget = Pair(dc, matchedFav)
                                }
                            )
                        }

                        if (isConfigureMode && ignoredContacts.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Ignored Popular Callers (${ignoredContacts.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "View ignored callers, restore them, or fix any wrong taggings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    for (ignored in ignoredContacts) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = ignored.name.ifBlank { ignored.tag.ifBlank { "Ignored Caller" } },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${ignored.phoneNumber} • Tag: ${ignored.tag.ifBlank { "None" }}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = { editTargetIgnored = ignored },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Fix Tag / Edit",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onUnignoreContact(ignored.phoneNumber) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Restore",
                                                            tint = MaterialTheme.colorScheme.primary,
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

                // Floating Dragged Card Overlay (Renders ON TOP of all grid items)
                if (draggingContactId != null && dragItemSize.width > 0) {
                    val dragContact = localFavorites.firstOrNull { it.id == draggingContactId }
                    if (dragContact != null) {
                        val density = LocalDensity.current
                        Box(
                            modifier = Modifier
                                .zIndex(9999f)
                                .graphicsLayer {
                                    translationX = dragStartOffset.x + dragTotalOffset.x
                                    translationY = dragStartOffset.y + dragTotalOffset.y
                                    scaleX = 1.05f
                                    scaleY = 1.05f
                                    shadowElevation = 24f
                                }
                                .width(with(density) { dragItemSize.width.toDp() })
                                .height(with(density) { dragItemSize.height.toDp() })
                        ) {
                            FavoriteGridCard(
                                contact = dragContact,
                                cardDesign = cardDesign,
                                isCompact = isCompact,
                                isConfigureMode = isConfigureMode,
                                isFloatingOverlay = true,
                                isDraggingActive = true,
                                preferredCallingMode = getPreferredCallingMode(dragContact.phoneNumber),
                                onCall = {},
                                onSelect = {},
                                onCreateRule = {},
                                onDelete = {},
                                onSpeedDialClick = {}
                            )
                        }
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

    // Edit Ignored Contact Dialog
    if (editTargetIgnored != null) {
        EditIgnoredContactDialog(
            ignored = editTargetIgnored!!,
            onDismiss = { editTargetIgnored = null },
            onSave = { phone, newTag, newName ->
                onUpdateIgnoredContactTag(phone, newTag, newName)
                editTargetIgnored = null
            }
        )
    }

    // Add Favorite / New Contact dialog
    if (showAddDialog) {
        CreateContactDialog(
            dialogTitle = "Add to Favorites",
            initialAddToFavorites = true,
            onDismiss = { showAddDialog = false },
            onSave = { name, number, label, destination, addToFavs ->
                onAddNewContact(name, number, label, destination, addToFavs)
                showAddDialog = false
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
            getPreferredCallingMode = getPreferredCallingMode,
            onSaveLearnedCallMode = onSaveLearnedCallMode,
            onEditContact = { name, number, label, nickname ->
                onUpdateContact(matchedContact.phoneNumber, name, number, label, nickname)
                contactDetailsTarget = null
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
            deviceContacts = deviceContacts,
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

    if (pendingCallConfirmation != null) {
        val (name, number) = pendingCallConfirmation!!
        val preferredMode = getPreferredCallingMode(number)
        val isWhatsApp = preferredMode == "whatsapp"
        AlertDialog(
            onDismissRequest = { pendingCallConfirmation = null },
            title = { Text("Call $name?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Number: $number")
                    Text(
                        text = if (isWhatsApp) "Via WhatsApp Calling" else "Via Cellular Phone Call",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isWhatsApp) Color(0xFF25D366) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val numToCall = number
                        pendingCallConfirmation = null
                        onCallNumber(numToCall)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isWhatsApp) Color(0xFF25D366) else Color(0xFF16A34A)
                    )
                ) {
                    Text(if (isWhatsApp) "WhatsApp Call" else "Call")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCallConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteGridCard(
    contact: FavoriteContact,
    cardDesign: FavCardDesign = FavCardDesign.MODERN_BENTO,
    isCompact: Boolean,
    isConfigureMode: Boolean = false,
    isFloatingOverlay: Boolean = false,
    isDraggingActive: Boolean = false,
    preferredCallingMode: String = "cellular",
    canMoveUpRow: Boolean = false,
    canMoveDownRow: Boolean = false,
    canMoveLeftCol: Boolean = false,
    canMoveRightCol: Boolean = false,
    onMoveUpRow: () -> Unit = {},
    onMoveDownRow: () -> Unit = {},
    onMoveLeftCol: () -> Unit = {},
    onMoveRightCol: () -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragDelta: (Offset) -> Unit = {},
    onCall: () -> Unit,
    onCallWhatsApp: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onSelect: () -> Unit,
    onCreateRule: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onSpeedDialClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shadowElevation by animateDpAsState(
        targetValue = if (isFloatingOverlay) 16.dp else when (cardDesign) {
            FavCardDesign.MODERN_BENTO -> 1.dp
            FavCardDesign.QUICK_ACTION -> 1.dp
            FavCardDesign.MATERIAL_YOU -> 0.dp
        },
        label = "drag_shadow"
    )

    val cardModifier = if (!isFloatingOverlay) {
        modifier
            .fillMaxWidth()
            .pointerInput(contact.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount)
                    }
                )
            }
            .combinedClickable(
                onClick = { onLongClick() },
                onLongClick = { onDragStart() }
            )
    } else {
        modifier.fillMaxWidth()
    }

    val cardShape = when (cardDesign) {
        FavCardDesign.MODERN_BENTO -> RoundedCornerShape(18.dp)
        FavCardDesign.QUICK_ACTION -> RoundedCornerShape(14.dp)
        FavCardDesign.MATERIAL_YOU -> RoundedCornerShape(22.dp)
    }

    val cardBorder = if (cardDesign == FavCardDesign.MATERIAL_YOU && !isFloatingOverlay) {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    } else if (cardDesign == FavCardDesign.MODERN_BENTO && !isFloatingOverlay) {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    } else if (cardDesign == FavCardDesign.QUICK_ACTION && !isFloatingOverlay) {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    } else {
        null
    }

    val cardContainerColor = if (isFloatingOverlay) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    } else if (isConfigureMode) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        when (cardDesign) {
            FavCardDesign.MODERN_BENTO -> MaterialTheme.colorScheme.surface
            FavCardDesign.QUICK_ACTION -> MaterialTheme.colorScheme.surface
            FavCardDesign.MATERIAL_YOU -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        }
    }

    Card(
        modifier = cardModifier.testTag("fav_grid_card_${contact.phoneNumber}"),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = shadowElevation),
        shape = cardShape,
        border = cardBorder
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                    // Avatar
                    val avatarShape = when (cardDesign) {
                        FavCardDesign.MODERN_BENTO -> RoundedCornerShape(12.dp)
                        FavCardDesign.QUICK_ACTION -> CircleShape
                        FavCardDesign.MATERIAL_YOU -> RoundedCornerShape(14.dp)
                    }
                    val avatarSize = if (cardDesign == FavCardDesign.MODERN_BENTO) 42.dp else 38.dp

                    Surface(
                        shape = avatarShape,
                        color = Color(contact.avatarColor),
                        modifier = Modifier.size(avatarSize)
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
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(26.dp).testTag("fav_edit_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(26.dp).testTag("fav_delete_${contact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Dedicated Drag Handle in Configure Mode
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(28.dp)
                                .pointerInput(contact.id) {
                                    detectDragGestures(
                                        onDragStart = { onDragStart() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDragDelta(dragAmount)
                                        },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { onDragEnd() }
                                    )
                                }
                                .testTag("fav_drag_handle_${contact.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Normal Mode: Action area tailored to selected design
                    when (cardDesign) {
                        FavCardDesign.MODERN_BENTO -> {
                            if (preferredCallingMode == "ask" || preferredCallingMode == "ask_always") {
                                // Ask & Learn or Ask Always mode: Dual dialers side-by-side
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Phone dialer
                                    Surface(
                                        onClick = onCall,
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .testTag("fav_call_btn_${contact.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Phone",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // WhatsApp dialer
                                    Surface(
                                        onClick = onCallWhatsApp,
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF25D366).copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .testTag("fav_wa_btn_${contact.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            WhatsAppIcon(modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "WhatsApp",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E7E34)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Single preferred mode pill button
                                Surface(
                                    onClick = if (preferredCallingMode == "whatsapp") onCallWhatsApp else onCall,
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (preferredCallingMode == "whatsapp") Color(0xFF25D366).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .testTag("fav_call_btn_${contact.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (preferredCallingMode == "whatsapp") {
                                            WhatsAppIcon(modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "WhatsApp",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E7E34)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Phone",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        FavCardDesign.QUICK_ACTION -> {
                            // Quick-Action Tile: Prominent round call action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (preferredCallingMode == "ask" || preferredCallingMode == "ask_always") {
                                    FilledIconButton(
                                        onClick = onCall,
                                        modifier = Modifier.size(30.dp).testTag("fav_call_btn_${contact.id}"),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF16A34A),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.Call, contentDescription = "Phone Call", modifier = Modifier.size(15.dp))
                                    }
                                    FilledIconButton(
                                        onClick = onCallWhatsApp,
                                        modifier = Modifier.size(30.dp).testTag("fav_wa_btn_${contact.id}"),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF25D366),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        WhatsAppIcon(modifier = Modifier.size(16.dp))
                                    }
                                } else if (preferredCallingMode == "whatsapp") {
                                    FilledIconButton(
                                        onClick = onCallWhatsApp,
                                        modifier = Modifier.size(30.dp).testTag("fav_call_btn_${contact.id}"),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF25D366),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        WhatsAppIcon(modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    FilledIconButton(
                                        onClick = onCall,
                                        modifier = Modifier.size(30.dp).testTag("fav_call_btn_${contact.id}"),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF16A34A),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call ${contact.name}",
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                        FavCardDesign.MATERIAL_YOU -> {
                            // Expressive Material You: Tonal chip buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (preferredCallingMode == "ask" || preferredCallingMode == "ask_always") {
                                    Surface(
                                        onClick = onCall,
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.height(28.dp).testTag("fav_call_btn_${contact.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                                            Text(text = "Phone", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                    Surface(
                                        onClick = onCallWhatsApp,
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF25D366),
                                        modifier = Modifier.height(28.dp).testTag("fav_wa_btn_${contact.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            WhatsAppIcon(modifier = Modifier.size(13.dp))
                                            Text(text = "WhatsApp", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                } else {
                                    Surface(
                                        onClick = if (preferredCallingMode == "whatsapp") onCallWhatsApp else onCall,
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (preferredCallingMode == "whatsapp") Color(0xFF25D366) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.height(28.dp).testTag("fav_call_btn_${contact.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (preferredCallingMode == "whatsapp") {
                                                WhatsAppIcon(modifier = Modifier.size(14.dp))
                                                Text(text = "WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            } else {
                                                Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                                Text(text = "Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Directional Reorder Arrows overlaid on the exact sides where movement is possible
            if (isConfigureMode && !isFloatingOverlay && !isDraggingActive) {
                // Top Arrow (Up)
                if (canMoveUpRow) {
                    Surface(
                        onClick = onMoveUpRow,
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 44.dp, height = 20.dp)
                            .testTag("fav_move_up_${contact.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up Row",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom Arrow (Down)
                if (canMoveDownRow) {
                    Surface(
                        onClick = onMoveDownRow,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(width = 44.dp, height = 20.dp)
                            .testTag("fav_move_down_${contact.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down Row",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Left Arrow (Left)
                if (canMoveLeftCol) {
                    Surface(
                        onClick = onMoveLeftCol,
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(width = 20.dp, height = 44.dp)
                            .testTag("fav_move_left_${contact.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Move Left Column",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Right Arrow (Right)
                if (canMoveRightCol) {
                    Surface(
                        onClick = onMoveRightCol,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 20.dp, height = 44.dp)
                            .testTag("fav_move_right_${contact.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Move Right Column",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularGridCard(
    item: PopularContactItem,
    isConfigureMode: Boolean = false,
    onCall: () -> Unit,
    onAddFavorite: () -> Unit,
    onIgnore: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("popular_card_${item.phoneNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    if (!item.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = item.photoUri,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.name.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Call count chip & Ignore or Add Favorite button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "${item.callCount} calls",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isConfigureMode) {
                        IconButton(
                            onClick = onIgnore,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ignore Contact",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onAddFavorite,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Add to Favorites",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.phoneNumber} • ${item.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onCall,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddFavorite,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "Star", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun EditIgnoredContactDialog(
    ignored: IgnoredContact,
    onDismiss: () -> Unit,
    onSave: (phoneNumber: String, newTag: String, newName: String) -> Unit
) {
    var tagName by remember { mutableStateOf(ignored.tag) }
    var contactName by remember { mutableStateOf(ignored.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fix Ignored Caller Tag / Name") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Number: ${ignored.phoneNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Ignore Tag / Substring") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(ignored.phoneNumber, tagName.trim(), contactName.trim())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

