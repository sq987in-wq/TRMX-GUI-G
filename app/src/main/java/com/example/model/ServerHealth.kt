package com.example.model

enum class ServerStatus {
    ONLINE,
    OFFLINE,
    CONNECTING,
    ERROR
}

data class ServerHealth(
    val status: ServerStatus = ServerStatus.OFFLINE,
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val latencyMs: Long? = null,
    val lastChecked: Long? = null,
    val backendVersion: String? = null,
    val errorMessage: String? = null
)

data class HealthResponse(
    val status: String,
    val version: String = "1.0.0",
    val timestamp: Long = System.currentTimeMillis()
)
