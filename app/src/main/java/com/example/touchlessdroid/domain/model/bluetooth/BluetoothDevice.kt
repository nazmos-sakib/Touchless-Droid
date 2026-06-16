package com.example.touchlessdroid.domain.model.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.util.Log

typealias BluetoothDeviceLocal = BluetoothDevice
data class BluetoothDevice(
    val name: String,
    val address: String,
    val type: DeviceType,
    val bondState:Int
)

enum class DeviceType {
    PHONE,
    COMPUTER,
    HEADSET,
    CAR,
    UNKNOWN
}
@SuppressLint("MissingPermission")
fun android.bluetooth.BluetoothDevice.toBluetoothDeviceLocal(): BluetoothDeviceLocal{
    Log.d("TAG", "toBluetoothDeviceLocal:  name: $name  type: ${bluetoothClass.deviceClass}")
    return BluetoothDeviceLocal(
        name = name?.takeIf { it.isNotBlank() } ?: "Unnamed",
        address = address,
        type = bluetoothClass.toDeviceType(),
        bondState = bondState
    )
}

fun BluetoothClass?.toDeviceType(): DeviceType {
    return when (this?.deviceClass) {

        BluetoothClass.Device.COMPUTER_LAPTOP,
        BluetoothClass.Device.COMPUTER_DESKTOP -> DeviceType.COMPUTER

        BluetoothClass.Device.PHONE_SMART -> DeviceType.PHONE

        BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
        BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceType.HEADSET

        BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> DeviceType.CAR

        else -> DeviceType.UNKNOWN
    }
}