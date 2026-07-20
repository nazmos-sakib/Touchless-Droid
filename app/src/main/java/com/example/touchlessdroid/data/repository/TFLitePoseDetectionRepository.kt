package com.example.touchlessdroid.data.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import com.example.touchlessdroid.data.datasource.TFLiteModelDataSource
import com.example.touchlessdroid.utils.Constants
import java.nio.ByteBuffer
import androidx.core.graphics.scale
import com.example.touchlessdroid.utils.YOLOPostprocessor
import kotlin.math.min
import androidx.core.graphics.createBitmap
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.domain.model.camera.DetectedPose
import com.example.touchlessdroid.domain.model.camera.LetterboxResultV2
import com.example.touchlessdroid.domain.model.camera.ReverseMapping
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Tensor
import java.nio.ByteOrder
import kotlin.IntArray
import kotlin.math.roundToInt

class TFLitePoseDetectionRepository(
    private val modelDataSource: TFLiteModelDataSource
) : PoseDetectionRepository {
    private var initializedConfiguration: InferenceConfiguration? = null
    private var size: Int = Constants.MODEL_INPUT_SIZE
    private lateinit var inputBuffer: ByteBuffer
    private lateinit var pixels: IntArray
    private lateinit var letterBoxBitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var inputTensor: Tensor
    private lateinit var outputTensor: Tensor

    private lateinit var floatOutputBuffer: Array<Array<FloatArray>>
    private lateinit var int8OutputBuffer: Array<Array<ByteArray>>

    override fun initialize(configuration: InferenceConfiguration) {
        if (initializedConfiguration == configuration) return

        modelDataSource.loadModel(configuration)
        inputTensor = modelDataSource.getInputTensor()
        outputTensor = modelDataSource.getOutputTensor()

        inputBuffer = createBuffer(inputTensor)
        size = inputTensor.shape()[1]
        createOutputBuffers(outputTensor)
        pixels = IntArray(size * size)
        letterBoxBitmap = createBitmap(size, size)
        canvas = Canvas(letterBoxBitmap)
        initializedConfiguration = configuration
        Log.d("TFLITE_REPOSITORY", "model loaded successfully")
    }

    /**
     * Initialize model
     */
    override suspend fun detectPose(bitmap: Bitmap,revMapping: ReverseMapping,infConfig:InferenceConfiguration): List<DetectedPose> {
        initialize(infConfig)

        bitmap
        return try {
            // Preprocess image
            val v2LetterBoxResult = prepareInput(bitmap)

            // Run inference
            val outputForPostprocess = runInference()

            //return statement
            // Postprocess results
            YOLOPostprocessor.parseOutputShape300x57(
                outputForPostprocess,
                v2LetterBoxResult,
                revMapping
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            bitmap.recycle()
        }
    }


    fun prepareInput(bitmap: Bitmap): LetterboxResultV2 {
        //val resizedBitmap = bitmap.scale(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)


        val result = letterbox(bitmap, size)

        inputBuffer.rewind()

        letterBoxBitmap.getPixels(pixels, 0, size, 0, 0,
            size, size
        )

        for (pixel in pixels) {
            val r = ((pixel shr 16 and 0xFF) / 255.0f)
            val g = ((pixel shr 8 and 0xFF) / 255.0f)
            val b = ((pixel and 0xFF) / 255.0f)

            // YOLOv8 expects RGB format, normalized to [0,1]
            when (inputTensor.dataType()) {
                DataType.FLOAT32 -> {
                    inputBuffer.putFloat(r)
                    inputBuffer.putFloat(g)
                    inputBuffer.putFloat(b)
                }

                DataType.INT8 -> {
                    putQuantizedInt8(inputBuffer, r, inputTensor)
                    putQuantizedInt8(inputBuffer, g, inputTensor)
                    putQuantizedInt8(inputBuffer, b, inputTensor)
                }

                else -> error("Unsupported TFLite input type: ${inputTensor.dataType()}")
            }
        }

        //resizedBitmap.recycle()
        return result
    }

    private fun runInference(): Array<Array<FloatArray>> {
        return when (outputTensor.dataType()) {
            DataType.FLOAT32 -> {
                modelDataSource.runInference(inputBuffer, floatOutputBuffer)
                floatOutputBuffer
            }

            DataType.INT8 -> {
                modelDataSource.runInference(inputBuffer, int8OutputBuffer)
                dequantizeInt8Output(int8OutputBuffer, outputTensor)
            }

            else -> error("Unsupported TFLite output type: ${outputTensor.dataType()}")
        }
    }

    private fun createOutputBuffers(tensor: Tensor) {
        val shape = tensor.shape()
        floatOutputBuffer = Array(shape[0]) {
            Array(shape[1]) {
                FloatArray(shape[2])
            }
        }
        int8OutputBuffer = Array(shape[0]) {
            Array(shape[1]) {
                ByteArray(shape[2])
            }
        }
    }

    private fun putQuantizedInt8(buffer: ByteBuffer, value: Float, tensor: Tensor) {
        val params = tensor.quantizationParams()
        val quantized = (value / params.scale + params.zeroPoint)
            .roundToInt()
            .coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt())

        buffer.put(quantized.toByte())
    }

    private fun dequantizeInt8Output(
        output: Array<Array<ByteArray>>,
        tensor: Tensor
    ): Array<Array<FloatArray>> {
        val params = tensor.quantizationParams()

        return Array(output.size) { batch ->
            Array(output[batch].size) { box ->
                FloatArray(output[batch][box].size) { i ->
                    (output[batch][box][i].toInt() - params.zeroPoint) * params.scale
                }
            }
        }
    }

    private fun createBuffer(tensor: Tensor): ByteBuffer {
        val numBytes = tensor.numBytes()
        return ByteBuffer.allocateDirect(numBytes).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    //preserving aspect ratio and padding the remaining area.
    //That's the standard "letterbox" preprocessing
    fun letterbox(bitmap: Bitmap, size: Int = 640): LetterboxResultV2 {
        val width = bitmap.width
        val height = bitmap.height

        val scale = min(size / width.toFloat(), size / height.toFloat())

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val resized = bitmap.scale(newWidth, newHeight)


        val padX = (size - newWidth) / 2f
        val padY = (size - newHeight) / 2f

        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(resized, padX, padY, null)

        return LetterboxResultV2( size,scale, padX, padY)
    }

    fun getModelDelegate(): String{
        return modelDataSource.delegate
    }

    /**
     * Clean up resources
     */
    fun close() {
        modelDataSource.close()
        initializedConfiguration = null
    }

    override fun release() {
        close()
    }
}
