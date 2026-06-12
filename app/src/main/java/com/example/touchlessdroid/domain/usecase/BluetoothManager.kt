package com.example.touchlessdroid.domain.usecase

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothDeviceLocal
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothStatus
import com.example.touchlessdroid.domain.model.bluetooth.toBluetoothDeviceLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceLocal>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceLocal>>
        get() = _pairedDevices.asStateFlow()

    init {
        updatePairedDevises()
    }

    private val _connectionState = MutableStateFlow(BluetoothStatus.DISCONNECTED)
    val connectionState: StateFlow<BluetoothStatus> = _connectionState

    private var socket: BluetoothSocket? = null

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startDiscovery() {
        _foundDevices.value = emptyList()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

        context.registerReceiver(receiver, filter)

        bluetoothAdapter?.startDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                device?.let {
                    _foundDevices.value += it
                }
            }
        }
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ]
    )
    fun connect(device: BluetoothDevice) {
        _connectionState.value = BluetoothStatus.CONNECTING

        Thread   {
            try {
                val uuid: UUID =
                    device.uuids?.firstOrNull()?.uuid
                        ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                socket = device.createRfcommSocketToServiceRecord(uuid)

                bluetoothAdapter?.cancelDiscovery()

                socket?.connect()

                _connectionState.value = BluetoothStatus.CONNECTED

            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = BluetoothStatus.DISCONNECTED
            }
        }.start()
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {}

        _connectionState.value = BluetoothStatus.DISCONNECTED
    }

    fun getBlAdapter() : BluetoothAdapter = bluetoothAdapter

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun updatePairedDevises(){

        bluetoothAdapter
            ?.bondedDevices
            ?.map{
                it.toBluetoothDeviceLocal()
            }
            ?.also { devices->
                //println("device size: ${devices.size}")
                _pairedDevices.update { devices }
            }
    }

}