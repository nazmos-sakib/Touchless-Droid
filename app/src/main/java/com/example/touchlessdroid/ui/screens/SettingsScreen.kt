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

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        Text("• Camera settings")
        Text("• Bluetooth preferences")
        Text("• App configuration")
    }
}