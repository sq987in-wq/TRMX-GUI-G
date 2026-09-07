package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.database.dao.RecentJobDao
import com.example.database.dao.SavedOutputDao
import com.example.database.entity.RecentJobEntity
import com.example.database.entity.SavedOutputEntity
import com.example.model.Job
import com.example.model.JobCreateRequest
import com.example.model.JobStatus
import com.example.model.OutputFile
import com.example.network.ApiClientFactory
import com.example.network.ServerConfig
import com.example.network.sse.JobEvent
import com.example.network.sse.JobSseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TermuxJobRepository(
    private val context: Context,
    private val recentJobDao: RecentJobDao,
    private val savedOutputDao: SavedOutputDao,
    private val sseClient: JobSseClient = JobSseClient(),
    private val applicationScope: CoroutineScope
) {
    // Map of jobId to active streaming Job coroutines
    private val activeJobStreams = mutableMapOf<String, CoroutineJob>()

    // In-memory cache of active jobs for fast UI response
    private val _activeJobs = MutableStateFlow<Map<String, Job>>(emptyMap())
    val activeJobs = _activeJobs.asStateFlow()

    val recentJobsFlow: Flow<List<Job>> = recentJobDao.getAllRecentJobs().map { list ->
        list.map { it.toDomain() }
    }

    val savedOutputsFlow: Flow<List<OutputFile>> = savedOutputDao.getAllSavedOutputs().map { list ->
        list.map { it.toOutputFile() }
    }

    suspend fun submitJob(
        config: ServerConfig,
        type: String,
        executable: String,
        arguments: List<String>,
        description: String = ""
    ): Result<Job> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClientFactory.createService(config)
            val request = JobCreateRequest(
                type = type,
                executable = executable,
                arguments = arguments,
                description = description
            )
            val response = api.createJob(request)
            if (response.isSuccessful && response.body() != null) {
                val createdJob = response.body()!!
                recentJobDao.insertOrUpdate(RecentJobEntity.fromDomain(createdJob))
                _activeJobs.update { it + (createdJob.jobId to createdJob) }

                // Start SSE streaming for this job
                startJobEventStreaming(config, createdJob.jobId)

                Result.success(createdJob)
            } else {
                val err = response.errorBody()?.string() ?: "Failed to create job (${response.code()})"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startJobEventStreaming(config: ServerConfig, jobId: String) {
        if (activeJobStreams[jobId]?.isActive == true) return

        val jobCoroutine = applicationScope.launch(Dispatchers.IO) {
            val sseUrl = config.jobEventsUrl(jobId)
            sseClient.streamJobEvents(sseUrl).collect { event ->
                handleJobEvent(jobId, event)
            }
        }
        activeJobStreams[jobId] = jobCoroutine
    }

    private suspend fun handleJobEvent(jobId: String, event: JobEvent) {
        _activeJobs.update { currentMap ->
            val current = currentMap[jobId] ?: run {
                val fromDb = recentJobDao.getJobById(jobId)?.toDomain()
                fromDb ?: Job(
                    jobId = jobId,
                    type = "CLI",
                    command = "",
                    status = JobStatus.RUNNING
                )
            }

            val updated = when (event) {
                is JobEvent.Status -> {
                    val newStatus = try {
                        JobStatus.valueOf(event.status.uppercase())
                    } catch (_: Exception) {
                        current.status
                    }
                    current.copy(status = newStatus, updatedAt = System.currentTimeMillis())
                }
                is JobEvent.Progress -> {
                    current.copy(
                        progress = event.percent,
                        speed = event.speed ?: current.speed,
                        eta = event.eta ?: current.eta,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is JobEvent.Stdout -> {
                    current.copy(
                        stdout = (current.stdout + event.line).takeLast(200),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is JobEvent.Stderr -> {
                    current.copy(
                        stderr = (current.stderr + event.line).takeLast(200),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is JobEvent.FileGenerated -> {
                    val files = (current.outputFiles + event.file).distinctBy { it.fileId }
                    current.copy(
                        outputFiles = files,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is JobEvent.Completed -> {
                    current.copy(
                        status = JobStatus.COMPLETED,
                        progress = 100f,
                        exitCode = event.exitCode,
                        completedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is JobEvent.Failed -> {
                    current.copy(
                        status = JobStatus.FAILED,
                        exitCode = event.exitCode,
                        errorMessage = event.error,
                        completedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is JobEvent.Cancelled -> {
                    current.copy(
                        status = JobStatus.CANCELLED,
                        errorMessage = event.reason,
                        completedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is JobEvent.Unknown -> current
            }

            // Persist to Room
            recentJobDao.insertOrUpdate(RecentJobEntity.fromDomain(updated))

            // Save output files to SavedOutputDao if completed
            if (updated.status == JobStatus.COMPLETED) {
                updated.outputFiles.forEach { file ->
                    savedOutputDao.insertSavedOutput(SavedOutputEntity.fromOutputFile(file, jobId))
                }
            }

            currentMap + (jobId to updated)
        }
    }

    suspend fun cancelJob(config: ServerConfig, jobId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClientFactory.createService(config)
            val response = api.cancelJob(jobId)
            activeJobStreams[jobId]?.cancel()
            activeJobStreams.remove(jobId)

            if (response.isSuccessful) {
                val current = _activeJobs.value[jobId]
                if (current != null) {
                    val updated = current.copy(status = JobStatus.CANCELLED, completedAt = System.currentTimeMillis())
                    recentJobDao.insertOrUpdate(RecentJobEntity.fromDomain(updated))
                    _activeJobs.update { it + (jobId to updated) }
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to cancel job: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads file and exports it to Android MediaStore or App External Storage.
     * Complies strictly with zero-permission SAF/MediaStore policies.
     */
    suspend fun exportFileToStorage(
        config: ServerConfig,
        file: OutputFile,
        jobId: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClientFactory.createService(config)
            val response = api.downloadFile(file.fileId)
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(Exception("Failed to download file from backend: ${response.code()}"))
            }

            val body = response.body()!!
            val filename = file.filename.ifEmpty { "download_${file.fileId}" }
            val mimeType = file.mimeType.ifEmpty { "application/octet-stream" }

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CommandDeck")
                }
                val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Could not create MediaStore entry"))

                resolver.openOutputStream(insertUri)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                insertUri
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "CommandDeck")
                if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, filename)
                FileOutputStream(targetFile).use { outputStream ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Uri.fromFile(targetFile)
            }

            // Record local export
            savedOutputDao.insertSavedOutput(
                SavedOutputEntity.fromOutputFile(file, jobId, uri.toString())
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
