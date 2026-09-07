package com.example.network

import com.example.model.CreateSessionRequest
import com.example.model.CreateSessionResponse
import com.example.model.HealthResponse
import com.example.model.Job
import com.example.model.JobCancelResponse
import com.example.model.JobCreateRequest
import com.example.model.OutputFile
import com.example.model.SendMessageRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface TermuxApiService {

    @POST("health")
    suspend fun checkHealth(): Response<HealthResponse>

    @POST("jobs")
    suspend fun createJob(
        @Body request: JobCreateRequest
    ): Response<Job>

    @GET("jobs")
    suspend fun getJobs(): Response<List<Job>>

    @GET("jobs/{jobId}")
    suspend fun getJob(
        @Path("jobId") jobId: String
    ): Response<Job>

    @POST("jobs/{jobId}/cancel")
    suspend fun cancelJob(
        @Path("jobId") jobId: String
    ): Response<JobCancelResponse>

    @GET("jobs/{jobId}/files")
    suspend fun getJobFiles(
        @Path("jobId") jobId: String
    ): Response<List<OutputFile>>

    @Streaming
    @GET("files/{fileId}")
    suspend fun downloadFile(
        @Path("fileId") fileId: String
    ): Response<ResponseBody>

    @POST("agent/sessions")
    suspend fun createAgentSession(
        @Body request: CreateSessionRequest
    ): Response<CreateSessionResponse>

    @POST("agent/sessions/{sessionId}/messages")
    suspend fun sendAgentMessage(
        @Path("sessionId") sessionId: String,
        @Body request: SendMessageRequest
    ): Response<ResponseBody>
}
