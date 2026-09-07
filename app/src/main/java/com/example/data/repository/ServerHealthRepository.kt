package com.example.data.repository

import com.example.model.ServerHealth
import com.example.model.ServerStatus
import com.example.network.ApiClientFactory
import com.example.network.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ServerHealthRepository {

    private val _healthFlow = MutableStateFlow(ServerHealth())
    val healthFlow: StateFlow<ServerHealth> = _healthFlow.asStateFlow()

    suspend fun checkHealth(config: ServerConfig): ServerHealth = withContext(Dispatchers.IO) {
        _healthFlow.value = _healthFlow.value.copy(
            status = ServerStatus.CONNECTING,
            host = config.host,
            port = config.port
        )

        val startTime = System.currentTimeMillis()
        try {
            val api = ApiClientFactory.createService(config)
            val response = api.checkHealth()
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val result = ServerHealth(
                    status = ServerStatus.ONLINE,
                    host = config.host,
                    port = config.port,
                    latencyMs = latency,
                    lastChecked = System.currentTimeMillis(),
                    backendVersion = body.version,
                    errorMessage = null
                )
                _healthFlow.value = result
                result
            } else {
                val result = ServerHealth(
                    status = ServerStatus.ERROR,
                    host = config.host,
                    port = config.port,
                    latencyMs = latency,
                    lastChecked = System.currentTimeMillis(),
                    errorMessage = "Server returned error: ${response.code()}"
                )
                _healthFlow.value = result
                result
            }
        } catch (e: Exception) {
            val result = ServerHealth(
                status = ServerStatus.OFFLINE,
                host = config.host,
                port = config.port,
                latencyMs = null,
                lastChecked = System.currentTimeMillis(),
                errorMessage = e.localizedMessage ?: "Connection refused"
            )
            _healthFlow.value = result
            result
        }
    }
}
