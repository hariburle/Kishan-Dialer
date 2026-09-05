package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FavoriteContact
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.EditFavoriteDialog

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
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var speedDialTargetContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var editTargetContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }

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
                    text = if (isReorderMode) "Reorder Favorites" else "VIP & Speed Dial Favorites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isReorderMode) "Use arrows on cards to arrange" else "One-tap speed calling & dialpad shortcuts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reorder toggle button
                if (favorites.size > 1) {
                    FilledTonalButton(
                        onClick = { isReorderMode = !isReorderMode },
                        modifier = Modifier.testTag("fav_screen_reorder_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.DragHandle,
                            contentDescription = if (isReorderMode) "Done Reordering" else "Reorder",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isReorderMode) "Done" else "Reorder",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (!isReorderMode) {
                    // Primary action: Search Contact Book (icon only)
                    FilledIconButton(
                        onClick = { showContactPicker = true },
                        modifier = Modifier.size(38.dp).testTag("fav_screen_search_contacts_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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

        if (favorites.isEmpty()) {
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
            // Dynamically calculate grid columns and card sizing based on screen constraints and count
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_grid_container")
            ) {
                val availableWidth = maxWidth
                val count = favorites.size
                
                // Dynamically adjust columns: 1-2 favorites -> 2 cols; 3-6 -> 2 or 3 cols; 7+ -> 3 or 4 cols
                val columnsCount = when {
                    availableWidth >= 600.dp -> if (count > 6) 4 else 3
                    count <= 4 -> 2
                    count in 5..8 -> if (availableWidth >= 360.dp) 3 else 2
                    else -> if (availableWidth >= 360.dp) 3 else 2
                }

                val isCompact = count >= 5

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(if (isCompact) 8.dp else 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp),
                    modifier = Modifier.fillMaxSize().testTag("favorites_grid")
                ) {
                    itemsIndexed(favorites, key = { _, it -> it.id }) { index, contact ->
                        FavoriteGridCard(
                            contact = contact,
                            isCompact = isCompact,
                            isReorderMode = isReorderMode,
                            canMoveUp = index > 0,
                            canMoveDown = index < favorites.size - 1,
                            onMoveUp = { onMoveFavorite(index, index - 1) },
                            onMoveDown = { onMoveFavorite(index, index + 1) },
                            onCall = { onCallNumber(contact.phoneNumber) },
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

    // Speed Dial Slot Assignment Dialog
    if (speedDialTargetContact != null) {
        SpeedDialAssignDialog(
            contact = speedDialTargetContact!!,
            onDismiss = { speedDialTargetContact = null },
            onAssign = { slot ->
                onAssignSpeedDial(speedDialTargetContact!!, slot)
                speedDialTargetContact = null
            }
        )
    }
}

@Composable
private fun FavoriteGridCard(
    contact: FavoriteContact,
    isCompact: Boolean,
    isReorderMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onCall: () -> Unit,
    onSelect: () -> Unit,
    onCreateRule: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onSpeedDialClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isReorderMode) onCall() }
            .testTag("fav_grid_card_${contact.phoneNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isReorderMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 8.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
        ) {
            // Speed Dial Slot badge top row (or reorder indicator)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { if (!isReorderMode) onSpeedDialClick() },
                    shape = RoundedCornerShape(6.dp),
                    color = if (contact.speedDialSlot != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (isCompact) 4.dp else 6.dp,
                            vertical = if (isCompact) 1.dp else 2.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (contact.speedDialSlot != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (isCompact) 10.dp else 12.dp)
                        )
                        Text(
                            text = if (contact.speedDialSlot != null) "#${contact.speedDialSlot}" else "+ Speed",
                            fontSize = if (isCompact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (contact.speedDialSlot != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(if (isCompact) 20.dp else 24.dp)
                            .testTag("fav_edit_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Contact",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(if (isCompact) 13.dp else 15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(if (isCompact) 20.dp else 24.dp)
                            .testTag("fav_delete_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                        )
                    }
                }
            }

            // Big Photo or Monogram (Dynamically scaled)
            Surface(
                shape = CircleShape,
                color = Color(contact.avatarColor),
                modifier = Modifier.size(if (isCompact) 40.dp else 54.dp)
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
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompact) 16.sp else 22.sp
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = contact.name,
                    style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (isCompact) 9.sp else 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Action row inside card (or Reorder arrows if isReorderMode)
            if (isReorderMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(if (isCompact) 28.dp else 34.dp).testTag("fav_move_prev_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Move Left",
                            tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Reorder position",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(if (isCompact) 28.dp else 34.dp).testTag("fav_move_next_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Move Right",
                            tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCreateRule,
                        modifier = Modifier.size(if (isCompact) 26.dp else 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Create Rule",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isCompact) 15.dp else 18.dp)
                        )
                    }

                    if (com.example.util.ContactHelper.shouldSuggestWhatsApp(contact.phoneNumber)) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        FilledIconButton(
                            onClick = { com.example.util.ContactHelper.launchWhatsAppCall(context, contact.phoneNumber) },
                            modifier = Modifier.size(if (isCompact) 26.dp else 32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "WA",
                                fontSize = if (isCompact) 9.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = onCall,
                        modifier = Modifier.size(if (isCompact) 28.dp else 36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call ${contact.name}",
                            modifier = Modifier.size(if (isCompact) 14.dp else 18.dp)
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
    onDismiss: () -> Unit,
    onAssign: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Speed Dial Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Long-pressing this keypad number will automatically call ${contact.name} (${contact.phoneNumber}):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (2..5).forEach { slot ->
                        FilledIconButton(
                            onClick = { onAssign(slot) },
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (contact.speedDialSlot == slot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(text = slot.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (6..9).forEach { slot ->
                        FilledIconButton(
                            onClick = { onAssign(slot) },
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (contact.speedDialSlot == slot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(text = slot.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
