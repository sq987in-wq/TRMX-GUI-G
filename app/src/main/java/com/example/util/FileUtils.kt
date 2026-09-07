package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    /**
     * Resolves an Android SAF Uri into an absolute file path accessible to CLI processes.
     * Copies the content stream to app's cache directory if direct path is not accessible.
     */
    fun resolveUriToLocalPath(context: Context, uri: Uri): String {
        // Direct file scheme
        if (uri.scheme == "file") {
            return uri.path ?: uri.toString()
        }

        // Query display name
        val fileName = getFileName(context, uri) ?: "input_file_${System.currentTimeMillis()}"

        try {
            val cacheDir = File(context.cacheDir, "cli_inputs")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val targetFile = File(cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            // Return uri string as fallback
            return uri.path ?: uri.toString()
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (name == null) {
            val path = uri.path
            if (path != null) {
                val cut = path.lastIndexOf('/')
                if (cut != -1) {
                    name = path.substring(cut + 1)
                }
            }
        }
        return name
    }
}
