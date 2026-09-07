package com.example.presentation.dashboard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.DeckPreferences
import com.example.data.repository.CustomToolRepository
import com.example.data.repository.ServerHealthRepository
import com.example.data.repository.TermuxJobRepository
import com.example.data.system.DeviceSystemStatus
import com.example.data.system.SystemStatusProvider
import com.example.database.AppDatabase
import com.example.model.CustomTool
import com.example.model.Job
import com.example.model.JobStatus
import com.example.model.OutputFile
import com.example.model.ServerHealth
import com.example.network.ServerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val serverHealth: ServerHealth = ServerHealth(),
    val systemStatus: DeviceSystemStatus? = null,
    val activeJobs: List<Job> = emptyList(),
    val recentJobs: List<Job> = emptyList(),
    val currentJob: Job? = null,
    val isSubmittingJob: Boolean = false,
    val messageSnackbar: String? = null,
    val showDiagnostics: Boolean = false,
    val selectedDownloadMode: String = "VIDEO", // "VIDEO" (MP4) or "AUDIO" (MP3)
    val mediaUrl: String = "",
    val compressorInputFile: String = "",
    val compressorPreset: String = "MEDIUM", // LOW, MEDIUM, ULTRA
    val extractorInputFile: String = "",
    val extractorBitrate: String = "192k", // 128k, 192k, 320k
    val ttsText: String = "",
    val ttsVoice: String = "en-US-ChristopherNeural",
    val pinnedTools: List<CustomTool> = emptyList(),
    val allTools: List<CustomTool> = emptyList(),
    val showCatalogDialog: Boolean = false,
    val catalogCategory: String = "All",
    val executionDialogTool: CustomTool? = null
)

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val preferencesRepo = AppPreferencesRepository(application)
    private val healthRepo = ServerHealthRepository()
    private val systemStatusProvider = SystemStatusProvider(application)
    private val customToolRepo = CustomToolRepository(db.customToolDao())

    private val jobRepo = TermuxJobRepository(
        context = application,
        recentJobDao = db.recentJobDao(),
        savedOutputDao = db.savedOutputDao(),
        applicationScope = viewModelScope
    )

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var currentConfig = ServerConfig()

    init {
        // Observe preferences and synchronize server config
        viewModelScope.launch {
            preferencesRepo.preferencesFlow.collect { prefs ->
                currentConfig = ServerConfig(prefs.backendHost, prefs.backendPort)
                checkServerHealth()
            }
        }

        // Periodic health check & system status poll every 10s
        viewModelScope.launch {
            while (isActive) {
                updateSystemStatus()
                checkServerHealth()
                delay(10000L)
            }
        }

        // Observe recent jobs from Room
        viewModelScope.launch {
            jobRepo.recentJobsFlow.collect { jobs ->
                _uiState.update { it.copy(recentJobs = jobs) }
            }
        }

        // Observe active jobs from memory
        viewModelScope.launch {
            jobRepo.activeJobs.collect { activeMap ->
                val activeList = activeMap.values.toList()
                _uiState.update { current ->
                    val updatedCurrentJob = current.currentJob?.let { activeMap[it.jobId] ?: it }
                    current.copy(activeJobs = activeList, currentJob = updatedCurrentJob)
                }
            }
        }

        // Observe pinned tools
        viewModelScope.launch {
            customToolRepo.pinnedToolsFlow.collect { pinned ->
                _uiState.update { it.copy(pinnedTools = pinned) }
            }
        }

        // Observe all tools
        viewModelScope.launch {
            customToolRepo.enabledToolsFlow.collect { tools ->
                _uiState.update { it.copy(allTools = tools) }
            }
        }
    }

    fun setCatalogDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showCatalogDialog = visible) }
    }

    fun setCatalogCategory(category: String) {
        _uiState.update { it.copy(catalogCategory = category) }
    }

    fun openToolExecution(tool: CustomTool) {
        if (tool.inputType == "NONE" && (tool.inputDefinitions.isEmpty() || tool.inputDefinitions.all { !it.required })) {
            executeCustomTool(tool, emptyMap())
        } else {
            _uiState.update { it.copy(executionDialogTool = tool) }
        }
    }

    fun dismissToolExecution() {
        _uiState.update { it.copy(executionDialogTool = null) }
    }

    fun toggleToolPinned(toolId: String, isPinned: Boolean) {
        viewModelScope.launch {
            customToolRepo.toggleToolPinned(toolId, isPinned)
        }
    }

    fun executeCustomTool(tool: CustomTool, inputs: Map<String, String>) {
        _uiState.update { it.copy(executionDialogTool = null) }
        val resolvedArgs = tool.resolveArguments(inputs)
        submitJobInternal(
            type = "CUSTOM_TOOL",
            executable = tool.executable,
            arguments = resolvedArgs,
            description = "${tool.title}: ${resolvedArgs.joinToString(" ").take(40)}"
        )
    }

    fun updateSystemStatus() {
        try {
            val status = systemStatusProvider.getSystemStatus()
            _uiState.update { it.copy(systemStatus = status) }
        } catch (_: Exception) {
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            val result = healthRepo.checkHealth(currentConfig)
            _uiState.update { it.copy(serverHealth = result) }
        }
    }

    fun setDiagnosticsVisible(visible: Boolean) {
        _uiState.update { it.copy(showDiagnostics = visible) }
    }

    fun setDownloadMode(mode: String) {
        _uiState.update { it.copy(selectedDownloadMode = mode) }
    }

    fun setMediaUrl(url: String) {
        _uiState.update { it.copy(mediaUrl = url) }
    }

    fun setCompressorInputFile(file: String) {
        _uiState.update { it.copy(compressorInputFile = file) }
    }

    fun setCompressorPreset(preset: String) {
        _uiState.update { it.copy(compressorPreset = preset) }
    }

    fun setTtsText(text: String) {
        _uiState.update { it.copy(ttsText = text) }
    }

    fun setTtsVoice(voice: String) {
        _uiState.update { it.copy(ttsVoice = voice) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(messageSnackbar = null) }
    }

    fun selectJob(job: Job?) {
        _uiState.update { it.copy(currentJob = job) }
    }

    // Media Downloader Tool using yt-dlp structurally
    fun startMediaDownload() {
        val url = _uiState.value.mediaUrl.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(messageSnackbar = "Please enter a valid media URL") }
            return
        }

        val isAudio = _uiState.value.selectedDownloadMode == "AUDIO"
        val args = if (isAudio) {
            listOf(
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--progress",
                "--no-mtime",
                "-o", "output/%(title)s.%(ext)s",
                url
            )
        } else {
            listOf(
                "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
                "--progress",
                "--no-mtime",
                "-o", "output/%(title)s.%(ext)s",
                url
            )
        }

        submitJobInternal(
            type = "MEDIA_DOWNLOAD",
            executable = "yt-dlp",
            arguments = args,
            description = "Download: $url"
        )
    }

    // Media Compressor Tool using ffmpeg structurally
    fun startMediaCompression() {
        val input = _uiState.value.compressorInputFile.trim()
        if (input.isEmpty()) {
            _uiState.update { it.copy(messageSnackbar = "Please specify a source media path") }
            return
        }

        val crf = when (_uiState.value.compressorPreset) {
            "LOW" -> "22"
            "ULTRA" -> "34"
            else -> "28"
        }

        val args = listOf(
            "-i", input,
            "-vcodec", "libx264",
            "-crf", crf,
            "-preset", "faster",
            "-acodec", "aac",
            "-b:a", "128k",
            "-y",
            "output/compressed_output.mp4"
        )

        submitJobInternal(
            type = "MEDIA_COMPRESS",
            executable = "ffmpeg",
            arguments = args,
            description = "Compress: $input (${_uiState.value.compressorPreset})"
        )
    }

    fun setExtractorInputFile(path: String) {
        _uiState.update { it.copy(extractorInputFile = path) }
    }

    fun setExtractorBitrate(bitrate: String) {
        _uiState.update { it.copy(extractorBitrate = bitrate) }
    }

    // Audio Extractor Tool using ffmpeg structurally
    fun startAudioExtraction() {
        val input = _uiState.value.extractorInputFile.trim()
        if (input.isEmpty()) {
            _uiState.update { it.copy(messageSnackbar = "Please specify a source media path") }
            return
        }

        val bitrate = _uiState.value.extractorBitrate
        val args = listOf(
            "-i", input,
            "-vn",
            "-acodec", "libmp3lame",
            "-b:a", bitrate,
            "-y",
            "output/extracted_audio.mp3"
        )

        submitJobInternal(
            type = "AUDIO_EXTRACT",
            executable = "ffmpeg",
            arguments = args,
            description = "Extract Audio: $input ($bitrate)"
        )
    }

    // Text-to-Speech Tool using edge-tts structurally
    fun startTts() {
        val text = _uiState.value.ttsText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(messageSnackbar = "Please enter text for synthesis") }
            return
        }

        val voice = _uiState.value.ttsVoice
        val args = listOf(
            "--voice", voice,
            "--text", text,
            "--write-media", "output/speech.mp3"
        )

        submitJobInternal(
            type = "TEXT_TO_SPEECH",
            executable = "edge-tts",
            arguments = args,
            description = "TTS: ${text.take(30)}..."
        )
    }

    private fun submitJobInternal(
        type: String,
        executable: String,
        arguments: List<String>,
        description: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingJob = true) }
            val result = jobRepo.submitJob(
                config = currentConfig,
                type = type,
                executable = executable,
                arguments = arguments,
                description = description
            )
            _uiState.update { it.copy(isSubmittingJob = false) }

            result.fold(
                onSuccess = { createdJob ->
                    _uiState.update {
                        it.copy(
                            currentJob = createdJob,
                            messageSnackbar = "Job created: ${createdJob.jobId}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            messageSnackbar = "Execution error: ${error.localizedMessage ?: "Failed to reach Termux"}"
                        )
                    }
                }
            )
        }
    }

    fun cancelJob(jobId: String) {
        viewModelScope.launch {
            val result = jobRepo.cancelJob(currentConfig, jobId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(messageSnackbar = "Job cancelled") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(messageSnackbar = "Failed to cancel: ${error.message}") }
                }
            )
        }
    }

    fun exportOutputFile(file: OutputFile, jobId: String) {
        viewModelScope.launch {
            val result = jobRepo.exportFileToStorage(currentConfig, file, jobId)
            result.fold(
                onSuccess = { uri ->
                    _uiState.update { it.copy(messageSnackbar = "Exported to Downloads: ${file.filename}") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(messageSnackbar = "Export failed: ${error.message}") }
                }
            )
        }
    }
}
