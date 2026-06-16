package com.example.touchlessdroid.domain.usecase

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import androidx.annotation.RequiresPermission
import com.example.touchlessdroid.data.repository.BluetoothDataTransfer
import com.example.touchlessdroid.data.repository.broadcast_receiver.BluetoothConnectionStateReceiver
import com.example.touchlessdroid.data.repository.broadcast_receiver.FoundDeviceReceiver
import com.example.touchlessdroid.domain.model.bluetooth.BlDataTransferStatus
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothDeviceLocal
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothConnectionStatus
import com.example.touchlessdroid.domain.model.bluetooth.toBluetoothDeviceLocal
import com.example.touchlessdroid.domain.model.camera.RobotCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context):
    BluetoothDataTransfer {

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    //broadcast receiver -----------------------------------------------
    //broadcast receiver to get bluetooth device list when search
    private val _foundDevices = MutableStateFlow<List<BluetoothDeviceLocal>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDeviceLocal>>
        get() = _foundDevices.asStateFlow()
    private val foundDeviceBroadcastReceiver = FoundDeviceReceiver { device ->
        _foundDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceLocal()
            if (newDevice in devices) devices else devices + newDevice
        }
    }
    //broadcast receiver - when a connection is interrupted with an existed connected device
    private val _connectionState = MutableStateFlow(BluetoothConnectionStatus.DISCONNECTED)
    val connectionState: StateFlow<BluetoothConnectionStatus> = _connectionState
    private val bluetoothStateReceiver = BluetoothConnectionStateReceiver{ status, bluetoothDevice ->
        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true){
            _connectionState.update {status}
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.emit("Disconnected: can't connect to a non-paired device")
                _connectionState.update {status}
            }
        }
    }
    //end -- broadcast receiver -----------------------------------------------


    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceLocal>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceLocal>>
        get() = _pairedDevices.asStateFlow()

    init {
        updatePairedDevises()
        //register bluetoothConnectionStateReceiver
        context.registerReceiver(
            bluetoothStateReceiver,
            //intent_filter take one arguments
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }



    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startDiscovery() {
        _foundDevices.value = emptyList()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

        context.registerReceiver(foundDeviceBroadcastReceiver, filter)

        bluetoothAdapter?.startDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        try {
            context.unregisterReceiver(foundDeviceBroadcastReceiver)
        } catch (_: Exception) {}
    }



    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ]
    )
    fun connect(device: BluetoothDevice) {
        _connectionState.value = BluetoothConnectionStatus.CONNECTING

        try {
            val uuid: UUID =
                device.uuids?.firstOrNull()?.uuid
                    ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            socket = device.createRfcommSocketToServiceRecord(uuid)

            bluetoothAdapter?.cancelDiscovery()

            socket?.connect()
            outputStream = socket?.outputStream

            _connectionState.value = BluetoothConnectionStatus.CONNECTED

        } catch (e: Exception) {
            e.printStackTrace()
            _connectionState.value = BluetoothConnectionStatus.DISCONNECTED
        }
    }

    fun send(command: String) {
        outputStream?.write((command + "\n").toByteArray())
    }

    override suspend fun sendCommand(command: RobotCommand): BlDataTransferStatus {
        return try {
            val stream = outputStream
                ?: return BlDataTransferStatus.NotConnected

            withContext(Dispatchers.IO) {
                stream.write((command.name + "\n").toByteArray())
            }

            BlDataTransferStatus.Success

        } catch (e: Exception) {
            BlDataTransferStatus.Error(e.message ?: "Unknown error")
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {}

        _connectionState.value = BluetoothConnectionStatus.DISCONNECTED
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