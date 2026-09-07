package com.example.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class AiEngineClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    suspend fun streamGemini(
        apiKey: String,
        prompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        onToken: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            onError("Gemini API key is not configured. Click the Settings icon in the top bar to enter your key.")
            return@withContext
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=$apiKey"

        val contentsArray = JSONArray()
        // Add conversation history if available
        conversationHistory.forEach { (role, text) ->
            val partObj = JSONObject().put("text", text)
            val contentObj = JSONObject()
                .put("role", if (role == "user") "user" else "model")
                .put("parts", JSONArray().put(partObj))
            contentsArray.put(contentObj)
        }
        // Current prompt
        val currentPart = JSONObject().put("text", prompt)
        val currentObj = JSONObject().put("role", "user").put("parts", JSONArray().put(currentPart))
        contentsArray.put(currentObj)

        val requestJson = JSONObject()
            .put("contents", contentsArray)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "You are Termux CommandDeck AI Copilot, a Linux and Android CLI assistant. Provide concise, expert shell pipelines, practical bash scripts, and command recommendations."))))

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val fullText = StringBuilder()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "HTTP ${response.code}"
                val parsedErr = try {
                    JSONObject(errBody).optJSONObject("error")?.optString("message") ?: errBody
                } catch (_: Exception) {
                    errBody
                }
                onError("Gemini API Error (${response.code}): $parsedErr")
                return@withContext
            }

            val inputStream = response.body?.byteStream()
            if (inputStream == null) {
                onError("Empty response stream from Gemini API")
                return@withContext
            }

            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val curLine = line?.trim() ?: continue
                if (curLine.startsWith("data: ")) {
                    val dataJson = curLine.removePrefix("data: ").trim()
                    if (dataJson.isNotEmpty()) {
                        try {
                            val jsonObj = JSONObject(dataJson)
                            val candidates = jsonObj.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val candidate = candidates.getJSONObject(0)
                                val content = candidate.optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    val textPart = parts.getJSONObject(0).optString("text", "")
                                    if (textPart.isNotEmpty()) {
                                        fullText.append(textPart)
                                        withContext(Dispatchers.Main) {
                                            onToken(textPart)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onComplete(fullText.toString())
            }
        } catch (e: Exception) {
            onError("Gemini stream error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun streamOpenAi(
        apiKey: String,
        prompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        onToken: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            onError("OpenAI API key is not configured. Click the Settings icon in the top bar to enter your key.")
            return@withContext
        }

        val url = "https://api.openai.com/v1/chat/completions"

        val messagesArray = JSONArray()
        messagesArray.put(
            JSONObject()
                .put("role", "system")
                .put("content", "You are Termux CommandDeck AI Copilot, a Linux and Android CLI assistant. Provide concise, expert shell pipelines, practical bash scripts, and command recommendations.")
        )
        conversationHistory.forEach { (role, text) ->
            messagesArray.put(
                JSONObject()
                    .put("role", if (role == "user") "user" else "assistant")
                    .put("content", text)
            )
        }
        messagesArray.put(JSONObject().put("role", "user").put("content", prompt))

        val requestJson = JSONObject()
            .put("model", "gpt-4o-mini")
            .put("messages", messagesArray)
            .put("stream", true)

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val fullText = StringBuilder()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "HTTP ${response.code}"
                val parsedErr = try {
                    JSONObject(errBody).optJSONObject("error")?.optString("message") ?: errBody
                } catch (_: Exception) {
                    errBody
                }
                onError("OpenAI API Error (${response.code}): $parsedErr")
                return@withContext
            }

            val inputStream = response.body?.byteStream()
            if (inputStream == null) {
                onError("Empty response stream from OpenAI API")
                return@withContext
            }

            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val curLine = line?.trim() ?: continue
                if (curLine.startsWith("data: ")) {
                    val dataJson = curLine.removePrefix("data: ").trim()
                    if (dataJson == "[DONE]") {
                        break
                    }
                    if (dataJson.isNotEmpty()) {
                        try {
                            val jsonObj = JSONObject(dataJson)
                            val choices = jsonObj.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                val delta = choice.optJSONObject("delta")
                                val textChunk = delta?.optString("content", "") ?: ""
                                if (textChunk.isNotEmpty()) {
                                    fullText.append(textChunk)
                                    withContext(Dispatchers.Main) {
                                        onToken(textChunk)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onComplete(fullText.toString())
            }
        } catch (e: Exception) {
            onError("OpenAI stream error: ${e.localizedMessage ?: e.message}")
        }
    }
}
