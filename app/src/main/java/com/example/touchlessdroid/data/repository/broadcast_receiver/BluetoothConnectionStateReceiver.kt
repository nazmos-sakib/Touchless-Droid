package com.example.touchlessdroid.data.repository.broadcast_receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothConnectionStatus


class BluetoothConnectionStateReceiver(
    private val onStateChange:(connectionStatus: BluetoothConnectionStatus, BluetoothDevice) -> Unit
): BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java
            )
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_NAME)
        }
        when(intent.action){
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                onStateChange(BluetoothConnectionStatus.CONNECTED,device?:return)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                onStateChange(BluetoothConnectionStatus.DISCONNECTED,device?:return)
            }
        }
    }

}