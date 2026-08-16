package com.example.touchlessdroid.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothConnectionStatus
import com.example.touchlessdroid.ui.viewmodel.BluetoothViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusBar(
    bluetoothViewModel: BluetoothViewModel,
    serverRunning: Boolean,
    onMenuClick: () -> Unit,
    onServerToggle: () -> Unit
) {

    val status by bluetoothViewModel.connectionStatus.collectAsState()

    val text = when (status) {
        BluetoothConnectionStatus.CONNECTED -> "Connected"
        BluetoothConnectionStatus.CONNECTING -> "Connecting..."
        BluetoothConnectionStatus.DISCONNECTED -> "Not Connected"
    }

    val color = when (status) {
        BluetoothConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
        BluetoothConnectionStatus.CONNECTING -> Color(0xFFFFA000)
        BluetoothConnectionStatus.DISCONNECTED -> Color(0xFFD32F2F)
    }

    //BuildInTopAppBar(text,color,onMenuClick)
    UserDefinedTopAppBar(
        text = text,
        color = color,
        serverRunning = serverRunning,
        onMenuClick = onMenuClick,
        onServerToggle = onServerToggle
    )



}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildInTopAppBar(
    text: String,
    color: Color,
    onMenuClick: () -> Unit
){
    TopAppBar(
        modifier = Modifier.padding(vertical = 0.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Text("BT: $text")
        },
        navigationIcon = {
            IconButton(modifier = Modifier.padding(0.dp),onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = color
        )
    )
}

@Composable
fun UserDefinedTopAppBar(
    text: String,
    color: Color,
    serverRunning: Boolean,
    onMenuClick: () -> Unit,
    onServerToggle: () -> Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(35.dp)
        ) {
            Icon(
                //modifier = Modifier.padding(0.dp),
                imageVector = Icons.Default.Menu,
                contentDescription = null,
                tint = Color.White
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            text = "BT: $text",
            color = Color.White
        )

        IconButton(
            onClick = onServerToggle,
            modifier = Modifier.size(35.dp)
        ) {
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = if (serverRunning) {
                        "Stop server"
                    } else {
                        "Start server"
                    },
                    tint = Color.White
                )

                if (!serverRunning) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFFFD54F)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun UserDefinedTopAppBarPreview() {
    Column {
        UserDefinedTopAppBar(
            text = "Connected",
            color = Color(0xFF4CAF50),
            serverRunning = true,
            onMenuClick = {},
            onServerToggle = {}
        )

        Spacer(modifier = Modifier.size(8.dp))

        UserDefinedTopAppBar(
            text = "Not Connected",
            color = Color(0xFFD32F2F),
            serverRunning = false,
            onMenuClick = {},
            onServerToggle = {}
        )
    }
}
