package com.example.network

data class ServerConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT
) {
    val baseUrl: String
        get() = "http://$host:$port/"

    val sseJobsUrl: String
        get() = "${baseUrl}jobs"

    fun jobEventsUrl(jobId: String): String =
        "${baseUrl}jobs/$jobId/events"

    fun fileDownloadUrl(fileId: String): String =
        "${baseUrl}files/$fileId"

    fun agentSessionEventsUrl(sessionId: String): String =
        "${baseUrl}agent/sessions/$sessionId/events"

    companion object {
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 8080
    }
}
