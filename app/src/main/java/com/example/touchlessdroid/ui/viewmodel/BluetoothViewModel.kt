package com.example.touchlessdroid.ui.viewmodel

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothDeviceLocal
import com.example.touchlessdroid.domain.usecase.BluetoothManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = BluetoothManager(application)
    val status = manager.connectionState
    val devices = manager.foundDevices

    // saved devices list
    val pairedDevices =  manager.pairedDevices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        manager.startDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        manager.stopDiscovery()
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun connect(device: BluetoothDevice) {
        manager.connect(device)
    }

    fun disconnect() =
        manager.disconnect()

    fun isBluetoothEnabled(): Boolean {
        return manager.isBluetoothEnabled()
    }

    // connect using saved device
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun connectSavedDevice(bluetoothDevice: com.example.touchlessdroid.domain.model.bluetooth.BluetoothDevice) {
        val device = manager.getBlAdapter()
            .getRemoteDevice(bluetoothDevice.address)

        manager.connect(device)
    }


}