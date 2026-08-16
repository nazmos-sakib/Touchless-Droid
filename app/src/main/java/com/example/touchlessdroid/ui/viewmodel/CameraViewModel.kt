package com.example.touchlessdroid.ui.viewmodel


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touchlessdroid.data.repository.PoseDetectionRepository
import com.example.touchlessdroid.data.repository.PoseRepositoryFactory
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.domain.model.bluetooth.BlDataTransferStatus
import com.example.touchlessdroid.domain.model.camera.DetectedPose
import com.example.touchlessdroid.domain.model.camera.ReverseMapping
import com.example.touchlessdroid.domain.model.camera.RobotCommand
import com.example.touchlessdroid.domain.model.camera.toPose
import com.example.touchlessdroid.domain.usecase.GestureDetector
import com.example.touchlessdroid.domain.usecase.GestureToCommandUseCase
import com.example.touchlessdroid.domain.usecase.RobotServer
import com.example.touchlessdroid.utils.Constants.ImageDebugTag
import com.example.touchlessdroid.utils.Constants.PerformanceDebugTag
import com.example.touchlessdroid.utils.Constants.UiDebugTag
import com.example.touchlessdroid.utils.DelegateOption
import com.example.touchlessdroid.utils.FpsCounter
import com.example.touchlessdroid.utils.PrecisionOption
import com.example.touchlessdroid.utils.RuntimeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val poseRepositoryFactory: PoseRepositoryFactory,
    private val gestureUseCase: GestureToCommandUseCase,
    private val gestureDetector: GestureDetector,
    private val robotServer: RobotServer
) : ViewModel() {

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

    init {
        observeGestures()
    }

    fun startCameraSession(configuration: InferenceConfiguration) {
        if (repository != null) return

        _configuration.value = configuration
        viewModelScope.launch(Dispatchers.IO) {
            repository = poseRepositoryFactory.get(configuration.runtime).also {
                it.initialize(configuration)
            }
        }
    }

    private fun observeGestures() {
        viewModelScope.launch {
            _command
                //.distinctUntilChanged()
                .collect { gesture ->
                    // Store the latest command in the TCP server
                    robotServer.setStatus(gesture.name)
                    val result = gestureUseCase.process(gesture)
                    when (result) {
                        is BlDataTransferStatus.Success -> { /* OK */ }

                        is BlDataTransferStatus.NotConnected -> {
                            /*_uiState.update {
                                it.copy(error = "Not connected to device")
                            }*/
                        }

                        is BlDataTransferStatus.Error -> {
                            /*_uiState.update {
                                it.copy(error = result.message)
                            }*/
                        }
                    }
                }
        }
    }

    fun  updateImageProxyFPS(){
        imageProxyFpsCounter.tick("Analyzer FPS")
        _imageProxyFps.value = imageProxyFpsCounter.fps.toFloat()
    }

    fun processFrame(bitmap: Bitmap,revMapping: ReverseMapping) {
        if (_isProcessing.value) return


        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            _isProcessing.value = true

            try {
                //val results = repository.detectObjects(bitmap)
                val currentConfiguration = configuration.value
                val activeRepository = repository
                if (activeRepository == null) {
                    bitmap.recycle()
                    return@launch
                }
                val results = activeRepository.detectPose(bitmap,revMapping,currentConfiguration)


                // take first person only
                val pose = results.firstOrNull()?.keyPoints?.toPose()
                //Log.d(UiDebugTag, "processFrame: $pose")
                val cmd = if (pose != null) gestureDetector.detect(pose) else RobotCommand.NO_PERSON

                Log.d(UiDebugTag, "viewmodel: processFrame: objects found: ${results.size}")
                Log.d(UiDebugTag, "viewmodel: processFrame: $results")
                withContext(Dispatchers.Main) {
                    _detectedObjects.value = results
                    _command.value = cmd
                    updateInferenceFPS()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }

            Log.d(PerformanceDebugTag, "viewModel: processFrame: time to inference: ${System.currentTimeMillis()-now}")
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
        super.onCleared()
        repository?.release()
        repository = null
    }

    fun updateDisplayFps(frameCount: Int) {
        _previewViewFps.value = frameCount.toFloat()
    }
}
