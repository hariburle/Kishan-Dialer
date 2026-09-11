package com.example.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FavoriteContact
import com.example.util.ContactHelper
import com.example.util.DeviceContact

@Composable
fun ContactPickerDialog(
    favorites: List<FavoriteContact> = emptyList(),
    deviceContacts: List<DeviceContact> = emptyList(),
    onContactSelected: (name: String, number: String, photoUri: String?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Contact",
    onManualAddClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var loadedContacts by remember { mutableStateOf(deviceContacts) }

    // System Contact Picker contract launcher
    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
            val contactData = ContactHelper.extractContactFromUri(context, result.data!!.data!!)
            if (contactData != null) {
                onContactSelected(contactData.name, contactData.phoneNumber, contactData.photoUri)
                onDismiss()
            }
        }
    }

    LaunchedEffect(deviceContacts) {
        if (deviceContacts.isNotEmpty()) {
            loadedContacts = deviceContacts
        } else {
            val fetched = ContactHelper.fetchDeviceContacts(context)
            loadedContacts = fetched
        }
    }

    val combinedList = remember(loadedContacts, favorites) {
        val list = mutableListOf<DeviceContact>()
        favorites.forEach { fav ->
            list.add(DeviceContact(fav.name, fav.phoneNumber, fav.label, fav.photoUri))
        }
        loadedContacts.forEach { dc ->
            if (list.none { it.phoneNumber == dc.phoneNumber }) {
                list.add(dc)
            }
        }
        list
    }

    val filteredContacts = combinedList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        ContactHelper.matchesNumberQuery(it.phoneNumber, searchQuery)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button to launch system native Android Contact Picker
                OutlinedButton(
                    onClick = {
                        try {
                            systemPickerLauncher.launch(ContactHelper.createContactPickerIntent())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("launch_system_picker_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Open Phone Contacts App")
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or number") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_search_field"),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "Select a Contact:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Contact list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(filteredContacts) { contact ->
                        val isFav = favorites.any { it.phoneNumber == contact.phoneNumber }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val selectedContactName = contact.nickname?.ifBlank { null } ?: contact.name
                                    onContactSelected(selectedContactName, contact.phoneNumber, contact.photoUri)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                                        coil.compose.AsyncImage(
                                            model = contact.photoUri,
                                            contentDescription = contact.name,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isFav) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Favorite",
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        if (contact.phoneNumbers.size > 1) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "${contact.phoneNumbers.size} numbers",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (!contact.nickname.isNullOrBlank()) {
                                        Text(
                                            text = contact.nickname,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "${contact.phoneNumber} • ${contact.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // If contact has multiple numbers, show other numbers for easy selection
                        if (contact.phoneNumbers.size > 1) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 52.dp, bottom = 6.dp)
                            ) {
                                contact.phoneNumbers.filter { it.number != contact.phoneNumber }.forEach { pn ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                val selectedContactName = contact.nickname?.ifBlank { null } ?: contact.name
                                                onContactSelected(selectedContactName, pn.number, contact.photoUri)
                                                onDismiss()
                                            }
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${pn.label}: ${pn.number}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Select",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onManualAddClick != null) {
                    TextButton(onClick = {
                        onDismiss()
                        onManualAddClick()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New Number")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
