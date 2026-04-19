package com.github.mishaguk.projecttrailer.actions

import com.github.mishaguk.projecttrailer.ai.FileReader
import com.github.mishaguk.projecttrailer.toolWindow.ChatPanelBridge
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager

class ExplainWithAiAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val basePath = project.basePath ?: return

        val relativePath = file.path.removePrefix(basePath).trimStart('/', '\\')

        val tw = ToolWindowManager.getInstance(project).getToolWindow("Project Trailer")
        tw?.show()

        val bridge = ChatPanelBridge.getInstance(project)
        val question = buildQuestion(project, file, relativePath)
        bridge.submitExplainRequest(relativePath, question)
    }

    private fun buildQuestion(project: Project, file: VirtualFile, relativePath: String): String {
        if (file.isDirectory) {
            val children = file.children?.joinToString(", ") { if (it.isDirectory) "${it.name}/" else it.name } ?: ""
            return "Explain what the directory \"$relativePath\" is for in this project. " +
                "Its contents are: $children. " +
                "Describe its role, the responsibilities of its key files, and how it fits into the overall project architecture."
        }

        val content = FileReader.read(project, relativePath, null, null)
        return "Explain this file: \"$relativePath\".\n\nFile content:\n$content\n\n" +
            "Describe what this file does, its role in the project, key classes/functions, and any important design decisions."
    }
}
