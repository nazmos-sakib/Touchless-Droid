package com.example.touchlessdroid.data.datasource


import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.providers.NNAPIFlags
import android.content.Context
import android.graphics.Bitmap
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.EnumSet

class ONNXModelDataSource(private val context: Context) {

    private val env = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    /**
     * Load ONNX  model from assets
     * input shape: [1, 640, 640, 3]
     * output shape: [1, 300, 57]
     * data type: FLOAT32
     */

    fun loadModel(modelPath: String) {
        val modelBytes = context.assets.open(modelPath).readBytes()

        val opts = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }

        //NNAPI
        val options = OrtSession.SessionOptions().apply {
            addNnapi(
                EnumSet.of(
                    NNAPIFlags.USE_FP16,
                    NNAPIFlags.CPU_DISABLED
                )
            )
        }
        session = env.createSession(modelBytes, opts)
    }



    fun run(inputTensor: OnnxTensor): Array<FloatArray> {
        val inputs = mapOf(
            "images" to inputTensor   // ⚠️ confirm input name using Netron
        )

        val outputs = session?.run(inputs)!!

        val outputTensor  = outputs[0].value as Array<Array<FloatArray>>
        return outputTensor[0]
    }

    fun getEnv(): OrtEnvironment {
        return env!!
    }

    fun close() {
        session?.close()
        session = null
    }

}