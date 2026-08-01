package com.example.touchlessdroid.ui.viewmodel


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touchlessdroid.benchmark.BenchmarkController
import com.example.touchlessdroid.benchmark.BenchmarkState
import com.example.touchlessdroid.benchmark.FrameTiming
import com.example.touchlessdroid.data.repository.PoseDetectionRepository
import com.example.touchlessdroid.data.repository.PoseRepositoryFactory
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.domain.model.bluetooth.BlDataTransferStatus
import com.example.touchlessdroid.domain.model.bluetooth.BluetoothConnectionStatus
import com.example.touchlessdroid.domain.model.camera.DetectedPose
import com.example.touchlessdroid.domain.model.camera.ReverseMapping
import com.example.touchlessdroid.domain.model.camera.RobotCommand
import com.example.touchlessdroid.domain.model.camera.toPose
import com.example.touchlessdroid.domain.usecase.GestureDetector
import com.example.touchlessdroid.domain.usecase.BluetoothManager
import com.example.touchlessdroid.utils.Constants.ImageDebugTag
import com.example.touchlessdroid.utils.Constants.PerformanceDebugTag
import com.example.touchlessdroid.utils.Constants.UiDebugTag
import com.example.touchlessdroid.utils.DelegateOption
import com.example.touchlessdroid.utils.FpsCounter
import com.example.touchlessdroid.utils.PrecisionOption
import com.example.touchlessdroid.utils.RuntimeOption
import com.example.touchlessdroid.utils.Utility
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val poseRepositoryFactory: PoseRepositoryFactory,
    private val gestureDetector: GestureDetector,
    private val bluetoothManager: BluetoothManager
) : ViewModel() {
    private val frameOwned = AtomicBoolean(false)
    private val benchmarkController = BenchmarkController(context)
    val benchmarkUiState = benchmarkController.uiState
    val bluetoothConnectionStatus = bluetoothManager.connectionState

    private val inferenceFpsCounter = FpsCounter()
    private val imageProxyFpsCounter = FpsCounter()

    private val _command = MutableStateFlow(RobotCommand.NONE)
    val command: StateFlow<RobotCommand> = _command

    private val _detectedObjects = MutableStateFlow<List<DetectedPose>>(emptyList())
    val detectedObjects: StateFlow<List<DetectedPose>> = _detectedObjects.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _imageProxyFps = MutableStateFlow(0f)
    val imageProxyFps: StateFlow<Float> = _imageProxyFps.asStateFlow()
    private val _previewViewFps = MutableStateFlow(0f)
    val previewViewFps: StateFlow<Float> = _previewViewFps.asStateFlow()

    private val _inferenceFps = MutableStateFlow(0f)
    val inferenceFps: StateFlow<Float> = _inferenceFps.asStateFlow()

    private val _configuration = MutableStateFlow(
        InferenceConfiguration(
            runtime = RuntimeOption.TFLITE,
            delegate = DelegateOption.CPU,
            precision = PrecisionOption.FP32
        )
    )

    val configuration: StateFlow<InferenceConfiguration> =
        _configuration.asStateFlow()

    private var repository: PoseDetectionRepository? = null
    private val _repositoryReady = MutableStateFlow(false)
    val repositoryReady: StateFlow<Boolean> = _repositoryReady.asStateFlow()

    init {
        viewModelScope.launch {
            benchmarkUiState.collect { state ->
                if (state.state == BenchmarkState.FINISHING) {
                    benchmarkController.finishIfReady(frameOwned.get())
                }
            }
        }
    }

    fun startCameraSession(configuration: InferenceConfiguration) {
        if (repository != null) return

        _configuration.value = configuration
        viewModelScope.launch(Dispatchers.IO) {
            repository = poseRepositoryFactory.get(configuration.runtime).also {
                it.initialize(configuration)
            }
            _repositoryReady.value = true
        }
    }

    fun startBenchmark(): Boolean {
        if (!_repositoryReady.value) {
            benchmarkController.reportStartError("The pose model is still initializing. Please try again shortly.")
            return false
        }
        if (bluetoothConnectionStatus.value != BluetoothConnectionStatus.CONNECTED) {
            benchmarkController.reportStartError("Connect a Bluetooth device before starting the benchmark.")
            return false
        }
        return benchmarkController.start(viewModelScope, configuration.value)
    }

    fun tryAcquireFrame(): FrameTiming? {
        if (!frameOwned.compareAndSet(false, true)) return null
        _isProcessing.value = true
        return FrameTiming.accepted(
            snapshot = benchmarkController.snapshotAcceptedFrame(),
            configuration = configuration.value,
            device = Utility.getDeviceName()
        )
    }

    fun markBitmapReady(timing: FrameTiming) {
        timing.bitmapReadyNs = SystemClock.elapsedRealtimeNanos()
    }

    fun releaseFailedFrame(bitmap: Bitmap? = null) {
        bitmap?.takeUnless { it.isRecycled }?.recycle()
        frameOwned.set(false)
        _isProcessing.value = false
    }

    fun  updateImageProxyFPS(){
        imageProxyFpsCounter.tick("Analyzer FPS")
        _imageProxyFps.value = imageProxyFpsCounter.fps.toFloat()
    }

    fun processFrame(bitmap: Bitmap, revMapping: ReverseMapping, timing: FrameTiming) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeRepository = repository
                if (activeRepository == null) {
                    bitmap.recycle()
                    return@launch
                }
                val results = activeRepository.detectPose(
                    bitmap,
                    revMapping,
                    configuration.value,
                    timing
                )


                // take first person only
                val pose = results.firstOrNull()?.keyPoints?.toPose()
                val cmd = if (pose != null) gestureDetector.detect(pose) else RobotCommand.NO_PERSON
                timing.gestureCompletedNs = SystemClock.elapsedRealtimeNanos()
                val bluetoothResult = bluetoothManager.sendCommand(cmd, timing)
                if (timing.shouldRecord && bluetoothResult is BlDataTransferStatus.Success) {
                    benchmarkController.submit(timing.completedMetrics())
                }

                withContext(Dispatchers.Main) {
                    _detectedObjects.value = results
                    _command.value = cmd
                    updateInferenceFPS()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
                frameOwned.set(false)
                _isProcessing.value = false
                benchmarkController.finishIfReady(false)
            }
        }
    }




    var timeForSaveImage = System.currentTimeMillis()
    // /storage/emulated/0/Android/data/com.example.cameraobjectanalyzer/files/debug_xxx.jpg
    fun saveImageProxy(context: Context, jpegBytes:  ByteArray, rotationDegree: Float) {


        if (System.currentTimeMillis() - timeForSaveImage < 1000) return

        try {
            val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

            //metadata about how to transform geometry. stores a mathematical transformation description
            val matrix = Matrix()
            //“When you draw this image, rotate every pixel by X degrees around a pivot.”
            matrix.postRotate(rotationDegree)

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height,
                matrix, true
            )

            saveImageProxy(context,rotatedBitmap)

        } finally {

        }
    }



     fun saveImageProxy(context: Context, bitmap: Bitmap ) {

         if (System.currentTimeMillis() - timeForSaveImage < 1000) return

        try {
            val file = File(
                context.getExternalFilesDir(null),
                "debug_${System.currentTimeMillis()}.jpg"
            )

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            Log.d("DEBUG_IMAGE", "Saved: ${file.absolutePath}")

        } finally {

        }
    }




    fun viewModelFPSRateLimit(){
        if (_isProcessing.value) return

        _isProcessing.value = true
        Log.d(PerformanceDebugTag, "viewModelFPSRateLimit")
        updateInferenceFPS()

        viewModelScope.launch {
            delay(1000) // performance delay
            _isProcessing.value = false
        }
    }

    fun debugInfo(imgProxy:ImageProxy,previewView: PreviewView){
        if (_isProcessing.value) return


        Log.d(ImageDebugTag, "viewModel:debugInfo: imageProxy-> rotation:${imgProxy.imageInfo.rotationDegrees}")
        Log.d(ImageDebugTag, "viewModel:debugInfo: imageProxy-> width:${imgProxy.width} height: ${imgProxy.height}")
        Log.d(ImageDebugTag, "viewModel:debugInfo: imageProxy-> format:${imgProxy.format}")

        Log.d(ImageDebugTag, "viewModel:debugInfo: previewView-> width:${previewView.width} height: ${previewView.height}")
        Log.d(ImageDebugTag, "viewModel:debugInfo: previewView-> rotation:${previewView.display.rotation}")
        //Log.d(ImageDebugTag, "Canvas: Canvas: width:${size.width} height: ${size.height}")

    }


    fun updateInferenceFPS() {
         inferenceFpsCounter.tick("Inference FPS")
        _inferenceFps.value = inferenceFpsCounter.fps.toFloat()
    }

    override fun onCleared() {
        runBlocking { benchmarkController.cancel() }
        repository?.release()
        repository = null
        super.onCleared()
    }

    fun updateDisplayFps(frameCount: Int) {
        _previewViewFps.value = frameCount.toFloat()
    }
}
