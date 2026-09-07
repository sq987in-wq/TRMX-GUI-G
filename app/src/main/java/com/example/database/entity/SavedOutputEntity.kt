package com.example.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.OutputFile

@Entity(tableName = "saved_outputs")
data class SavedOutputEntity(
    @PrimaryKey val fileId: String,
    val jobId: String,
    val filename: String,
    val sizeBytes: Long,
    val mimeType: String,
    val downloadUrl: String,
    val savedAt: Long = System.currentTimeMillis(),
    val localUri: String? = null
) {
    fun toOutputFile(): OutputFile {
        return OutputFile(
            fileId = fileId,
            filename = filename,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            downloadUrl = downloadUrl
        )
    }

    companion object {
        fun fromOutputFile(output: OutputFile, jobId: String, localUri: String? = null): SavedOutputEntity {
            return SavedOutputEntity(
                fileId = output.fileId,
                jobId = jobId,
                filename = output.filename,
                sizeBytes = output.sizeBytes,
                mimeType = output.mimeType,
                downloadUrl = output.downloadUrl,
                savedAt = System.currentTimeMillis(),
                localUri = localUri
            )
        }
    }
}
