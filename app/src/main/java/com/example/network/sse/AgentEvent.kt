package com.example.network.sse

import org.json.JSONObject

sealed class AgentEvent {
    data class Token(val text: String) : AgentEvent()
    data class MessageComplete(val fullText: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    data class Status(val status: String) : AgentEvent()

    companion object {
        fun parse(eventType: String, data: String): AgentEvent {
            return try {
                when (eventType.trim().lowercase()) {
                    "token" -> {
                        val obj = JSONObject(data)
                        Token(obj.optString("text", ""))
                    }
                    "complete" -> {
                        val obj = JSONObject(data)
                        MessageComplete(obj.optString("fullText", obj.optString("full_text", "")))
                    }
                    "error" -> {
                        val obj = JSONObject(data)
                        Error(obj.optString("message", "Error in agent execution"))
                    }
                    "status" -> {
                        val obj = JSONObject(data)
                        Status(obj.optString("status", "running"))
                    }
                    else -> Token(data)
                }
            } catch (_: Exception) {
                Token(data)
            }
        }
    }
}
