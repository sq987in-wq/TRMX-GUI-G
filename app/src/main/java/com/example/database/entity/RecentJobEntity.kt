package com.example.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Job
import com.example.model.JobStatus
import com.example.model.OutputFile
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "recent_jobs")
data class RecentJobEntity(
    @PrimaryKey val jobId: String,
    val type: String,
    val command: String,
    val status: String,
    val progress: Float,
    val startedAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val exitCode: Int?,
    val outputFilesJson: String,
    val errorMessage: String?
) {
    fun toDomain(): Job {
        return Job(
            jobId = jobId,
            type = type,
            command = command,
            status = try {
                JobStatus.valueOf(status)
            } catch (_: Exception) {
                JobStatus.QUEUED
            },
            progress = progress,
            startedAt = startedAt,
            updatedAt = updatedAt,
            completedAt = completedAt,
            exitCode = exitCode,
            outputFiles = parseFilesJson(outputFilesJson),
            errorMessage = errorMessage
        )
    }

    companion object {
        fun fromDomain(job: Job): RecentJobEntity {
            return RecentJobEntity(
                jobId = job.jobId,
                type = job.type,
                command = job.command,
                status = job.status.name,
                progress = job.progress,
                startedAt = job.startedAt,
                updatedAt = job.updatedAt,
                completedAt = job.completedAt,
                exitCode = job.exitCode,
                outputFilesJson = serializeFiles(job.outputFiles),
                errorMessage = job.errorMessage
            )
        }

        fun parseFilesJson(json: String): List<OutputFile> {
            if (json.isBlank()) return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<OutputFile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        OutputFile(
                            fileId = obj.optString("fileId", ""),
                            filename = obj.optString("filename", ""),
                            sizeBytes = obj.optLong("sizeBytes", 0L),
                            mimeType = obj.optString("mimeType", "application/octet-stream"),
                            downloadUrl = obj.optString("downloadUrl", "")
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun serializeFiles(files: List<OutputFile>): String {
            val array = JSONArray()
            files.forEach { file ->
                val obj = JSONObject()
                obj.put("fileId", file.fileId)
                obj.put("filename", file.filename)
                obj.put("sizeBytes", file.sizeBytes)
                obj.put("mimeType", file.mimeType)
                obj.put("downloadUrl", file.downloadUrl)
                array.put(obj)
            }
            return array.toString()
        }
    }
}
