package com.github.mishaguk.projecttrailer.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.mishaguk.projecttrailer.ProjectTrailerBundle
import com.github.mishaguk.projecttrailer.ai.OpenAiClient
import com.github.mishaguk.projecttrailer.services.ProjectTrailerProjectService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import javax.swing.JButton


class ProjectTrailerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = ProjectTrailerToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class ProjectTrailerToolWindow(toolWindow: ToolWindow) {

        private val project = toolWindow.project
        private val service = toolWindow.project.service<ProjectTrailerProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(ProjectTrailerBundle.message("random", "?"))


            add(label)
            add(JButton(ProjectTrailerBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = ProjectTrailerBundle.message("randomLabel", service.getRandomNumber())
                }
            })
            add(JButton(ProjectTrailerBundle.message("ai.test.button")).apply {
                addActionListener {
                    val button = this
                    button.isEnabled = false
                    ApplicationManager.getApplication().executeOnPooledThread {
                        val result = OpenAiClient.getInstance().testConnection()
                        ApplicationManager.getApplication().invokeLater {
                            button.isEnabled = true
                            val title = ProjectTrailerBundle.message("ai.test.title")
                            result.onSuccess {
                                Messages.showInfoMessage(
                                    project,
                                    ProjectTrailerBundle.message("ai.test.ok"),
                                    title,
                                )
                            }.onFailure { e ->
                                Messages.showErrorDialog(
                                    project,
                                    ProjectTrailerBundle.message("ai.test.failed", e.message ?: ""),
                                    title,
                                )
                            }
                        }
                    }
                }
            })
        }
    }
}
