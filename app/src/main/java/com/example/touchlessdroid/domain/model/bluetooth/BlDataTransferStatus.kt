package com.example.touchlessdroid.domain.model.bluetooth

sealed class BlDataTransferStatus {
    object Success : BlDataTransferStatus()
    object NotConnected : BlDataTransferStatus()
    data class Error(val message: String) : BlDataTransferStatus()
}