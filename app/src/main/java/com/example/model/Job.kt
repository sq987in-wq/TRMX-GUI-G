package com.example.model

enum class JobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class OutputFile(
    val fileId: String,
    val filename: String,
    val sizeBytes: Long = 0L,
    val mimeType: String = "application/octet-stream",
    val downloadUrl: String = ""
)

data class Job(
    val jobId: String,
    val type: String,
    val command: String,
    val status: JobStatus,
    val progress: Float = 0f,
    val speed: String? = null,
    val eta: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val exitCode: Int? = null,
    val stdout: List<String> = emptyList(),
    val stderr: List<String> = emptyList(),
    val outputFiles: List<OutputFile> = emptyList(),
    val errorMessage: String? = null
)

data class JobCreateRequest(
    val type: String,
    val executable: String,
    val arguments: List<String>,
    val description: String = ""
)

data class JobCancelResponse(
    val success: Boolean,
    val message: String
)
