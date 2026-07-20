package com.example.touchlessdroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.touchlessdroid.domain.model.Screen
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.ui.screens.components.DrawerContent
import com.example.touchlessdroid.ui.screens.HomeScreen
import com.example.touchlessdroid.ui.screens.InfoScreen
import com.example.touchlessdroid.ui.screens.PairDeviceScreen
import com.example.touchlessdroid.ui.screens.SavedDevicesScreen
import com.example.touchlessdroid.ui.screens.SettingsScreen
import com.example.touchlessdroid.ui.screens.components.StatusBar
import com.example.touchlessdroid.ui.screens.components.camera.CameraScreen
import com.example.touchlessdroid.ui.theme.TouchlessDroidTheme
import com.example.touchlessdroid.ui.viewmodel.BluetoothViewModel
import com.example.touchlessdroid.ui.viewmodel.CameraViewModel
import com.example.touchlessdroid.utils.DelegateOption
import com.example.touchlessdroid.utils.PrecisionOption
import com.example.touchlessdroid.utils.RuntimeOption
import com.example.yolo26localposeanalyzer.ui.screens.PermissionDeniedScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //keep the screen always on

        enableEdgeToEdge()
        setContent {
            TouchlessDroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppPermissionHandler {
                        MainApp(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}


@Composable
fun AppPermissionHandler(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val permissions = mutableListOf(
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var allGranted by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allGranted = results.values.all { it }
        showSettings = !allGranted
    }

    LaunchedEffect(Unit) {
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (granted) {
            allGranted = true
        } else {
            launcher.launch(permissions.toTypedArray())
        }
    }

    when {
        allGranted -> content()

        showSettings -> {
            PermissionDeniedScreen(
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                onRetry = {
                    launcher.launch(permissions.toTypedArray())
                }
            )
        }

        else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Requesting permissions...")
            }
        }
    }
}

@androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT])
@Composable
fun MainApp(modifier: Modifier = Modifier){

     val context = LocalContext.current
    //val repository = ObjectDetectionRepository(LocalModelDataSource(context))

    // Bluetooth ViewModel
    val bluetoothViewModel: BluetoothViewModel = hiltViewModel()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var appDrawerSelectedRoute by remember {
        mutableStateOf(Screen.Home.route)
    }
    var selectedInferenceConfiguration by remember {
        mutableStateOf(
            InferenceConfiguration(
                runtime = RuntimeOption.TFLITE,
                delegate = DelegateOption.CPU,
                precision = PrecisionOption.FP32
            )
        )
    }
    //observe changes in the route
    LaunchedEffect(currentRoute) {
        currentRoute?.let {route->
            // Do something whenever the route changes
            //println("Route changed: $route")
            //if current route is to discover nearby devices the then enable bluetooth discovery mode.
            appDrawerSelectedRoute = route
            //if (route== ROUTE_PAIR_NEW_DEVICE) viewModel.startScan() else viewModel.stopScan()
        }
    }
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(appDrawerSelectedRoute) { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                StatusBar(
                    bluetoothViewModel = bluetoothViewModel,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    }
                )
            }
        ) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {

                composable(Screen.Home.route) {
                    HomeScreen(
                        bluetoothViewModel = bluetoothViewModel,
                        onStartClick = {configuration ->
                            selectedInferenceConfiguration = configuration
                            navController.navigate(Screen.Camera.route)
                        }
                    )
                }

                composable(Screen.Camera.route) {
                    val cameraViewModel: CameraViewModel = hiltViewModel()
                    LaunchedEffect(selectedInferenceConfiguration) {
                        cameraViewModel.startCameraSession(selectedInferenceConfiguration)
                    }
                    CameraScreen(viewModel = cameraViewModel)
                }

                composable(Screen.Pair.route) {
                    PairDeviceScreen(bluetoothViewModel)
                }

                composable(Screen.Saved.route) {
                    SavedDevicesScreen(bluetoothViewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }

                composable(Screen.Info.route) {
                    InfoScreen()
                }
            }
        }
    }
}
