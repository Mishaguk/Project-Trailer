package com.github.mishaguk.projecttrailer.ai

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * MVP tool-using agent for tour generation. Alternates between model turns and
 * local `read_file` execution, then forces one final structured-JSON turn.
 */
internal object TourAgent {

    private const val MAX_ITERATIONS = 6
    private val json = Json { ignoreUnknownKeys = true }

    private const val READ_FILE_TOOL = """
        {"type":"function","function":{
          "name":"read_file",
          "description":"Read a file from the project. Path is project-relative. Optional 1-based line range.",
          "parameters":{
            "type":"object",
            "properties":{
              "path":{"type":"string"},
              "startLine":{"type":"integer"},
              "endLine":{"type":"integer"}
            },
            "required":["path"]
          }
        }}
    """

    fun run(project: Project, structure: String): Result<String> {
        val client = OpenAiClient.getInstance()
        val esc: (String) -> String = { client.jsonEscapePublic(it) }

        val messages = mutableListOf<String>()
        messages += """{"role":"system","content":"${esc(TourSchema.SYSTEM_PROMPT_AGENT)}"}"""
        messages += """{"role":"user","content":"${esc(TourSchema.userPrompt(structure))}"}"""

        var iteration = 0
        while (iteration < MAX_ITERATIONS) {
            iteration++
            val body = buildBody(messages, withTools = true, withSchema = false)
            val msgJson = client.chatRawMessage(body).getOrElse { return Result.failure(it) }
            val msg = json.parseToJsonElement(msgJson).jsonObject
            val toolCalls = msg["tool_calls"]?.takeIf { it is JsonArray }?.jsonArray

            if (toolCalls == null || toolCalls.isEmpty()) {
                thisLogger().info("TourAgent: no tool calls on iter $iteration, forcing final turn")
                break
            }

            thisLogger().warn("TourAgent: iter $iteration, ${toolCalls.size} tool call(s)")
            println("===== TourAgent iter $iteration: ${toolCalls.size} tool call(s) =====")
            messages += msgJson  // echo assistant message with tool_calls
            for (call in toolCalls) {
                val obj = call.jsonObject
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                val fn = obj["function"]?.jsonObject ?: continue
                val name = fn["name"]?.jsonPrimitive?.contentOrNull
                val argsStr = fn["arguments"]?.jsonPrimitive?.contentOrNull ?: "{}"
                val result = executeTool(project, name, argsStr)
                val preview = result.take(200).replace('\n', ' ')

                messages += """{"role":"tool","tool_call_id":"${esc(id)}","content":"${esc(result)}"}"""
            }
        }

        // Final turn: force structured JSON, no tools.
        messages += """{"role":"system","content":"${esc("No more tool calls. Respond now with the final JSON matching the schema.")}"}"""
        val finalBody = buildBody(messages, withTools = false, withSchema = true)
        val finalMsgJson = OpenAiClient.getInstance().chatRawMessage(finalBody).getOrElse { return Result.failure(it) }
        val content = json.parseToJsonElement(finalMsgJson).jsonObject["content"]?.jsonPrimitive?.contentOrNull
            ?: return Result.failure(IllegalStateException("Empty final content"))
        return Result.success(content)
    }

    private fun executeTool(project: Project, name: String?, argsJson: String): String {
        if (name != "read_file") return "ERROR: unknown tool '$name'"
        return try {
            val args = Json.parseToJsonElement(argsJson).jsonObject
            val path = args["path"]?.jsonPrimitive?.contentOrNull ?: return "ERROR: missing 'path'"
            val startLine = args["startLine"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            val endLine = args["endLine"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            FileReader.read(project, path, startLine, endLine)
        } catch (e: Exception) {
            "ERROR: bad arguments: ${e.message}"
        }
    }

    private fun buildBody(messages: List<String>, withTools: Boolean, withSchema: Boolean): String {
        val sb = StringBuilder()
        sb.append("""{"model":"${AiConfig.MODEL}","messages":[""")
        sb.append(messages.joinToString(","))
        sb.append("]")
        if (withTools) {
            sb.append(""","tools":[""").append(READ_FILE_TOOL.trim()).append("]")
        }
        if (withSchema) {
            sb.append(""","response_format":{"type":"json_schema","json_schema":{"name":"tour","strict":true,"schema":""")
            sb.append(TourSchema.JSON_SCHEMA)
            sb.append("}}")
        }
        sb.append("}")
        return sb.toString()
    }
}
