package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp

@Composable
fun AddFavoriteDialog(
    initialNumber: String = "",
    initialName: String = "",
    initialPhotoUri: String? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onPickFromContacts: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var number by remember { mutableStateOf(initialNumber) }
    var label by remember { mutableStateOf("Mobile") }
    var photoUri by remember { mutableStateOf(initialPhotoUri) }

    // Auto-lookup contact if number is provided and name is empty
    androidx.compose.runtime.LaunchedEffect(number) {
        if (number.isNotBlank() && name.isBlank()) {
            val contact = com.example.util.ContactHelper.lookupContactByNumber(context, number)
            if (contact != null) {
                name = contact.name
                label = contact.label
                photoUri = contact.photoUri
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Favorites") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_fav_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_fav_number_input")
                    )

                    IconButton(
                        onClick = onPickFromContacts,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "Pick Contact"
                        )
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Mobile, Family, Work, Gate)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && number.isNotBlank()) {
                        onSave(name.trim(), number.trim(), label.trim().ifBlank { "Mobile" }, photoUri)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_favorite_confirm_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
