package com.example.presentation.tools

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.CustomToolRepository
import com.example.data.repository.TermuxJobRepository
import com.example.database.AppDatabase
import com.example.model.CustomTool
import com.example.model.Job
import com.example.network.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomToolsUiState(
    val tools: List<CustomTool> = emptyList(),
    val toolToEdit: CustomTool? = null,
    val showEditorDialog: Boolean = false,
    val toolToExecute: CustomTool? = null,
    val showExecutionDialog: Boolean = false,
    val messageSnackbar: String? = null,
    val lastExecutedJob: Job? = null
)

class CustomToolsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val toolRepo = CustomToolRepository(db.customToolDao())
    private val preferencesRepo = AppPreferencesRepository(application)
    private val jobRepo = TermuxJobRepository(
        context = application,
        recentJobDao = db.recentJobDao(),
        savedOutputDao = db.savedOutputDao(),
        applicationScope = viewModelScope
    )

    private val _uiState = MutableStateFlow(CustomToolsUiState())
    val uiState: StateFlow<CustomToolsUiState> = _uiState.asStateFlow()

    private var serverConfig = ServerConfig()

    init {
        viewModelScope.launch {
            preferencesRepo.preferencesFlow.collect { prefs ->
                serverConfig = ServerConfig(prefs.backendHost, prefs.backendPort)
            }
        }

        viewModelScope.launch {
            toolRepo.allToolsFlow.collect { list ->
                _uiState.update { it.copy(tools = list) }
            }
        }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(toolToEdit = null, showEditorDialog = true) }
    }

    fun openEditDialog(tool: CustomTool) {
        _uiState.update { it.copy(toolToEdit = tool, showEditorDialog = true) }
    }

    fun closeEditorDialog() {
        _uiState.update { it.copy(toolToEdit = null, showEditorDialog = false) }
    }

    fun openExecutionDialog(tool: CustomTool) {
        _uiState.update { it.copy(toolToExecute = tool, showExecutionDialog = true) }
    }

    fun closeExecutionDialog() {
        _uiState.update { it.copy(toolToExecute = null, showExecutionDialog = false) }
    }

    fun saveTool(tool: CustomTool) {
        viewModelScope.launch {
            toolRepo.saveTool(tool)
            _uiState.update { it.copy(showEditorDialog = false, toolToEdit = null, messageSnackbar = "Action saved: ${tool.name}") }
        }
    }

    fun deleteTool(id: String) {
        viewModelScope.launch {
            toolRepo.deleteTool(id)
            _uiState.update { it.copy(messageSnackbar = "Action removed") }
        }
    }

    fun toggleToolEnabled(tool: CustomTool) {
        viewModelScope.launch {
            val newStatus = !tool.enabled
            toolRepo.toggleToolEnabled(tool.id, newStatus)
        }
    }

    fun executeTool(tool: CustomTool, inputs: Map<String, String>) {
        closeExecutionDialog()
        val resolvedArgs = toolRepo.buildResolvedArguments(tool.arguments, inputs)

        viewModelScope.launch {
            val result = jobRepo.submitJob(
                config = serverConfig,
                type = "CUSTOM_ACTION",
                executable = tool.executable,
                arguments = resolvedArgs,
                description = "${tool.name} (${tool.executable})"
            )

            result.fold(
                onSuccess = { createdJob ->
                    _uiState.update {
                        it.copy(
                            lastExecutedJob = createdJob,
                            messageSnackbar = "Action running: ${tool.name} (Job: ${createdJob.jobId})"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            messageSnackbar = "Execution failed: ${err.localizedMessage ?: "Check Termux connection"}"
                        )
                    }
                }
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(messageSnackbar = null) }
    }
}
