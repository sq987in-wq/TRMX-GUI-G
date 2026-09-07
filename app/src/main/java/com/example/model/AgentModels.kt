package com.example.model

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class AgentMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

data class AgentSession(
    val sessionId: String,
    val title: String = "CLI Assistant Session",
    val messages: List<AgentMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CreateSessionRequest(
    val title: String? = null,
    val agentType: String = "aider"
)

data class CreateSessionResponse(
    val sessionId: String,
    val title: String,
    val createdAt: Long
)

data class SendMessageRequest(
    val message: String
)
