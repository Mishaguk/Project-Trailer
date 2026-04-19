package com.github.mishaguk.projecttrailer.toolWindow

import com.github.mishaguk.projecttrailer.ProjectTrailerBundle
import com.github.mishaguk.projecttrailer.ai.ChatService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ChatPanelBridge(private val project: Project) {

    var onExplainRequest: ((userLabel: String, question: String) -> Unit)? = null

    fun submitExplainRequest(relativePath: String, question: String) {
        val label = ProjectTrailerBundle.message("explain.userLabel", relativePath)
        val callback = onExplainRequest
        if (callback != null) {
            ApplicationManager.getApplication().invokeLater {
                callback(label, question)
            }
        }
    }

    companion object {
        fun getInstance(project: Project): ChatPanelBridge = project.service()
    }
}
