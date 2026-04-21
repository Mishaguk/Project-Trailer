package com.github.mishaguk.projecttrailer.ai.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.APP)
class AiKeyProvider {

    fun getApiKey(): String? {
        val stream = javaClass.classLoader.getResourceAsStream(AiConfig.ENV_RESOURCE)
        if (stream == null) {
            thisLogger().info("${AiConfig.ENV_RESOURCE} resource not found on plugin classpath")
            return null
        }
        val lines = stream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
        val value = parseEnvFile(lines)[AiConfig.KEY_NAME]?.takeIf { it.isNotBlank() }
        thisLogger().info(
            if (value == null) "${AiConfig.KEY_NAME} missing in ${AiConfig.ENV_RESOURCE}"
            else "${AiConfig.KEY_NAME} loaded from ${AiConfig.ENV_RESOURCE}"
        )
        return value
    }

    private fun parseEnvFile(lines: List<String>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val key = line.substring(0, eq).trim()
            var value = line.substring(eq + 1).trim()
            if (value.length >= 2 &&
                ((value.first() == '"' && value.last() == '"') ||
                    (value.first() == '\'' && value.last() == '\''))
            ) {
                value = value.substring(1, value.length - 1)
            }
            out[key] = value
        }
        return out
    }

    companion object {
        fun getInstance(): AiKeyProvider = service()
    }
}
