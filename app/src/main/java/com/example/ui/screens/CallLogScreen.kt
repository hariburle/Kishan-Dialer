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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
    modifier: Modifier = Modifier
) {
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
    onToggleSpam: () -> Unit,
    onToggleFavorite: () -> Unit
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (call.durationSeconds > 0) {
                            Text(
                                text = "• ${call.durationSeconds}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // SIM Slot Indicator Pill
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "SIM ${call.simSlot}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (!call.ruleMatched.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Automated: ${call.ruleMatched}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // One-tap Favorite Star (Android / iOS Recents style)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Remove Favorite" else "Add Favorite",
                        tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Mark / Unmark Spam
                IconButton(
                    onClick = onToggleSpam,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (group.isSpam) Icons.Default.Security else Icons.Default.Block,
                        contentDescription = if (group.isSpam) "Unblock Number" else "Report Spam",
                        tint = if (group.isSpam) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Create rule button
                IconButton(
                    onClick = onCreateRule,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Rule",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Call Back Button
                FilledIconButton(
                    onClick = onCallBack,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Back",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
