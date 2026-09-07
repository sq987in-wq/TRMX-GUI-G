package com.example.network.sse

import com.example.model.OutputFile
import org.json.JSONObject

sealed class JobEvent {
    data class Status(val status: String) : JobEvent()
    data class Progress(val percent: Float, val speed: String? = null, val eta: String? = null) : JobEvent()
    data class Stdout(val line: String) : JobEvent()
    data class Stderr(val line: String) : JobEvent()
    data class FileGenerated(val file: OutputFile) : JobEvent()
    data class Completed(val exitCode: Int, val message: String? = null) : JobEvent()
    data class Failed(val exitCode: Int?, val error: String) : JobEvent()
    data class Cancelled(val reason: String = "Cancelled by user") : JobEvent()
    data class Unknown(val eventType: String, val rawData: String) : JobEvent()

    companion object {
        fun parse(eventType: String, data: String): JobEvent {
            return try {
                when (eventType.trim().lowercase()) {
                    "status" -> {
                        val obj = JSONObject(data)
                        Status(obj.optString("status", "RUNNING"))
                    }
                    "progress" -> {
                        val obj = JSONObject(data)
                        Progress(
                            percent = obj.optDouble("percent", 0.0).toFloat(),
                            speed = obj.optString("speed").takeIf { it.isNotEmpty() },
                            eta = obj.optString("eta").takeIf { it.isNotEmpty() }
                        )
                    }
                    "stdout" -> {
                        val obj = JSONObject(data)
                        Stdout(obj.optString("line", ""))
                    }
                    "stderr" -> {
                        val obj = JSONObject(data)
                        Stderr(obj.optString("line", ""))
                    }
                    "file" -> {
                        val obj = JSONObject(data)
                        FileGenerated(
                            OutputFile(
                                fileId = obj.optString("fileId", obj.optString("file_id", "")),
                                filename = obj.optString("filename", "output"),
                                sizeBytes = obj.optLong("sizeBytes", obj.optLong("size_bytes", 0L)),
                                mimeType = obj.optString("mimeType", obj.optString("mime_type", "application/octet-stream")),
                                downloadUrl = obj.optString("downloadUrl", obj.optString("download_url", ""))
                            )
                        )
                    }
                    "completed" -> {
                        val obj = JSONObject(data)
                        Completed(
                            exitCode = obj.optInt("exitCode", obj.optInt("exit_code", 0)),
                            message = obj.optString("message").takeIf { it.isNotEmpty() }
                        )
                    }
                    "failed" -> {
                        val obj = JSONObject(data)
                        Failed(
                            exitCode = if (obj.has("exitCode")) obj.getInt("exitCode") else null,
                            error = obj.optString("error", "Process execution failed")
                        )
                    }
                    "cancelled" -> {
                        val obj = JSONObject(data)
                        Cancelled(reason = obj.optString("reason", "Cancelled"))
                    }
                    else -> Unknown(eventType, data)
                }
            } catch (e: Exception) {
                // If not valid JSON, treat as raw text fallback
                when (eventType.trim().lowercase()) {
                    "stdout" -> Stdout(data)
                    "stderr" -> Stderr(data)
                    else -> Unknown(eventType, data)
                }
            }
        }
    }
}
