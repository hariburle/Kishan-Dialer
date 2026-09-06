package com.example.ui.components

import android.telecom.CallAudioState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AudioRouteOption(
    val route: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun AudioOutputSelectorDialog(
    currentRoute: Int,
    supportedRoutes: Int,
    bluetoothDeviceName: String?,
    onSelectRoute: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = mutableListOf<AudioRouteOption>()

    // Handset / Earpiece
    options.add(
        AudioRouteOption(
            route = CallAudioState.ROUTE_EARPIECE,
            title = "Phone / Handset",
            subtitle = "Internal earpiece speaker",
            icon = Icons.Default.PhoneAndroid
        )
    )

    // Speaker
    options.add(
        AudioRouteOption(
            route = CallAudioState.ROUTE_SPEAKER,
            title = "Speakerphone",
            subtitle = "Hands-free loud speaker",
            icon = Icons.AutoMirrored.Filled.VolumeUp
        )
    )

    // Bluetooth (always provide option so user can connect or route to Bluetooth)
    val btTitle = bluetoothDeviceName ?: "Bluetooth Device"
    options.add(
        AudioRouteOption(
            route = CallAudioState.ROUTE_BLUETOOTH,
            title = btTitle,
            subtitle = if (bluetoothDeviceName != null) "Connected audio device" else "Connected Bluetooth headset",
            icon = Icons.Default.BluetoothAudio
        )
    )

    // Wired Headset if supported
    if ((supportedRoutes and CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
        options.add(
            AudioRouteOption(
                route = CallAudioState.ROUTE_WIRED_HEADSET,
                title = "Wired Headset",
                subtitle = "Connected 3.5mm / USB-C headphones",
                icon = Icons.Default.Headphones
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Audio Output",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    val isSelected = currentRoute == option.route
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectRoute(option.route)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
