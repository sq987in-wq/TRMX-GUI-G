package com.example.data.repository

import com.example.model.AgentMessage
import com.example.model.AgentSession
import com.example.model.CreateSessionRequest
import com.example.model.MessageRole
import com.example.model.SendMessageRequest
import com.example.network.ApiClientFactory
import com.example.network.ServerConfig
import com.example.network.sse.AgentEvent
import com.example.network.sse.AgentSseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AgentRepository(
    private val sseClient: AgentSseClient = AgentSseClient(),
    private val scope: CoroutineScope
) {
    private val _currentSession = MutableStateFlow<AgentSession?>(null)
    val currentSession: StateFlow<AgentSession?> = _currentSession.asStateFlow()

    private var streamingJob: Job? = null

    suspend fun getOrCreateSession(config: ServerConfig): Result<AgentSession> = withContext(Dispatchers.IO) {
        val existing = _currentSession.value
        if (existing != null) return@withContext Result.success(existing)

        try {
            val api = ApiClientFactory.createService(config)
            val response = api.createAgentSession(CreateSessionRequest(title = "CommandDeck Assistant"))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val session = AgentSession(
                    sessionId = body.sessionId,
                    title = body.title,
                    messages = listOf(
                        AgentMessage(
                            id = "welcome_msg",
                            role = MessageRole.ASSISTANT,
                            content = "CommandDeck CLI Agent initialized. Ask about Termux packages, CLI pipelines, bash scripts, or task debugging."
                        )
                    )
                )
                _currentSession.value = session
                Result.success(session)
            } else {
                // Fallback to local session ID if backend session endpoint responds without body
                val fallbackId = "session_" + UUID.randomUUID().toString().take(8)
                val session = AgentSession(
                    sessionId = fallbackId,
                    title = "Local Assistant",
                    messages = listOf(
                        AgentMessage(
                            id = "welcome_msg",
                            role = MessageRole.ASSISTANT,
                            content = "CommandDeck Agent ready. Connect to Termux backend on localhost to execute agent actions."
                        )
                    )
                )
                _currentSession.value = session
                Result.success(session)
            }
        } catch (e: Exception) {
            // Local fallback session
            val fallbackId = "session_" + UUID.randomUUID().toString().take(8)
            val session = AgentSession(
                sessionId = fallbackId,
                title = "Local Assistant",
                messages = listOf(
                    AgentMessage(
                        id = "welcome_msg",
                        role = MessageRole.ASSISTANT,
                        content = "Termux agent offline. Please start the Termux backend service at ${config.baseUrl}."
                    )
                )
            )
            _currentSession.value = session
            Result.success(session)
        }
    }

    suspend fun sendMessage(
        config: ServerConfig,
        userPrompt: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: getOrCreateSession(config).getOrNull()
            ?: return@withContext Result.failure(Exception("No active agent session"))

        val userMessage = AgentMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = userPrompt
        )

        val assistantMessageId = UUID.randomUUID().toString()
        val assistantMessage = AgentMessage(
            id = assistantMessageId,
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _currentSession.update { current ->
            current?.copy(
                messages = current.messages + userMessage + assistantMessage,
                isGenerating = true
            )
        }

        try {
            val api = ApiClientFactory.createService(config)
            val response = api.sendAgentMessage(session.sessionId, SendMessageRequest(userPrompt))
            if (!response.isSuccessful) {
                _currentSession.update { current ->
                    current?.copy(
                        isGenerating = false,
                        messages = current.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(
                                    content = "Backend error (${response.code()}): ${response.errorBody()?.string() ?: "Failed to generate"}",
                                    isStreaming = false
                                )
                            } else msg
                        }
                    )
                }
                return@withContext Result.failure(Exception("Failed to send message: ${response.code()}"))
            }

            // Stream response tokens via SSE
            streamingJob?.cancel()
            streamingJob = scope.launch(Dispatchers.IO) {
                val sseUrl = config.agentSessionEventsUrl(session.sessionId)
                val buffer = StringBuilder()
                sseClient.streamSessionEvents(sseUrl).collect { event ->
                    when (event) {
                        is AgentEvent.Token -> {
                            buffer.append(event.text)
                            _currentSession.update { cur ->
                                cur?.copy(
                                    messages = cur.messages.map { msg ->
                                        if (msg.id == assistantMessageId) {
                                            msg.copy(content = buffer.toString(), isStreaming = true)
                                        } else msg
                                    }
                                )
                            }
                        }
                        is AgentEvent.MessageComplete -> {
                            val finalContent = if (event.fullText.isNotEmpty()) event.fullText else buffer.toString()
                            _currentSession.update { cur ->
                                cur?.copy(
                                    isGenerating = false,
                                    messages = cur.messages.map { msg ->
                                        if (msg.id == assistantMessageId) {
                                            msg.copy(content = finalContent, isStreaming = false)
                                        } else msg
                                    }
                                )
                            }
                        }
                        is AgentEvent.Error -> {
                            buffer.append("\n[Error: ${event.message}]")
                            _currentSession.update { cur ->
                                cur?.copy(
                                    isGenerating = false,
                                    messages = cur.messages.map { msg ->
                                        if (msg.id == assistantMessageId) {
                                            msg.copy(content = buffer.toString(), isStreaming = false)
                                        } else msg
                                    }
                                )
                            }
                        }
                        is AgentEvent.Status -> {
                            // Status update
                        }
                    }
                }

                _currentSession.update { cur ->
                    cur?.copy(
                        isGenerating = false,
                        messages = cur.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(isStreaming = false)
                            } else msg
                        }
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _currentSession.update { cur ->
                cur?.copy(
                    isGenerating = false,
                    messages = cur.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                content = "Connection failed: ${e.localizedMessage ?: "Termux backend unavailable"}. Ensure the Termux backend is running at ${config.baseUrl}.",
                                isStreaming = false
                            )
                        } else msg
                    }
                )
            }
            Result.failure(e)
        }
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        streamingJob = null
        _currentSession.update { cur ->
            cur?.copy(
                isGenerating = false,
                messages = cur.messages.map { msg ->
                    if (msg.isStreaming) msg.copy(isStreaming = false) else msg
                }
            )
        }
    }

    fun clearHistory() {
        _currentSession.update { cur ->
            cur?.copy(messages = emptyList(), isGenerating = false)
        }
    }
}
