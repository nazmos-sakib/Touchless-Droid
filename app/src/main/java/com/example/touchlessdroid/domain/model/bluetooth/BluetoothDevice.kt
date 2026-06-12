package com.example.touchlessdroid.domain.model.bluetooth

import android.annotation.SuppressLint

typealias BluetoothDeviceLocal = BluetoothDevice
data class BluetoothDevice(
    val name: String,
    val address: String,
    val type: Int,
    val bondState:Int
)

@SuppressLint("MissingPermission")
fun android.bluetooth.BluetoothDevice.toBluetoothDeviceLocal(): BluetoothDeviceLocal{
    return BluetoothDeviceLocal(
        name = name,
        address = address,
        type = type,
        bondState = bondState
    )
}