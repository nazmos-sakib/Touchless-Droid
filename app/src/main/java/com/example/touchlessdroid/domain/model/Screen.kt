package com.example.touchlessdroid.domain.model

import com.example.touchlessdroid.utils.ROUTE_HOME
import com.example.touchlessdroid.utils.ROUTE_CAMERA
import com.example.touchlessdroid.utils.ROUTE_PAIR_NEW_DEVICE
import com.example.touchlessdroid.utils.ROUTE_SETTINGS
import com.example.touchlessdroid.utils.ROUTE_SAVED_DEVICES
import com.example.touchlessdroid.utils.ROUTE_INFO

sealed class Screen(val route: String) {
    object Home : Screen(ROUTE_HOME)
    object Camera : Screen(ROUTE_CAMERA)
    object Pair : Screen(ROUTE_PAIR_NEW_DEVICE)
    object Saved : Screen(ROUTE_SAVED_DEVICES)
    object Settings : Screen(ROUTE_SETTINGS)
    object Info : Screen(ROUTE_INFO)
}