package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentCall
import com.example.ui.components.ContactDetailsBottomSheet
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.FavoriteContact
import com.example.data.SpamNumber

data class GroupedCallLog(
    val primaryCall: RecentCall,
    val count: Int,
    val isSpam: Boolean,
    val spamDetails: SpamNumber?
)

@Composable
fun CallLogScreen(
    recentCalls: List<RecentCall>,
    spamNumbers: List<SpamNumber> = emptyList(),
    favorites: List<FavoriteContact> = emptyList(),
    onCallBack: (String) -> Unit,
    onCreateRuleForNumber: (String) -> Unit,
    onMarkSpam: (String) -> Unit = {},
    onRemoveSpam: (String) -> Unit = {},
    onToggleFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit = { _, _, _, _ -> },
    onUpdateNoteAndReminder: (RecentCall, String?, Long?) -> Unit = { _, _, _ -> },
    onUpdateContact: (oldNum: String, name: String, number: String, label: String, nickname: String?) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var noteDialogCall by remember { mutableStateOf<RecentCall?>(null) }
    var noteText by remember { mutableStateOf("") }
    var reminderMinutes by remember { mutableStateOf<Long?>(null) }
    var contactDetailsTarget by remember { mutableStateOf<Pair<DeviceContact, FavoriteContact?>?>(null) }

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
                onCallBack(num)
            },
            onSelectInDialer = { num ->
                onCallBack(num)
            },
            onToggleFavorite = {
                val favName = matchedContact.nickname?.ifBlank { null } ?: matchedContact.name
                onToggleFavorite(favName, favContact?.phoneNumber ?: matchedContact.phoneNumber, favContact?.label ?: matchedContact.label, matchedContact.photoUri)
            },
            onSetAsDefaultNumber = { newNum, newLabel ->
                if (favContact != null) {
                    onToggleFavorite(favContact.name, newNum, newLabel, matchedContact.photoUri)
                } else {
                    onToggleFavorite(matchedContact.name, newNum, newLabel, matchedContact.photoUri)
                }
            },
            onClearDefaultNumber = {
                if (favContact != null) {
                    onToggleFavorite(favContact.name, favContact.phoneNumber, favContact.label, favContact.photoUri)
                }
            },
            onCreateRule = { num ->
                onCreateRuleForNumber(num)
            },
            onEditContact = { name, number, label, nickname ->
                onUpdateContact(matchedContact.phoneNumber, name, number, label, nickname)
                contactDetailsTarget = null
            },
            onDismiss = {
                contactDetailsTarget = null
            }
        )
    }

    if (noteDialogCall != null) {
        val targetCall = noteDialogCall!!
        AlertDialog(
            onDismissRequest = { noteDialogCall = null },
            title = {
                Text(
                    text = "Post-Call Note & Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = targetCall.callerName ?: targetCall.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Add call summary, action items...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_call_note_input")
                    )

                    Text(
                        text = "Follow-up Reminder:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(null to "None", 15L to "15m", 60L to "1h", 1440L to "Tomorrow").forEach { (mins, label) ->
                            AssistChip(
                                onClick = { reminderMinutes = mins },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (reminderMinutes == mins) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reminderEpoch = reminderMinutes?.let { System.currentTimeMillis() + it * 60 * 1000 }
                        onUpdateNoteAndReminder(targetCall, noteText.ifBlank { null }, reminderEpoch)
                        noteDialogCall = null
                    },
                    modifier = Modifier.testTag("dialog_save_note_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteDialogCall = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    // Group consecutive calls from the same phone number
    val groupedCalls = androidx.compose.runtime.remember(recentCalls, spamNumbers) {
        val groups = mutableListOf<GroupedCallLog>()
        if (recentCalls.isEmpty()) return@remember groups

        var currentGroupCall = recentCalls[0]
        var currentCount = 1

        for (i in 1 until recentCalls.size) {
            val call = recentCalls[i]
            if (call.phoneNumber == currentGroupCall.phoneNumber && call.callType == currentGroupCall.callType) {
                currentCount++
            } else {
                val spam = spamNumbers.firstOrNull { it.phoneNumber == currentGroupCall.phoneNumber }
                groups.add(GroupedCallLog(currentGroupCall, currentCount, spam != null || currentGroupCall.isSpam, spam))
                currentGroupCall = call
                currentCount = 1
            }
        }
        val lastSpam = spamNumbers.firstOrNull { it.phoneNumber == currentGroupCall.phoneNumber }
        groups.add(GroupedCallLog(currentGroupCall, currentCount, lastSpam != null || currentGroupCall.isSpam, lastSpam))
        groups
    }

    if (recentCalls.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("call_log_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "No Recent Calls",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Calls made or received will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("call_log_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groupedCalls, key = { it.primaryCall.id }) { group ->
                val isFav = favorites.any { it.phoneNumber == group.primaryCall.phoneNumber }
                CallLogItem(
                    group = group,
                    isFavorite = isFav,
                    onCallBack = { onCallBack(group.primaryCall.phoneNumber) },
                    onCreateRule = { onCreateRuleForNumber(group.primaryCall.phoneNumber) },
                    onOpenNoteDialog = { target ->
                        noteDialogCall = target
                        noteText = target.note ?: ""
                        reminderMinutes = target.reminderTime?.let {
                            val diff = (it - System.currentTimeMillis()) / (60 * 1000)
                            if (diff > 0) diff else null
                        }
                    },
                    onToggleSpam = {
                        if (group.isSpam) {
                            onRemoveSpam(group.primaryCall.phoneNumber)
                        } else {
                            onMarkSpam(group.primaryCall.phoneNumber)
                        }
                    },
                    onToggleFavorite = {
                        onToggleFavorite(
                            group.primaryCall.callerName ?: group.primaryCall.phoneNumber,
                            group.primaryCall.phoneNumber,
                            "Mobile",
                            group.primaryCall.photoUri
                        )
                    },
                    onOpenDetails = {
                        val call = group.primaryCall
                        val contactName = call.callerName?.ifBlank { null } ?: call.phoneNumber
                        val dc = DeviceContact(
                            name = contactName,
                            phoneNumber = call.phoneNumber,
                            label = "Mobile",
                            photoUri = call.photoUri,
                            phoneNumbers = listOf(ContactPhoneNumber(call.phoneNumber, "Mobile"))
                        )
                        val matchedFav = favorites.firstOrNull { fav ->
                            fav.phoneNumber == call.phoneNumber ||
                            (!call.callerName.isNullOrBlank() && fav.name.equals(call.callerName, ignoreCase = true))
                        }
                        contactDetailsTarget = Pair(dc, matchedFav)
                    }
                )
            }
        }
    }
}

@Composable
private fun CallLogItem(
    group: GroupedCallLog,
    isFavorite: Boolean,
    onCallBack: () -> Unit,
    onCreateRule: () -> Unit,
    onOpenNoteDialog: (RecentCall) -> Unit,
    onToggleSpam: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val call = group.primaryCall
    val (typeIcon, typeColor, typeLabel) = when (call.callType) {
        1 -> Triple(Icons.AutoMirrored.Filled.CallReceived, Color(0xFF16A34A), "Incoming")
        2 -> Triple(Icons.AutoMirrored.Filled.CallMade, Color(0xFF2563EB), "Outgoing")
        else -> Triple(Icons.AutoMirrored.Filled.CallMissed, Color(0xFFDC2626), "Missed")
    }

    val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(call.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
            .testTag("call_item_${call.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (group.isSpam) {
                Color(0xFFFEF2F2)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with contact photo or call type indicator or Spam warning
                Box(modifier = Modifier.size(44.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (group.isSpam) Color(0xFFDC2626).copy(alpha = 0.15f) else typeColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (group.isSpam) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Report,
                                    contentDescription = "Spam Caller",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else if (!call.photoUri.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = call.photoUri,
                                contentDescription = call.callerName ?: call.phoneNumber,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!call.callerName.isNullOrBlank()) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = call.callerName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = typeColor,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = typeLabel,
                                    tint = typeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (!call.photoUri.isNullOrBlank() || !call.callerName.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = typeLabel,
                                    tint = typeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = call.callerName?.ifBlank { call.phoneNumber } ?: call.phoneNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (group.isSpam) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                        )
                        if (group.count > 1) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "(${group.count})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (group.isSpam) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = group.spamDetails?.label ?: "Suspected Spam Caller",
                                    fontSize = 11.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!call.callerName.isNullOrBlank() && call.callerName != call.phoneNumber) {
                        Text(
                            text = call.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (call.durationSeconds > 0) {
                            Text(
                                text = "• ${call.durationSeconds}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        // Call Type Distinct Pill
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = typeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        // SIM Slot Indicator Pill
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "SIM ${call.simSlot}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (!call.ruleMatched.isNullOrBlank()) {
                        Column(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = call.ruleMatched,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Executed: Auto-answered + DTMF 9#",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Contextual Call Reason Badge
                    if (!call.callReason.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Reason: ${call.callReason}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Live Community Caller ID Badge
                    val communityTag = call.communityTag ?: com.example.util.CommunityCallerIdService.lookup(call.phoneNumber)?.name
                    if (!communityTag.isNullOrBlank() && !group.isSpam) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE0F2FE)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = communityTag,
                                    fontSize = 10.sp,
                                    color = Color(0xFF0369A1),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Post-call Note badge
                    if (!call.note.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = call.note,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Post-call Reminder badge
                    if (call.reminderTime != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Reminder Set",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            var showOverflowMenu by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // One-tap Call Back Button
                FilledIconButton(
                    onClick = onCallBack,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Back",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // One-tap Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Remove Favorite" else "Add Favorite",
                        tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // More Options Menu (Note, Spam, Rule)
                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (!call.note.isNullOrBlank() || call.reminderTime != null) "Edit Note / Reminder" else "Add Note / Reminder") },
                            leadingIcon = {
                                Icon(Icons.Default.EditNote, contentDescription = null)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenNoteDialog(call)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (group.isSpam) "Unmark as Spam" else "Report as Spam") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (group.isSpam) Icons.Default.Security else Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (group.isSpam) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onToggleSpam()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create Automation Rule") },
                            leadingIcon = {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onCreateRule()
                            }
                        )
                    }
                }
            }
        }
    }
}
