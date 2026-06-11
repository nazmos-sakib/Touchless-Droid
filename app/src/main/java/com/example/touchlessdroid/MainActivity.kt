package com.example.touchlessdroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.touchlessdroid.data.datasource.LocalModelDataSource
import com.example.touchlessdroid.data.repository.ObjectDetectionRepository
import com.example.touchlessdroid.ui.screens.CameraScreen
import com.example.touchlessdroid.ui.theme.TouchlessDroidTheme
import com.example.touchlessdroid.ui.viewmodel.CameraViewModel
import com.example.yolo26localposeanalyzer.ui.screens.PermissionDeniedScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //keep the screen always on

        enableEdgeToEdge()
        setContent {
            TouchlessDroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraPermissionHandler {
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
fun CameraPermissionHandler(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var permissionGranted by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        showSettings = !isGranted
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            permissionGranted = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when {
        permissionGranted -> {
            content()
        }

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
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }

        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Requesting camera permission...")
            }
        }
    }
}


@Composable
fun MainApp(modifier: Modifier = Modifier){

    // Create repository and ViewModel
    val context = LocalContext.current
    val repository = ObjectDetectionRepository(LocalModelDataSource(context))
    val viewModel: CameraViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CameraViewModel(repository)
            }
        }
    )

    CameraScreen(viewModel = viewModel)

}