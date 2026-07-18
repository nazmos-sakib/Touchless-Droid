package com.example.touchlessdroid.domain.model

sealed class Delegate(val value: String) {
    object CPU : Delegate("CPU")
    object GPU : Delegate("GPU")
    object NNAPI : Delegate("NNAPI")
}