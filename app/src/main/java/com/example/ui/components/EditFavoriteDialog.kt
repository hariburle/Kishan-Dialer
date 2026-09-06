package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditFavoriteDialog(
    contact: FavoriteContact,
    onDismiss: () -> Unit,
    onSave: (updatedContact: FavoriteContact, newNickname: String?) -> Unit
) {
    var name by remember { mutableStateOf(contact.name) }
    var nickname by remember { mutableStateOf(contact.nickname.orEmpty()) }
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
                    label = { Text("Contact Name") },
                    placeholder = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fav_name_input")
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (optional)") },
                    placeholder = { Text("e.g. Mom, Alex, Boss") },
                    supportingText = { Text("Shown on keypad & speed dial shortcuts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fav_nickname_input")
                )

                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fav_number_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Label", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        labels.forEach { option ->
                            FilterChip(
                                selected = label == option,
                                onClick = { label = option },
                                label = {
                                    Text(
                                        text = option,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
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
                        val cleanNick = nickname.trim().takeIf { it.isNotBlank() }
                        val updated = contact.copy(
                            name = name.trim().ifBlank { contact.name },
                            nickname = cleanNick,
                            phoneNumber = number.trim(),
                            label = label
                        )
                        onSave(updated, cleanNick)
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
