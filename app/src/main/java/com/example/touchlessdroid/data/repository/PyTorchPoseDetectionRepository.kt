package com.example.touchlessdroid.data.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import java.nio.ByteBuffer
import androidx.core.graphics.scale
import kotlin.math.min
import androidx.core.graphics.createBitmap
import com.example.touchlessdroid.data.datasource.PyTorchModelDataSource
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.domain.model.camera.DetectedPose
import com.example.touchlessdroid.domain.model.camera.LetterboxResult
import com.example.touchlessdroid.domain.model.camera.ReverseMapping
import com.example.touchlessdroid.utils.Constants
import com.example.touchlessdroid.utils.YOLOPostprocessor
import com.example.touchlessdroid.utils.extentions.bitmapToTensor
import org.pytorch.IValue
import java.nio.ByteOrder
import kotlin.collections.get

class PyTorchPoseDetectionRepository(
    private val modelDataSource: PyTorchModelDataSource
) {

    init {
        modelDataSource.loadModel(Constants.MODEL_PATH_26N_POSE_OPTIMIZED)
        //modelDataSource.loadModel(Constants.MODEL_PATH_26N_POSE)
    }

    /**
     * Initialize model
     */
    fun initialize() {
         //modelDataSource.loadModel(Constants.MODEL_PATH)
    }


    suspend fun detectPose(bitmap: Bitmap,revMapping: ReverseMapping,infConfig:InferenceConfiguration): List<DetectedPose> {

        return try {
            val letterBoxResult = letterbox(bitmap)

            val inputTensor = letterBoxResult.bitmap.bitmapToTensor()

            val output = modelDataSource.runInference(inputTensor)
            val shape = output.shape() // [1, 300, 57]
            val reshaped = reshapeTo2D(output.dataAsFloatArray, shape[1].toInt(), shape[2].toInt())

            return YOLOPostprocessor.parseOutputShape300x57(
                reshaped,
                letterBoxResult,
                revMapping
            )
        }  catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            bitmap.recycle()
        }
    }
    fun reshapeTo2D(array: FloatArray, numDetections: Int, features: Int): Array<FloatArray> {
        val result = Array(numDetections) { FloatArray(features) }

        for (i in 0 until numDetections) {
            for (j in 0 until features) {
                result[i][j] = array[i * features + j]
            }
        }

        return result
    }



    fun letterbox(bitmap: Bitmap, size: Int = 640): LetterboxResult {
        val width = bitmap.width
        val height = bitmap.height

        val scale = min(size / width.toFloat(), size / height.toFloat())

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val resized = bitmap.scale(newWidth, newHeight)

        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        val padX = (size - newWidth) / 2f
        val padY = (size - newHeight) / 2f

        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(resized, padX, padY, null)

        return LetterboxResult(output, scale, padX, padY)
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