package com.example.touchlessdroid.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothDeviceLocal
import com.example.touchlessdroid.domain.model.bluetooth.DeviceType


@Composable
fun BluetoothDeviceItem(
    device: BluetoothDeviceLocal,
    isConnected: Boolean = false,
    onItemClick: () -> Unit
) {
    val backgroundColor = when {
        isConnected -> Color(0xFFFFCDD2)   // light red
        device.type == DeviceType.HEADSET -> Color(0xFFFFE0B2) // orange
        device.type == DeviceType.COMPUTER -> Color(0xFFE1BEE7) // purple-ish
        else -> Color(0xFFFFE0B2)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{onItemClick()}
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 🔥 ICON CIRCLE (like your screenshot)
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(backgroundColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = deviceIcon(device.type),
                contentDescription = null,
                tint = Color(0xFF8D6E63),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // DEVICE NAME
        Text(
            text = device.name,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

@Composable
fun deviceIcon(type: DeviceType): ImageVector {
    return when (type) {
        DeviceType.PHONE -> Icons.Default.Smartphone
        DeviceType.COMPUTER -> Icons.Default.Laptop
        DeviceType.HEADSET -> Icons.Default.Headset
        DeviceType.CAR -> Icons.Default.DirectionsCar
        DeviceType.UNKNOWN -> Icons.Default.Bluetooth
    }
}