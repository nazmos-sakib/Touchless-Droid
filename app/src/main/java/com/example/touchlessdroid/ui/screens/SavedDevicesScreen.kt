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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.touchlessdroid.ui.viewmodel.BluetoothViewModel

@androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT])
@Composable
fun SavedDevicesScreen(bluetoothViewModel: BluetoothViewModel) {

    val savedDevices by bluetoothViewModel.pairedDevices.collectAsState()
    val status by bluetoothViewModel.status.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text("Saved Devices", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        Text("Status: $status")

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(savedDevices) { device ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            bluetoothViewModel.connectSavedDevice(device)
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(device.name)
                        Text(device.address)
                    }
                }
            }
        }
    }
}