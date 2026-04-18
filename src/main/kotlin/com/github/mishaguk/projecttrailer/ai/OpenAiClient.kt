package com.github.mishaguk.projecttrailer.ai

import com.github.mishaguk.projecttrailer.ProjectTrailerBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service(Service.Level.APP)
class OpenAiClient {

    private val http: HttpClient by lazy {
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    }

    fun testConnection(): Result<String> {
        val key = AiKeyProvider.getInstance().getApiKey()
            ?: return Result.failure(IllegalStateException(ProjectTrailerBundle.message("ai.test.noKey")))

        val request = HttpRequest.newBuilder()
            .uri(URI.create("${AiConfig.BASE_URL}/models"))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer $key")
            .GET()
            .build()

        return try {
            val response = http.send(request, HttpResponse.BodyHandlers.discarding())
            when (val code = response.statusCode()) {
                200 -> Result.success("OK")
                401 -> Result.failure(IllegalStateException("Invalid key (401)"))
                else -> Result.failure(IllegalStateException("HTTP $code"))
            }
        } catch (e: IOException) {
            Result.failure(IllegalStateException(e.message ?: "Network error"))
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Result.failure(IllegalStateException("Interrupted"))
        }
    }

    companion object {
        fun getInstance(): OpenAiClient = service()
    }
}
