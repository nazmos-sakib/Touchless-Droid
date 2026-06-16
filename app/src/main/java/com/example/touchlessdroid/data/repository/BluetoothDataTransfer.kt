package com.example.touchlessdroid.data.repository

import com.example.touchlessdroid.domain.model.bluetooth.BlDataTransferStatus
import com.example.touchlessdroid.domain.model.camera.RobotCommand

interface BluetoothDataTransfer {
    suspend fun sendCommand(command: RobotCommand) : BlDataTransferStatus
}