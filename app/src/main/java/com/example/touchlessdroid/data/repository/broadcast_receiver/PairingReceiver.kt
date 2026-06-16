package com.example.touchlessdroid.data.repository.broadcast_receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothDeviceLocal
import com.example.touchlessdroid.domain.model.bluetooth.toBluetoothDeviceLocal

class PairingReceiver(
    private val onDevicePaired: (BluetoothDeviceLocal) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val device: BluetoothDevice? = getBluetoothDeviceFromIntent(intent)
                val bondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                when (bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        // Device is successfully paired
                        Toast.makeText(context, "Paired with ${device?.toBluetoothDeviceLocal()?.name}", Toast.LENGTH_SHORT)
                            .show()
                        device?.let { onDevicePaired(it.toBluetoothDeviceLocal()) }  // Connect to the device after pairing
                    }

                    BluetoothDevice.BOND_BONDING -> {
                        // Pairing in progress
                        Toast.makeText(context, "Pairing with ${device?.toBluetoothDeviceLocal()?.name}", Toast.LENGTH_SHORT)
                            .show()
                    }

                    BluetoothDevice.BOND_NONE -> {
                        // Pairing failed or unpaired
                        Toast.makeText(
                            context,
                            "Pairing failed with ${device?.toBluetoothDeviceLocal()?.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun getBluetoothDeviceFromIntent(intent: Intent):BluetoothDevice?{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_NAME)
        }
    }
}
