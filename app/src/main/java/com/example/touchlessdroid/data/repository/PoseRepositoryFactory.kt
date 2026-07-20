package com.example.touchlessdroid.data.repository

import com.example.touchlessdroid.utils.RuntimeOption
import javax.inject.Inject
import javax.inject.Provider

class PoseRepositoryFactory @Inject constructor(
    private val tfliteProvider: Provider<TFLitePoseDetectionRepository>,
    private val onnxProvider: Provider<ONNXPoseDetectionRepository>,
    private val pyTorchProvider: Provider<PyTorchPoseDetectionRepository>,
    private val ncnnProvider: Provider<NCNNPoseRepository>
) {
    fun get(runtime: RuntimeOption): PoseDetectionRepository {
        return when (runtime) {
            RuntimeOption.TFLITE -> tfliteProvider.get()
            RuntimeOption.ONNX -> onnxProvider.get()
            RuntimeOption.PYTORCH -> pyTorchProvider.get()
            RuntimeOption.NCNN -> ncnnProvider.get()
        }
    }
}
