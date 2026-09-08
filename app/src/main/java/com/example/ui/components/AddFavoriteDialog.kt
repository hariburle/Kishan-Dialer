package com.example.ui.components

import androidx.compose.runtime.Composable

@Composable
fun AddFavoriteDialog(
    initialNumber: String = "",
    initialName: String = "",
    initialPhotoUri: String? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onPickFromContacts: () -> Unit = {}
) {
    CreateContactDialog(
        initialNumber = initialNumber,
        initialName = initialName,
        dialogTitle = "Add to Favorites",
        initialAddToFavorites = true,
        onDismiss = onDismiss,
        onSave = { name, number, label, destination, addToFavorites ->
            onSave(name, number, label, initialPhotoUri)
        }
    )
}
