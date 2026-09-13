package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FavoriteContact
import com.example.ui.models.FavCardDesign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteGridCard(
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
        FavCardDesign.MATERIAL_YOU -> RoundedCornerShape(22.dp)
    }

    val cardBorder = if (cardDesign == FavCardDesign.MATERIAL_YOU && !isFloatingOverlay) {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    } else if (cardDesign == FavCardDesign.MODERN_BENTO && !isFloatingOverlay) {
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

                    // Name, Nickname, Phone Number & Label
                    Column(modifier = Modifier.weight(1f)) {
                        val displayName = if (!contact.nickname.isNullOrBlank()) contact.nickname!! else contact.name
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
