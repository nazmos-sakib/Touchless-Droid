package com.example.touchlessdroid.benchmark

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Locale

class BenchmarkCsvWriter(private val context: Context) {
    private var channel: Channel<CompletedFrameMetrics>? = null
    private var writerJob: Job? = null
    private var outputFile: File? = null

    fun open(scope: CoroutineScope, filename: String): File {
        check(channel == null) { "CSV writer is already open" }
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        val directory = File(root, "benchmarks").apply {
            check(exists() || mkdirs()) { "Unable to create benchmark directory: $absolutePath" }
        }
        val file = File(directory, filename)
        val rows = Channel<CompletedFrameMetrics>(Channel.UNLIMITED)
        channel = rows
        outputFile = file
        writerJob = scope.launch(Dispatchers.IO) {
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)).use { writer ->
                writer.write(HEADER)
                writer.newLine()
                var pending = 0
                for (row in rows) {
                    writer.write(row.toCsv())
                    writer.newLine()
                    pending++
                    if (pending >= FLUSH_EVERY_ROWS) {
                        writer.flush()
                        pending = 0
                    }
                }
                writer.flush()
            }
        }
        return file
    }

    fun submit(metrics: CompletedFrameMetrics) {
        checkNotNull(channel).trySend(metrics).getOrThrow()
    }

    suspend fun close(): File {
        val rows = checkNotNull(channel)
        rows.close()
        writerJob?.join()
        channel = null
        writerJob = null
        return checkNotNull(outputFile)
    }

    suspend fun cancel() {
        channel?.close()
        writerJob?.join()
        channel = null
        writerJob = null
    }

    private fun CompletedFrameMetrics.toCsv(): String = listOf(
        runId, frameNumber.toString(), device, runtime, backend, precision,
        elapsedSinceRunStartNs.ms(), imageConversionNs.ms(), modelPreprocessingNs.ms(),
        totalPreprocessingNs.ms(), inferenceNs.ms(), postprocessingNs.ms(), gestureNs.ms(),
        bluetoothQueueNs.ms(), bluetoothWriteNs.ms(), gestureToBluetoothNs.ms(), endToEndNs.ms()
    ).joinToString(",") { csvEscape(it) }

    private fun Long.ms(): String = String.format(Locale.US, "%.6f", this / 1_000_000.0)

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value

    companion object {
        private const val FLUSH_EVERY_ROWS = 30
        private const val HEADER =
            "run_id,frame_number,device,runtime,backend,precision,elapsed_since_run_start_ms," +
                "image_conversion_ms,model_preprocessing_ms,total_preprocessing_ms,inference_ms," +
                "postprocessing_ms,gesture_ms,bluetooth_queue_ms,bluetooth_write_ms," +
                "gesture_to_bluetooth_ms,end_to_end_ms"
    }
}
