package com.example.touchlessdroid.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.touchlessdroid.utils.ROUTE_CAMERA
import com.example.touchlessdroid.utils.ROUTE_HOME
import com.example.touchlessdroid.utils.ROUTE_INFO
import com.example.touchlessdroid.utils.ROUTE_PAIR_NEW_DEVICE
import com.example.touchlessdroid.utils.ROUTE_SAVED_DEVICES
import com.example.touchlessdroid.utils.ROUTE_SETTINGS


data class NavigationItem(
    val title: String,
    val route:String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int? = null
)


@Composable
fun getNavigationItems():List<NavigationItem> = listOf(
    NavigationItem(
        title = "Home",
        route = ROUTE_HOME,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),

    NavigationItem(
        title = "Camera",
        route = ROUTE_CAMERA,
        selectedIcon = Icons.Filled.Camera,
        unselectedIcon = Icons.Outlined.Camera
    ),

    NavigationItem(
        title = "Pair New Device",
        route = ROUTE_PAIR_NEW_DEVICE,
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Outlined.Add
    ),
    NavigationItem(
        title = "Saved Devices",
        route = ROUTE_SAVED_DEVICES,
        selectedIcon = Icons.Filled.PhoneAndroid,
        unselectedIcon = Icons.Outlined.PhoneAndroid,
    ),
    NavigationItem(
        title = "Settings",
        route = ROUTE_SETTINGS,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    ),

    NavigationItem(
        title = "Info",
        route = ROUTE_INFO,
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    )
)