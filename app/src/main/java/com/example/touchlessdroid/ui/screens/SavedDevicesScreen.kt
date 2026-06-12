package com.example.touchlessdroid.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.touchlessdroid.ui.viewmodel.BluetoothViewModel

@Composable
fun SavedDevicesScreen(bluetoothViewModel: BluetoothViewModel) {

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text("Saved Devices", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            bluetoothViewModel.setConnected()
        }) {
            Text("Reconnect to Saved Device")
        }
    }
}