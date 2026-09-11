package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ContactHelper

enum class ContactSaveDestination {
    PHONE_CONTACTS,
    APP_ONLY
}

@Composable
fun CreateContactDialog(
    initialNumber: String = "",
    initialName: String = "",
    dialogTitle: String = "New Contact",
    initialAddToFavorites: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var number by remember { mutableStateOf(initialNumber) }
    var label by remember { mutableStateOf("Mobile") }
    var saveDestination by remember { mutableStateOf(ContactSaveDestination.PHONE_CONTACTS) }
    var addToFavorites by remember { mutableStateOf(initialAddToFavorites) }

    val presetLabels = listOf("Mobile", "Home", "Work", "Other")

    LaunchedEffect(number) {
        if (number.isNotBlank() && name.isBlank()) {
            val contact = ContactHelper.lookupContactByNumber(context, number)
            if (contact != null) {
                name = contact.name
                if (label.isBlank() || label == "Mobile") {
                    label = contact.label
                }
            }
        }
    }

    val initials = name.trim().take(1).uppercase().ifBlank {
        if (number.isNotBlank()) "#" else "?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dynamic Avatar Preview Monogram
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp),
                    shadowElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = dialogTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (initialAddToFavorites) "Create & add directly to favorites" else "Add contact with details & destination",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name Field with Person Icon
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Sarah Connor") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_contact_name_input")
                )

                // Phone Number Field with Phone Icon
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 (555) 000-0000") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_contact_number_input")
                )

                // Label selection chips & custom input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Phone Label",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetLabels.forEach { preset ->
                            FilterChip(
                                selected = label.equals(preset, ignoreCase = true),
                                onClick = { label = preset },
                                label = { Text(preset, fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Destination options
                Text(
                    text = "Save Location",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Phone Contacts (Google / Device synced)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (saveDestination == ContactSaveDestination.PHONE_CONTACTS) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveDestination = ContactSaveDestination.PHONE_CONTACTS }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = saveDestination == ContactSaveDestination.PHONE_CONTACTS,
                            onClick = { saveDestination = ContactSaveDestination.PHONE_CONTACTS }
                        )
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp).size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Device & Google Account",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Synced across your Android contacts & Google account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: App Only (Local)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (saveDestination == ContactSaveDestination.APP_ONLY) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveDestination = ContactSaveDestination.APP_ONLY }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = saveDestination == ContactSaveDestination.APP_ONLY,
                            onClick = { saveDestination = ContactSaveDestination.APP_ONLY }
                        )
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp).size(22.dp)
                        )
                        Column {
                            Text(
                                text = "App Only (Local)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Private offline entry inside dialer, syncable later",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Optional: Add to Favorites checkbox
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (addToFavorites) Color(0xFFFEF3C7) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addToFavorites = !addToFavorites }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Checkbox(
                            checked = addToFavorites,
                            onCheckedChange = { addToFavorites = it }
                        )
                        Icon(
                            imageVector = if (addToFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (addToFavorites) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Add to Favorites for instant speed dialing",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (addToFavorites) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (addToFavorites) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && number.isNotBlank()) {
                        onSave(name.trim(), number.trim(), label.trim().ifBlank { "Mobile" }, saveDestination, addToFavorites)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank() && number.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("create_contact_save_btn")
            ) {
                Text("Save Contact", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
