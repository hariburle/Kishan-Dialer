package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ContactSaveDestination {
    GOOGLE_DEVICE,
    APP_ONLY
}

@Composable
fun CreateContactDialog(
    initialNumber: String = "",
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var number by remember { mutableStateOf(initialNumber) }
    var label by remember { mutableStateOf("Mobile") }
    var saveDestination by remember { mutableStateOf(ContactSaveDestination.GOOGLE_DEVICE) }
    var addToFavorites by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Contact",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_contact_name_input")
                )

                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_contact_number_input")
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Mobile, Work, Home)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_contact_label_input")
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Save Location",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Google / Device Contacts
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (saveDestination == ContactSaveDestination.GOOGLE_DEVICE) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveDestination = ContactSaveDestination.GOOGLE_DEVICE }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = saveDestination == ContactSaveDestination.GOOGLE_DEVICE,
                            onClick = { saveDestination = ContactSaveDestination.GOOGLE_DEVICE }
                        )
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Column {
                            Text(
                                text = "Google / Device Contacts",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Syncs automatically to your Google account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: App Only (Local)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (saveDestination == ContactSaveDestination.APP_ONLY) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveDestination = ContactSaveDestination.APP_ONLY }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Column {
                            Text(
                                text = "App Only (Local)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Kept private inside dialer, syncable later",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Optional: Also Add to Favorites checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addToFavorites = !addToFavorites }
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = addToFavorites,
                        onCheckedChange = { addToFavorites = it }
                    )
                    Text(
                        text = "Add to Favorites",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && number.isNotBlank()) {
                        onSave(name.trim(), number.trim(), label.trim().ifBlank { "Mobile" }, saveDestination, addToFavorites)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("create_contact_save_btn")
            ) {
                Text("Save Contact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
