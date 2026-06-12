package com.example.touchlessdroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.touchlessdroid.ui.viewmodel.BluetoothViewModel

@Composable
fun HomeScreen(
    bluetoothViewModel: BluetoothViewModel,
    onStartClick: () -> Unit
) {
    val status by bluetoothViewModel.status.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (!bluetoothViewModel.isBluetoothEnabled()) {
            Text("⚠️ Bluetooth is OFF")
        }

        Spacer(Modifier.height(16.dp))

        Text("Bluetooth Status: $status")

        Spacer(Modifier.height(16.dp))

        Button(onClick = onStartClick) {
            Text("Start Camera")
        }
    }
}