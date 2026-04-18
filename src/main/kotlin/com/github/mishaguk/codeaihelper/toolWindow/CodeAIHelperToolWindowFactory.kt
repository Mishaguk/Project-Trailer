package com.github.mishaguk.codeaihelper.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.mishaguk.codeaihelper.CodeAIHelperBundle
import com.github.mishaguk.codeaihelper.ai.OpenAiClient
import com.github.mishaguk.codeaihelper.services.CodeAIHelperProjectService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import javax.swing.JButton


class CodeAIHelperToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = CodeAIHelperToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class CodeAIHelperToolWindow(toolWindow: ToolWindow) {

        private val project = toolWindow.project
        private val service = toolWindow.project.service<CodeAIHelperProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(CodeAIHelperBundle.message("random", "?"))


            add(label)
            add(JButton(CodeAIHelperBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = CodeAIHelperBundle.message("randomLabel", service.getRandomNumber())
                }
            })
            add(JButton(CodeAIHelperBundle.message("ai.test.button")).apply {
                addActionListener {
                    val button = this
                    button.isEnabled = false
                    ApplicationManager.getApplication().executeOnPooledThread {
                        val result = OpenAiClient.getInstance().testConnection()
                        ApplicationManager.getApplication().invokeLater {
                            button.isEnabled = true
                            val title = CodeAIHelperBundle.message("ai.test.title")
                            result.onSuccess {
                                Messages.showInfoMessage(
                                    project,
                                    CodeAIHelperBundle.message("ai.test.ok"),
                                    title,
                                )
                            }.onFailure { e ->
                                Messages.showErrorDialog(
                                    project,
                                    CodeAIHelperBundle.message("ai.test.failed", e.message ?: ""),
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
