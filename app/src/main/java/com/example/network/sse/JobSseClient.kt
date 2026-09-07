package com.example.network.sse

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class JobSseClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite read timeout for SSE stream
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()
) {

    fun streamJobEvents(
        url: String,
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L
    ): Flow<JobEvent> = flow {
        var attempts = 0
        var shouldReconnect = true

        while (shouldReconnect && attempts < maxRetries) {
            var streamCompletedNormally = false
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "text/event-stream")
                    .addHeader("Cache-Control", "no-cache")
                    .build()

                val call = client.newCall(request)
                val response = call.execute()

                if (!response.isSuccessful) {
                    emit(JobEvent.Failed(response.code, "HTTP ${response.code}: ${response.message}"))
                    response.close()
                    break
                }

                val body = response.body
                if (body == null) {
                    emit(JobEvent.Failed(null, "Response body was null"))
                    break
                }

                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var currentEvent: String? = null
                val dataBuffer = StringBuilder()

                try {
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) {
                            // Dispatch event if we have data
                            if (dataBuffer.isNotEmpty()) {
                                val eventType = currentEvent ?: "message"
                                val event = JobEvent.parse(eventType, dataBuffer.toString().trim())
                                emit(event)

                                if (event is JobEvent.Completed || event is JobEvent.Failed || event is JobEvent.Cancelled) {
                                    shouldReconnect = false
                                    streamCompletedNormally = true
                                    break
                                }
                            }
                            currentEvent = null
                            dataBuffer.setLength(0)
                        } else if (line.startsWith("event:")) {
                            currentEvent = line.substring(6).trim()
                        } else if (line.startsWith("data:")) {
                            if (dataBuffer.isNotEmpty()) {
                                dataBuffer.append("\n")
                            }
                            dataBuffer.append(line.substring(5).trim())
                        }
                    }
                } finally {
                    reader.close()
                    response.close()
                }

                if (streamCompletedNormally) {
                    break
                }
            } catch (e: CancellationException) {
                // User or VM cancelled flow
                throw e
            } catch (e: Exception) {
                attempts++
                if (attempts >= maxRetries) {
                    emit(JobEvent.Failed(null, "SSE connection lost: ${e.localizedMessage ?: "Unknown network error"}"))
                    break
                }
                delay(initialDelayMs * attempts)
            }
        }
    }.flowOn(Dispatchers.IO)
}
