package com.example.data.repository

import com.example.database.dao.CustomToolDao
import com.example.database.entity.CustomToolEntity
import com.example.model.CustomTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomToolRepository(
    private val customToolDao: CustomToolDao
) {
    val allToolsFlow: Flow<List<CustomTool>> = customToolDao.getAllTools().map { list ->
        list.map { it.toDomain() }
    }

    val enabledToolsFlow: Flow<List<CustomTool>> = customToolDao.getEnabledTools().map { list ->
        list.map { it.toDomain() }
    }

    val pinnedToolsFlow: Flow<List<CustomTool>> = customToolDao.getPinnedTools().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getToolById(id: String): CustomTool? = withContext(Dispatchers.IO) {
        customToolDao.getToolById(id)?.toDomain()
    }

    suspend fun saveTool(tool: CustomTool) = withContext(Dispatchers.IO) {
        customToolDao.insertTool(CustomToolEntity.fromDomain(tool))
    }

    suspend fun deleteTool(id: String) = withContext(Dispatchers.IO) {
        customToolDao.deleteToolById(id)
    }

    suspend fun toggleToolEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        customToolDao.setToolEnabled(id, enabled)
    }

    suspend fun toggleToolPinned(id: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        customToolDao.setToolPinned(id, isPinned)
    }

    suspend fun updateToolOrder(id: String, order: Int) = withContext(Dispatchers.IO) {
        customToolDao.updateSortOrder(id, order)
    }

    /**
     * Resolves command argument templates structurally.
     * Replaces placeholder tokens like "{input}" or "{output}" with sanitized provided values.
     * No shell expansion or arbitrary eval is performed.
     */
    fun buildResolvedArguments(
        templateArgs: List<String>,
        inputValues: Map<String, String>
    ): List<String> {
        return templateArgs.map { arg ->
            var resolved = arg
            for ((key, value) in inputValues) {
                resolved = resolved.replace("{$key}", value)
            }
            resolved
        }
    }
}
