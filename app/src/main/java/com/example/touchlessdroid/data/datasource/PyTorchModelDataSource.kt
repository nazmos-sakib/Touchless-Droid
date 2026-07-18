package com.example.touchlessdroid.data.datasource

import android.content.Context
import android.util.Log
import com.example.touchlessdroid.utils.Constants.TFModelDebugTag
import org.pytorch.Device
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PyTorchModelDataSource(private val context: Context) {

    private var module: Module? = null

    var delegate: String = "GPU"
        private set

    /**
     * Load PyTorch model from assets
     * input shape: [1, 640, 640, 3]
     * output shape: [1, 300, 57]
     * data type: FLOAT32
     */
    fun loadModel(assetName: String): Module {
        // Close existing interpreter if any
        close()

        //module = Module.load(loadModelFile(assetName)) //heavy
        module = LiteModuleLoader.load(loadModelFile(assetName)) //lite
        // Move to Vulkan (GPU)
        module to Device.VULKAN

        //logTensorInfo("Input",module?.getInputTensor(0)!!)
        //logTensorInfo("Output",module? .getInputTensor(0)!!)

        return module!!
    }


    /**
     * Load model file from assets
     */
    private fun loadModelFile(assetName: String) : String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath

        context.assets.open(assetName).use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    fun runInference(inputTensor: Tensor): Tensor {
        return module?.forward(IValue.from(inputTensor))?.toTensor()!!
    }

    /*
    private fun logTensorInfo(name: String, tensor: Tensor) {
        Log.d(TFModelDebugTag, "=== $name INFO ===")
        Log.d(TFModelDebugTag, "logTensorInfo: shape: ${tensor.shape().contentToString()}")
        Log.d(TFModelDebugTag, "logTensorInfo: data type: ${tensor.dataType()}")
        Log.d(TFModelDebugTag, "logTensorInfo: data capacity: ${tensor.numBytes()}")
        Log.d(TFModelDebugTag, "logTensorInfo: Quantization: ${tensor.quantizationParams().toString()}")
        Log.d(TFModelDebugTag, "logTensorInfo: Quantization Scale: ${tensor.quantizationParams()?.scale}")
        Log.d(TFModelDebugTag, "logTensorInfo: Quantization zero Point: ${tensor.quantizationParams()?.zeroPoint}")
    }*/

    fun close() {
        module = null
    }
}