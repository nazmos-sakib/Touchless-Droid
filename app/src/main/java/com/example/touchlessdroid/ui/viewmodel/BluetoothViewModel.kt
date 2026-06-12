package com.example.touchlessdroid.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothViewModel : ViewModel() {

    private val _status = MutableStateFlow(BluetoothStatus.DISCONNECTED)
    val status: StateFlow<BluetoothStatus> = _status

    fun setConnected() {
        _status.value = BluetoothStatus.CONNECTED
    }

    fun setConnecting() {
        _status.value = BluetoothStatus.CONNECTING
    }

    fun setDisconnected() {
        _status.value = BluetoothStatus.DISCONNECTED
    }
}