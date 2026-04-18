package com.github.mishaguk.codeaihelper.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.mishaguk.codeaihelper.CodeAIHelperBundle
import com.github.mishaguk.codeaihelper.services.CodeAIHelperProjectService
import javax.swing.JButton


class CodeAIHelperToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = CodeAIHelperToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class CodeAIHelperToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<CodeAIHelperProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(CodeAIHelperBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(CodeAIHelperBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = CodeAIHelperBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}
