package com.example.touchlessdroid.ui.viewmodel

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touchlessdroid.data.repository.BluetoothDataTransfer
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothDeviceLocal
import com.example.touchlessdroid.domain.model.camera.RobotCommand
import com.example.touchlessdroid.domain.usecase.BluetoothManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStream

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val manager: BluetoothManager
) : ViewModel() {
    val connectionStatus = manager.connectionState
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
    fun connect(device: BluetoothDeviceLocal) {
        val device = manager.getBlAdapter()
            .getRemoteDevice(device.address)
        viewModelScope.launch (Dispatchers.IO) {
            manager.connect(device)
        }

    }
    private var lastSent: RobotCommand? = null
    fun sendCommand(command: RobotCommand) {
        if (command == lastSent) return
        lastSent = command

        viewModelScope.launch(Dispatchers.IO) {
            manager.send(command.name)
        }
    }

    fun disconnect() =
        manager.disconnect()

    fun isBluetoothEnabled(): Boolean {
        return manager.isBluetoothEnabled()
    }

    // connect using saved device
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun connectSavedDevice(device: BluetoothDeviceLocal) {
        val device = manager.getBlAdapter()
            .getRemoteDevice(device.address)

        manager.connect(device)
    }


}