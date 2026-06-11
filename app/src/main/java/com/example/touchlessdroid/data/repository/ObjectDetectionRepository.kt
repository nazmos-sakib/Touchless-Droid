package com.example.touchlessdroid.data.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.touchlessdroid.data.datasource.LocalModelDataSource
import com.example.touchlessdroid.utils.Constants
import java.nio.ByteBuffer
import androidx.core.graphics.scale
import com.example.touchlessdroid.utils.YOLOPostprocessor
import kotlin.math.min
import androidx.core.graphics.createBitmap
import com.example.touchlessdroid.domain.model.DetectedPose
import com.example.touchlessdroid.domain.model.LetterboxResultV2
import com.example.touchlessdroid.domain.model.ReverseMapping
import org.tensorflow.lite.Tensor
import java.nio.ByteOrder
import kotlin.IntArray

class ObjectDetectionRepository(
    private val modelDataSource: LocalModelDataSource
) {
    private val size: Int
    val inputBuffer:ByteBuffer
    private val pixels:IntArray
    val letterBoxBitmap: Bitmap
    val canvas: Canvas

    val outputBuffer = Array(1) {
        Array(300) {
            FloatArray(57)
        }
    }

    init {
        //modelDataSource.loadModel(Constants.MODEL_PATH_26N_POSE_16)
        //modelDataSource.loadModel(Constants.MODEL_PATH_26N_POSE_32)
        //modelDataSource.loadModel(Constants.MODEL_PATH_26N_POSE_INT8)
        modelDataSource.loadModel(Constants.MODEL_PATH_26N_POSE_INTEGER_QUANT)
        //modelDataSource.loadModel(Constants.MODEL_PATH_26N_POSE_FULL_INTEGER_QUANT)
        val inputTensor = modelDataSource.getInputTensor()

        inputBuffer = createBuffer(inputTensor)
        size = inputTensor.shape()[1]
        pixels = IntArray(size * size)
        letterBoxBitmap = createBitmap(size, size)
        canvas = Canvas(letterBoxBitmap)
    }

    /**
     * Initialize model
     */
    fun initialize() {
         //modelDataSource.loadModel(Constants.MODEL_PATH)
    }

    /**
     * Detect objects in bitmap
     */

    suspend fun detectPose(bitmap: Bitmap,revMapping: ReverseMapping): List<DetectedPose> {

        bitmap
        return try {
            // Preprocess image
            val v2LetterBoxResult = prepareInput(bitmap)

            // Run inference
            modelDataSource.runInference(inputBuffer, outputBuffer)

            //return statement
            // Postprocess results
            YOLOPostprocessor.parseOutputShape300x57(
                outputBuffer,
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
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        //resizedBitmap.recycle()
        return result
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
    }
}