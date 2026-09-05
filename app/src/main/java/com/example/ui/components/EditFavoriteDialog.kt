package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.FavoriteContact

@Composable
fun EditFavoriteDialog(
    contact: FavoriteContact,
    onDismiss: () -> Unit,
    onSave: (updatedContact: FavoriteContact, newNickname: String?) -> Unit
) {
    var name by remember { mutableStateOf(contact.name) }
    var number by remember { mutableStateOf(contact.phoneNumber) }
    var label by remember { mutableStateOf(contact.label) }
    val labels = listOf("Mobile", "Home", "Work", "VIP", "Family")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Favorite Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name / Nickname") },
                    supportingText = { Text("Nickname is prioritized in Favorites & synced with Contacts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fav_name_input")
                )

                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fav_number_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Label", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        labels.forEach { option ->
                            FilterChip(
                                selected = label == option,
                                onClick = { label = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (number.isNotBlank()) {
                        val updated = contact.copy(
                            name = name.ifBlank { contact.name },
                            phoneNumber = number.trim(),
                            label = label
                        )
                        onSave(updated, name.trim())
                    }
                },
                modifier = Modifier.testTag("edit_fav_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_fav_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
