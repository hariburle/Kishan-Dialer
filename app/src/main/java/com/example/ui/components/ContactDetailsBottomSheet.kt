package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.AppDatabase
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android Phone Dialer-style Contact Card Bottom Sheet.
 * Displays full contact details, quick action bar (Call, Message, WhatsApp, Star Favorite),
 * and an interactive list of all phone numbers.
 * Long-pressing any phone number opens options to set or clear as default number, copy, WhatsApp, or automate.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactDetailsBottomSheet(
    contact: DeviceContact,
    favoriteContact: FavoriteContact?,
    isFavorite: Boolean,
    onCallNumber: (String) -> Unit,
    onSelectInDialer: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onSetAsDefaultNumber: (number: String, label: String) -> Unit,
    onClearDefaultNumber: () -> Unit,
    onCreateRule: (String) -> Unit,
    onSyncToGoogle: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Current default/primary number for this contact - reactive to user changes
    var currentDefaultNumber by remember(favoriteContact?.phoneNumber, contact.phoneNumber) {
        mutableStateOf(favoriteContact?.phoneNumber ?: contact.phoneNumber)
    }
    var numberForActionMenu by remember { mutableStateOf<ContactPhoneNumber?>(null) }

    var contactCallHistory by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(true) }

    LaunchedEffect(contact) {
        isLoadingHistory = true
        withContext(Dispatchers.IO) {
            val phoneNumbers = contact.phoneNumbers.map { it.number } + listOfNotNull(contact.phoneNumber.ifBlank { null })
            val db = AppDatabase.getInstance(context)
            val localHistory = db.appDao().getCallHistoryForContactList(phoneNumbers, contact.name)
            val deviceHistory = ContactHelper.fetchDeviceCallHistoryForContact(context, phoneNumbers, contact.name)

            // Merge local and device call logs, eliminating close duplicates
            val merged = (localHistory + deviceHistory)
                .distinctBy { "${it.phoneNumber}_${it.timestamp / 10000}_${it.callType}" }
                .sortedByDescending { it.timestamp }
                .take(40)

            contactCallHistory = merged
            isLoadingHistory = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(3.dp)
            ) {
                Spacer(modifier = Modifier.size(width = 38.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Avatar, Name, and Close Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Contact Avatar
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(76.dp),
                        shadowElevation = 2.dp
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
                                    fontSize = 32.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!contact.nickname.isNullOrBlank()) {
                        Text(
                            text = "\"${contact.nickname}\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (contact.label.isNotBlank() && contact.label != "Mobile") {
                        Text(
                            text = contact.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Action Buttons Bar (Call, Text, WhatsApp, Star Favorite)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Default Button
                ActionRoundButton(
                    icon = Icons.Default.Call,
                    label = "Call",
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White,
                    onClick = {
                        val numToCall = currentDefaultNumber.ifBlank {
                            contact.phoneNumbers.firstOrNull()?.number ?: ""
                        }
                        if (numToCall.isNotBlank()) {
                            onDismiss()
                            onCallNumber(numToCall)
                        }
                    }
                )

                // Message / SMS Button
                ActionRoundButton(
                    icon = Icons.AutoMirrored.Filled.Message,
                    label = "Message",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        val numToSms = currentDefaultNumber.ifBlank {
                            contact.phoneNumbers.firstOrNull()?.number ?: ""
                        }
                        if (numToSms.isNotBlank()) {
                            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$numToSms"))
                            context.startActivity(smsIntent)
                        }
                    }
                )

                // WhatsApp Voice Call Button
                ActionRoundButton(
                    icon = Icons.Default.Chat,
                    label = "WhatsApp",
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White,
                    onClick = {
                        val numToWa = currentDefaultNumber.ifBlank {
                            contact.phoneNumbers.firstOrNull()?.number ?: ""
                        }
                        if (numToWa.isNotBlank()) {
                            ContactHelper.launchWhatsAppCall(context, numToWa)
                        }
                    }
                )

                // Favorite Star Toggle Button
                ActionRoundButton(
                    icon = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    label = if (isFavorite) "Favorited" else "Favorite",
                    containerColor = if (isFavorite) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isFavorite) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggleFavorite
                )
            }

            if (contact.isAppOnly && onSyncToGoogle != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App-Only Contact",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Saved locally. Sync to Google Contacts to make it available system-wide.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSyncToGoogle()
                                Toast.makeText(context, "Synced ${contact.name} to Google Contacts!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Sync", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Phone Numbers Header & Info Hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PHONE NUMBERS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap to call • Long press options",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // List of Phone Numbers with Dialer Card Look
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    contact.phoneNumbers.forEachIndexed { index, pn ->
                        val normPn = pn.number.replace(Regex("[^0-9+]"), "")
                        val normDef = currentDefaultNumber.replace(Regex("[^0-9+]"), "")
                        val isDefault = (normDef.isNotBlank() && normPn == normDef) || (contact.phoneNumbers.size == 1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    when {
                                        contact.phoneNumbers.size == 1 -> RoundedCornerShape(16.dp)
                                        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                        index == contact.phoneNumbers.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                        else -> RoundedCornerShape(0.dp)
                                    }
                                )
                                .combinedClickable(
                                    onClick = {
                                        onDismiss()
                                        onCallNumber(pn.number)
                                    },
                                    onLongClick = {
                                        numberForActionMenu = pn
                                    }
                                )
                                .background(
                                    if (isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = pn.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (isDefault) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFF59E0B),
                                            contentColor = Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "DEFAULT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = pn.number,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Star / Default toggle button
                                IconButton(
                                    onClick = {
                                        if (isDefault && contact.phoneNumbers.size > 1 && favoriteContact != null) {
                                            currentDefaultNumber = ""
                                            onClearDefaultNumber()
                                            Toast.makeText(context, "Default number cleared", Toast.LENGTH_SHORT).show()
                                        } else {
                                            currentDefaultNumber = pn.number
                                            onSetAsDefaultNumber(pn.number, pn.label)
                                            Toast.makeText(context, "★ Set as default: ${pn.number}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDefault) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Toggle default number",
                                        tint = if (isDefault) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Quick Call Button
                                FilledIconButton(
                                    onClick = {
                                        onDismiss()
                                        onCallNumber(pn.number)
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFF16A34A),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call ${pn.number}",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (index < contact.phoneNumbers.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // CALL HISTORY SECTION (Native Android Dialer Style)
            // -------------------------------------------------------------
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "CALL HISTORY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (contactCallHistory.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${contactCallHistory.size} calls",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoadingHistory) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (contactCallHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "No call history found",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Calls with ${contact.name} will be logged here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        contactCallHistory.forEachIndexed { idx, call ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onCallNumber(call.phoneNumber)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Call Type Icon
                                    val (icon, tint, typeLabel) = when (call.callType) {
                                        1 -> Triple(
                                            Icons.AutoMirrored.Filled.CallReceived,
                                            Color(0xFF16A34A),
                                            "Incoming"
                                        )
                                        2 -> Triple(
                                            Icons.AutoMirrored.Filled.CallMade,
                                            Color(0xFF2563EB),
                                            "Outgoing"
                                        )
                                        3 -> Triple(
                                            Icons.AutoMirrored.Filled.CallMissed,
                                            Color(0xFFDC2626),
                                            "Missed"
                                        )
                                        else -> Triple(
                                            Icons.Default.Call,
                                            Color(0xFF4B5563),
                                            "Call"
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = tint.copy(alpha = 0.12f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = typeLabel,
                                                tint = tint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = typeLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (call.callType == 3) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (call.durationSeconds > 0) {
                                                val mins = call.durationSeconds / 60
                                                val secs = call.durationSeconds % 60
                                                val durText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                                                Text(
                                                    text = "• $durText",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${dateFormat.format(Date(call.timestamp))} • ${call.phoneNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (!call.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Notes,
                                                    contentDescription = "Note",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = call.note,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        onCallNumber(call.phoneNumber)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (idx < contactCallHistory.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Long-press Action Dialog for a selected phone number (Android Dialer Style)
    if (numberForActionMenu != null) {
        val selectedPn = numberForActionMenu!!
        val normSel = selectedPn.number.replace(Regex("[^0-9+]"), "")
        val normDef = currentDefaultNumber.replace(Regex("[^0-9+]"), "")
        val isCurrentDefault = normDef.isNotBlank() && normSel == normDef

        AlertDialog(
            onDismissRequest = { numberForActionMenu = null },
            title = {
                Column {
                    Text(
                        text = selectedPn.number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${contact.name} • ${selectedPn.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Call Number
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numberForActionMenu = null
                                onDismiss()
                                onCallNumber(selectedPn.number)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color(0xFF16A34A))
                            Text(text = "Call ${selectedPn.number}", fontWeight = FontWeight.Medium)
                        }
                    }

                    // Set or Clear Default Number
                    if (!isCurrentDefault) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentDefaultNumber = selectedPn.number
                                    onSetAsDefaultNumber(selectedPn.number, selectedPn.label)
                                    Toast.makeText(context, "★ Set as default: ${selectedPn.number}", Toast.LENGTH_SHORT).show()
                                    numberForActionMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                                Column {
                                    Text(text = "Set as default number", fontWeight = FontWeight.Bold)
                                    Text(text = "Use this number when calling or speed-dialing", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentDefaultNumber = ""
                                    onClearDefaultNumber()
                                    Toast.makeText(context, "Default number cleared", Toast.LENGTH_SHORT).show()
                                    numberForActionMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.StarBorder, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(text = "Clear default number", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Open in Dialer
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numberForActionMenu = null
                                onDismiss()
                                onSelectInDialer(selectedPn.number)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(text = "Load in Dialer keypad", fontWeight = FontWeight.Medium)
                        }
                    }

                    // Copy Number
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(selectedPn.number))
                                Toast.makeText(context, "Copied ${selectedPn.number}", Toast.LENGTH_SHORT).show()
                                numberForActionMenu = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Copy number", fontWeight = FontWeight.Medium)
                        }
                    }

                    // WhatsApp Call
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numberForActionMenu = null
                                ContactHelper.launchWhatsAppCall(context, selectedPn.number)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF25D366),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("WA", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(text = "Call on WhatsApp", fontWeight = FontWeight.Medium)
                        }
                    }

                    // Create Automation Rule
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numberForActionMenu = null
                                onDismiss()
                                onCreateRule(selectedPn.number)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(text = "Create automation rule", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { numberForActionMenu = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActionRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconText: String? = null,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (iconText != null) {
                    Text(
                        text = iconText,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
