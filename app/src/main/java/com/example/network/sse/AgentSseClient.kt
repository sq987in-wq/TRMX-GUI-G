package com.example.network.sse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class AgentSseClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()
) {

    fun streamSessionEvents(url: String): Flow<AgentEvent> = flow {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .build()

            val call = client.newCall(request)
            val response = call.execute()

            if (!response.isSuccessful) {
                emit(AgentEvent.Error("HTTP ${response.code}: ${response.message}"))
                response.close()
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(AgentEvent.Error("Response body was null"))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var currentEvent: String? = null
            val dataBuffer = StringBuilder()

            try {
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) {
                        if (dataBuffer.isNotEmpty()) {
                            val eventType = currentEvent ?: "token"
                            val event = AgentEvent.parse(eventType, dataBuffer.toString().trim())
                            emit(event)
                            if (event is AgentEvent.MessageComplete || event is AgentEvent.Error) {
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
        } catch (e: Exception) {
            emit(AgentEvent.Error(e.localizedMessage ?: "Agent connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
