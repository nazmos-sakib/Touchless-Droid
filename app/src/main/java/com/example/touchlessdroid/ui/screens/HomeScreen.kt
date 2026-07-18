package com.example.touchlessdroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.ui.screens.components.OptionSelector
import com.example.touchlessdroid.ui.viewmodel.BluetoothViewModel
import com.example.touchlessdroid.utils.DelegateOption
import com.example.touchlessdroid.utils.PrecisionOption
import com.example.touchlessdroid.utils.RuntimeOption

@Composable
fun HomeScreen(
    bluetoothViewModel: BluetoothViewModel,
    onStartClick: (InferenceConfiguration) -> Unit
) {
    val bluetoothStatus by
    bluetoothViewModel.connectionStatus.collectAsState()

    var selectedRuntime by rememberSaveable {
        mutableStateOf(RuntimeOption.TFLITE)
    }

    var selectedDelegate by rememberSaveable {
        mutableStateOf(DelegateOption.CPU)
    }

    var selectedPrecision by rememberSaveable {
        mutableStateOf(PrecisionOption.FP32)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!bluetoothViewModel.isBluetoothEnabled()) {
            Text(
                text = "Bluetooth is OFF",
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        Text("Bluetooth Status: $bluetoothStatus")

        Spacer(modifier = Modifier.height(32.dp))

        OptionSelector(
            title = "Runtime",
            options = RuntimeOption.entries,
            selectedOption = selectedRuntime,
            label = { it.label },
            onSelected = { runtime ->
                selectedRuntime = runtime

                if (selectedDelegate !in runtime.supportedDelegates) {
                    selectedDelegate = runtime.supportedDelegates.first()
                }

                if (selectedPrecision !in runtime.supportedPrecisions) {
                    selectedPrecision = runtime.supportedPrecisions.first()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        OptionSelector(
            title = "Delegate",
            options = selectedRuntime.supportedDelegates,
            selectedOption = selectedDelegate,
            label = { it.label },
            onSelected = { delegate ->
                selectedDelegate = delegate
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        OptionSelector(
            title = "Precision",
            options = selectedRuntime.supportedPrecisions,
            selectedOption = selectedPrecision,
            label = { it.label },
            onSelected = { precision ->
                selectedPrecision = precision
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onStartClick(
                    InferenceConfiguration(
                        runtime = selectedRuntime,
                        delegate = selectedDelegate,
                        precision = selectedPrecision
                    )
                )
            }
        ) {
            Text("Start Camera")
        }
    }
}