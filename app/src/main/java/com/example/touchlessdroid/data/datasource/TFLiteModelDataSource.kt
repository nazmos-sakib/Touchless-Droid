package com.example.touchlessdroid.data.datasource


import android.content.Context
import android.util.Log
import com.example.touchlessdroid.domain.model.InferenceConfiguration
import com.example.touchlessdroid.utils.Constants
import com.example.touchlessdroid.utils.Constants.TFModelDebugTag
import com.example.touchlessdroid.utils.DelegateOption
import com.example.touchlessdroid.utils.PrecisionOption
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModelDataSource(private val context: Context) {

    private var tflite: Interpreter? = null
    private var  nnApiDelegate: NnApiDelegate? = null

    var delegate: String = ""
        private set

    /**
     * Load TensorFlow Lite model from assets
     * input shape: [1, 640, 640, 3]
     * output shape: [1, 300, 57]
     * data type: FLOAT32
     */
    fun loadModel(configuration: InferenceConfiguration): Interpreter {
        // Close existing interpreter if any
        close()

        val modelPath = when (configuration.precision) {
            PrecisionOption.FP32 -> {
                //Constants.MODEL_TFLITE_FP32 //input shape [1, 3, 640, 640] - [1, 300, 57]
                Constants.MODEL_TFLITE_OLD_FLOAT32 //actual FP32 - shape: [1, 640, 640, 3] - [1, 300, 57]
                //Constants.MODEL_TFLITE_OLD_FP32 //integer_quant
            }
            PrecisionOption.INT8 -> {
                //Constants.MODEL_TFLITE_INT8 //shape: [1, 3, 640, 640] - [1, 56, 8400]
                Constants.MODEL_TFLITE_OLD_INT8 //full_integer_quant
            }
        }

        tflite = when (configuration.delegate) {
            DelegateOption.CPU -> {
                createCpuOnlyInterpreter( model = loadModelFile(modelPath))
            }

            DelegateOption.GPU -> {
                createGpuOnlyInterpreter( model = loadModelFile(modelPath))
            }

            DelegateOption.NNAPI -> {
                createNNAPIOnlyInterpreter( model = loadModelFile(modelPath))
                //createNNAPIExplicitInterpreter( model = loadModelFile(modelPath))
            }

            DelegateOption.VULKAN-> {
                createInterpreter( model = loadModelFile(modelPath))
            }

        }

        //tflite = createInterpreter( model = loadModelFile(modelPath))


        logTensorInfo("Input",tflite?.getInputTensor(0)!!)
        logTensorInfo("Output",tflite?.getOutputTensor(0)!!)

        return tflite!!
    }

    fun createGpuOnlyInterpreter(model: ByteBuffer): Interpreter{
        val options = Interpreter.Options()

        try {
            val compatList = CompatibilityList()
            val gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
            options.addDelegate(gpuDelegate)
            delegate = "GPU"
            Log.d(TFModelDebugTag, "Using GPU")
            return Interpreter(model, options)

        } catch (e: Exception) {
            Log.e(TFModelDebugTag, "❌ GPU init failed: ${e.message}")
            return createCpuOnlyInterpreter(model)
        }
    }
    fun createNNAPIOnlyInterpreter(model: ByteBuffer): Interpreter{
        val numThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(4)

        val options = Interpreter.Options().apply {
            setUseNNAPI(true)
            setNumThreads(numThreads)
        }
        Log.d(TFModelDebugTag, "Using NNAPI")
        this.delegate = "NNAPI"
        return Interpreter(model, options)
    }
    fun createNNAPIExplicitInterpreter(model: ByteBuffer): Interpreter{
        val numThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(4)

        nnApiDelegate = NnApiDelegate(
            NnApiDelegate.Options().apply {
                allowFp16 = true
                useNnapiCpu = false
            }
        )  //NnApiDelegate must be released: nnApiDelegate.close()

        val options = Interpreter.Options().apply {
            addDelegate(nnApiDelegate)
            setNumThreads(numThreads)
        }
        Log.d(TFModelDebugTag, "Using NNAPI")
        delegate = "NNAPI"
        return Interpreter(model, options)
    }


    fun createCpuOnlyInterpreter(model: ByteBuffer): Interpreter{
        val numThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(4)

        // 3. CPU fallback (MOST IMPORTANT for threads)
        val options = Interpreter.Options().apply {
            setNumThreads(numThreads)
            //setNumThreads(4)
            setUseXNNPACK(true)
        }

        Log.d(TFModelDebugTag, "⚠️ Using CPU with $numThreads threads")
        delegate = "CPU"
        return Interpreter(model, options)
    }

    fun createInterpreter(model: ByteBuffer): Interpreter {

        val numThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
        // limit to ~4 (usually optimal on phones)

        // 1. Try GPU
        try {
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                val gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)

                val options = Interpreter.Options().apply {
                    addDelegate(gpuDelegate)
                    setNumThreads(numThreads) // harmless for GPU
                }
                Log.d(TFModelDebugTag, "Using GPU")
                this.delegate = "GPU"
                return Interpreter(model, options)
            }else{
                Log.d(TFModelDebugTag, "⚠️ GPU delegation is not supported")
            }
        } catch (e: Exception) {
            Log.e(TFModelDebugTag, "GPU failed: ${e.message}")
        }

        // 2. Try NNAPI
        try {
            val options = Interpreter.Options().apply {
                setUseNNAPI(true)
                setNumThreads(numThreads)
            }
            Log.d(TFModelDebugTag, "Using NNAPI")
            this.delegate = "NNAPI"
            return Interpreter(model, options)
        } catch (e: Exception) {
            Log.e(TFModelDebugTag, "NNAPI failed: ${e.message}")
        }

        // 3. CPU fallback (MOST IMPORTANT for threads)
        val options = Interpreter.Options().apply {
            setNumThreads(numThreads)
            setUseXNNPACK(true)
        }
        this.delegate = "CPU"
        Log.d(TFModelDebugTag, "Using CPU with $numThreads threads")
        return Interpreter(model, options)
    }

    /**
     * Load model file from assets
     */
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun logTensorInfo(name: String, tensor: Tensor) {
        Log.d(TFModelDebugTag, "=== $name INFO ===")
        Log.d(TFModelDebugTag, "logTensorInfo: shape: ${tensor.shape().contentToString()}")
        Log.d(TFModelDebugTag, "logTensorInfo: data type: ${tensor.dataType()}")
        Log.d(TFModelDebugTag, "logTensorInfo: data capacity: ${tensor.numBytes()}")
        Log.d(TFModelDebugTag, "logTensorInfo: Quantization: ${tensor.quantizationParams().toString()}")
        Log.d(TFModelDebugTag, "logTensorInfo: Quantization Scale: ${tensor.quantizationParams()?.scale}")
        Log.d(TFModelDebugTag, "logTensorInfo: Quantization zero Point: ${tensor.quantizationParams()?.zeroPoint}")
    }
    /**
     * Get input tensor shape
     */
    fun getInputShape(): IntArray? {
        return tflite?.getInputTensor(0)?.shape()
    }

    /**
     * Get output tensor shape
     */
    fun getOutputShape(): IntArray? {
        return tflite?.getOutputTensor(0)?.shape()
    }

    fun getDataType(): DataType? {
        return tflite?.getInputTensor(0)?.dataType()
    }
    fun getInputTensor(): Tensor {
        return tflite?.getInputTensor(0)!!
    }

    fun getOutputTensor(): Tensor {
        return tflite?.getOutputTensor(0)!!
    }

    /**
     * Run inference
     */
    fun runInference(inputBuffer: Any, outputBuffer: Any) {
        //tflite?.resizeInput(0, intArrayOf(1, 640, 640, 3)) // change the input tensor
        //tflite?.allocateTensors() //The model has dynamic input shapes
        tflite?.run(inputBuffer, outputBuffer)
    }

    /**
     * Close interpreter and release resources
     */
    fun close() {
        tflite?.close()
        tflite = null
        nnApiDelegate?.close()
    }
}
