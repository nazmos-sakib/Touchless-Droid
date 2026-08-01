package com.example.touchlessdroid.data.repository

import com.example.touchlessdroid.benchmark.FrameTiming
import com.example.touchlessdroid.domain.model.bluetooth.BlDataTransferStatus
import com.example.touchlessdroid.domain.model.camera.RobotCommand

interface BluetoothDataTransfer {
    suspend fun sendCommand(command: RobotCommand, timing: FrameTiming? = null) : BlDataTransferStatus
}
