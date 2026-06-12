package com.example.touchlessdroid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.touchlessdroid.ui.viewmodel.BluetoothViewModel

@androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT])
@Composable
fun PairDeviceScreen(bluetoothViewModel: BluetoothViewModel) {

    val devices by bluetoothViewModel.devices.collectAsState()
    val status by bluetoothViewModel.status.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            bluetoothViewModel.stopScan()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text("Pair New Device", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            bluetoothViewModel.startScan()
        }) {
            Text("Scan Devices")
        }

        Spacer(Modifier.height(16.dp))

        Text("Status: $status")

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(devices) { device ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable  {
                            bluetoothViewModel.connect(device)
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(device.name ?: "Unknown Device")
                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}