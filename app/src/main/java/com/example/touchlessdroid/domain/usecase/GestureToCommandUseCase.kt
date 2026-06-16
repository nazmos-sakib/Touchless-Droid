package com.example.touchlessdroid.domain.usecase

import com.example.touchlessdroid.data.repository.BluetoothDataTransfer
import com.example.touchlessdroid.domain.model.bluetooth.BlDataTransferStatus
import com.example.touchlessdroid.domain.model.camera.RobotCommand

class GestureToCommandUseCase(
    private val sender: BluetoothDataTransfer
){

    private var lastCommand: RobotCommand? = null

    suspend fun process(gesture: RobotCommand): BlDataTransferStatus{
        if (gesture == RobotCommand.NONE) return BlDataTransferStatus.Success
        if (gesture == lastCommand) return BlDataTransferStatus.Success

        lastCommand = gesture

        return sender.sendCommand(gesture)
    }
}