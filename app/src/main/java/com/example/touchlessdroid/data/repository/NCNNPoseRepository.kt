package com.example.touchlessdroid.data.repository

import android.content.Context
import android.content.res.AssetManager
 import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
 import androidx.core.graphics.createBitmap
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.scale
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.domain.model.camera.DetectedPose
import com.example.touchlessdroid.domain.model.camera.Keypoint
import com.example.touchlessdroid.domain.model.camera.LetterboxResult
import com.example.touchlessdroid.domain.model.camera.ReverseMapping
import com.example.touchlessdroid.domain.model.camera.mapFromModel
import com.example.touchlessdroid.domain.model.camera.mapToPreview
import com.example.touchlessdroid.utils.Constants.FLOATS_PER_POSE
import com.example.touchlessdroid.utils.Constants.NUM_KEYPOINTS
import com.example.touchlessdroid.utils.DelegateOption
import com.example.touchlessdroid.utils.PrecisionOption
import com.example.touchlessdroid.utils.extentions.mapFromModel
import com.example.touchlessdroid.utils.extentions.mapToPreview
import kotlin.math.min

class NCNNPoseRepository (
    private val context: Context
) : PoseDetectionRepository {
    companion object {
        init {
            System.loadLibrary("ncnn_yolo26_pose_analyzer")
        }
    }

    //----------------------------------------
    // JNI FUNCTIONS
    //----------------------------------------

    private external fun initModelNative(assetManager: AssetManager,useIntQuant: Boolean,useVulkan: Boolean): Boolean
    private external fun detectNative(bitmap: Bitmap): FloatArray
    private external fun releaseNative()

    private var initialized = false

    override fun initialize(configuration: InferenceConfiguration) {
        if (initialized) return
        val success = initModelNative(assetManager = context.assets, configuration.precision == PrecisionOption.INT8, configuration.delegate == DelegateOption.VULKAN)
        initialized = success
        Log.d("NCNN_REPOSITORY", "model loaded = $success")
        check(success) { "Failed to initialize the NCNN model" }
    }

    //----------------------------------------
    // PUBLIC FUNCTIONS
    //----------------------------------------
    override suspend fun detectPose(bitmap: Bitmap, revMapping: ReverseMapping,infConfig:InferenceConfiguration): List<DetectedPose> {
        initialize(infConfig)
        check(initialized) { "NCNN inference requested before model initialization" }
        val letterboxResult = letterbox(bitmap)

        val raw = detectNative(letterboxResult.bitmap)

        return convertToPoses(raw,letterboxResult,revMapping)
    }


    override fun release() {
        releaseNative()
        initialized = false
    }

    fun letterbox(bitmap: Bitmap, size: Int = 640): LetterboxResult {
        val width = bitmap.width
        val height = bitmap.height

        val scale = min(size / width.toFloat(), size / height.toFloat())

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val resized = bitmap.scale(newWidth, newHeight)


        val padX = (size - newWidth) / 2f
        val padY = (size - newHeight) / 2f

        val letterBoxBitmap = createBitmap(size, size)
        val canvas = Canvas(letterBoxBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
        canvas.drawColor(
            //Color.BLACK
            Color.rgb(114, 114, 114)
        )
        canvas.drawBitmap(resized, padX, padY, paint)

        return LetterboxResult( letterBoxBitmap,scale, padX, padY)
    }

    fun resizeBitmapHighQuality(source: Bitmap): Bitmap {
        val targetWidth = 640
        val targetHeight = 640

        val resized = createBitmap(targetWidth, targetHeight)

        val canvas = Canvas(resized)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val srcRect = Rect(0, 0, source.width, source.height)
        val dstRect = Rect(0, 0, targetWidth, targetHeight)

        canvas.drawBitmap(source, srcRect, dstRect, paint)

        return resized
    }

    private fun convertToPoses(
        data: FloatArray,
        letterBox: LetterboxResult,
        revMapping: ReverseMapping
    ): List<DetectedPose> {

        val poses = mutableListOf<DetectedPose>()

        var index = 0

        while (index + FLOATS_PER_POSE <= data.size) {

            val x1 = data[index++]
            val y1 = data[index++]
            val x2 = data[index++]
            val y2 = data[index++]
            Log.d(
                "POSE_DEBUG",
                "bbox = $x1 $y1 $x2 $y2"
            )
            val score = data[index++]

            val keypoints = mutableListOf<Keypoint>()

            repeat(NUM_KEYPOINTS) {

                val x = data[index++]
                val y = data[index++]
                val confidence = data[index++]

                keypoints.add(
                    Keypoint(
                        kx = x,
                        ky = y,
                        kc = confidence
                    ).mapFromModel(letterBox).mapToPreview(revMapping)
                )
            }

            poses.add(
                DetectedPose(
                    boundingBox = RectF(x1, y1, x2, y2)
                        .mapFromModel(letterBox).mapToPreview(revMapping),
                    label = "person",
                    confidence = score,
                    classId = 0,
                    keyPoints = keypoints
                )
            )
        }

        return poses
    }

}
