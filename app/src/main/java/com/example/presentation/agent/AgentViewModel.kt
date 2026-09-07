package com.example.presentation.agent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.AgentRepository
import com.example.data.repository.ServerHealthRepository
import com.example.model.AgentMessage
import com.example.model.AgentSession
import com.example.model.MessageRole
import com.example.model.ServerHealth
import com.example.model.ServerStatus
import com.example.network.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentUiState(
    val session: AgentSession? = null,
    val inputText: String = "",
    val isSending: Boolean = false,
    val serverHealth: ServerHealth = ServerHealth(),
    val errorMessage: String? = null,
    val selectedEngine: String = "GEMINI_CLOUD", // GEMINI_CLOUD, OPENAI_CLOUD, LOCAL_AIDER
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val showApiKeyDialog: Boolean = false
)

class AgentViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val preferencesRepo = AppPreferencesRepository(application)
    private val healthRepo = ServerHealthRepository()
    private val agentRepo = AgentRepository(scope = viewModelScope)

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var serverConfig = ServerConfig()

    init {
        viewModelScope.launch {
            preferencesRepo.preferencesFlow.collect { prefs ->
                serverConfig = ServerConfig(prefs.backendHost, prefs.backendPort)
                _uiState.update {
                    it.copy(
                        selectedEngine = prefs.selectedAiEngine,
                        geminiApiKey = prefs.geminiApiKey,
                        openaiApiKey = prefs.openaiApiKey
                    )
                }
                checkHealthAndInitSession()
            }
        }

        viewModelScope.launch {
            agentRepo.currentSession.collect { session ->
                _uiState.update { it.copy(session = session) }
            }
        }
    }

    private fun checkHealthAndInitSession() {
        viewModelScope.launch {
            val health = healthRepo.checkHealth(serverConfig)
            _uiState.update { it.copy(serverHealth = health) }
            agentRepo.getOrCreateSession(serverConfig)
        }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectEngine(engine: String) {
        _uiState.update { it.copy(selectedEngine = engine) }
        viewModelScope.launch {
            preferencesRepo.updateSelectedAiEngine(engine)
        }
    }

    fun setShowApiKeyDialog(show: Boolean) {
        _uiState.update { it.copy(showApiKeyDialog = show) }
    }

    fun saveApiKeys(geminiKey: String, openaiKey: String) {
        _uiState.update {
            it.copy(
                geminiApiKey = geminiKey.trim(),
                openaiApiKey = openaiKey.trim(),
                showApiKeyDialog = false
            )
        }
        viewModelScope.launch {
            preferencesRepo.updateGeminiApiKey(geminiKey)
            preferencesRepo.updateOpenAiApiKey(openaiKey)
        }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val state = _uiState.value
        _uiState.update { it.copy(inputText = "", isSending = true, errorMessage = null) }

        viewModelScope.launch {
            val result = agentRepo.sendMessage(
                config = serverConfig,
                userPrompt = text,
                engine = state.selectedEngine,
                geminiApiKey = state.geminiApiKey,
                openaiApiKey = state.openaiApiKey
            )
            _uiState.update { it.copy(isSending = false) }
            result.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage) }
            }
        }
    }

    fun stopGeneration() {
        agentRepo.stopGeneration()
    }

    fun clearChat() {
        agentRepo.clearHistory()
    }

    fun retryConnection() {
        checkHealthAndInitSession()
    }
}
