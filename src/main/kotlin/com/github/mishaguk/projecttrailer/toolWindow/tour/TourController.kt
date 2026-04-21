package com.github.mishaguk.projecttrailer.toolWindow.tour

import com.github.mishaguk.projecttrailer.ai.tour.TourStep
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.nio.file.Path

class TourController(
    private val project: Project,
    private val onStepChanged: (step: TourStep, currentIndex: Int, totalSteps: Int) -> Unit,
    private val onTourClosed: () -> Unit,
    private val onAfterSelect: (step: TourStep, currentIndex: Int, totalSteps: Int, file: VirtualFile?) -> Unit = { _, _, _, _ -> },
) {
    private var steps: List<TourStep> = emptyList()
    private var resolvedFiles: List<VirtualFile?> = emptyList()
    private var currentIndex: Int = -1

    fun start(newSteps: List<TourStep>) {
        if (newSteps.isEmpty()) return
        steps = newSteps
        resolvedFiles = newSteps.map { resolve(it) }
        currentIndex = 0
        pushRegistry()
        renderCurrentStep()
    }

    fun next() {
        if (hasNext()) {
            currentIndex++
            pushRegistry()
            renderCurrentStep()
        }
    }

    fun prev() {
        if (hasPrev()) {
            currentIndex--
            pushRegistry()
            renderCurrentStep()
        }
    }

    fun close() {
        steps = emptyList()
        resolvedFiles = emptyList()
        currentIndex = -1
        TourHighlightRegistry.getInstance(project).clear()
        onTourClosed()
    }

    fun hasNext(): Boolean = currentIndex < steps.size - 1
    fun hasPrev(): Boolean = currentIndex > 0

    private fun resolve(step: TourStep): VirtualFile? {
        val basePath = project.basePath ?: return null
        val absolute = Path.of(basePath, step.path.trimStart('/', '\\')).toString().replace('\\', '/')
        return LocalFileSystem.getInstance().findFileByPath(absolute)
    }

    private fun pushRegistry() {
        val files = resolvedFiles.filterNotNull()
        val current = resolvedFiles.getOrNull(currentIndex)
        TourHighlightRegistry.getInstance(project).set(files, current)
    }

    private fun renderCurrentStep() {
        if (currentIndex !in steps.indices) return
        val step = steps[currentIndex]

        onStepChanged(step, currentIndex, steps.size)

        ApplicationManager.getApplication().invokeLater {
            val virtualFile = resolvedFiles.getOrNull(currentIndex) ?: return@invokeLater

            val psiManager = PsiManager.getInstance(project)
            val psiElement = if (virtualFile.isDirectory) {
                psiManager.findDirectory(virtualFile)
            } else {
                psiManager.findFile(virtualFile)
            } ?: return@invokeLater

            ProjectView.getInstance(project).selectPsiElement(psiElement, false)

            if (!virtualFile.isDirectory) {
                FileEditorManager.getInstance(project).openFile(virtualFile, false)
            }

            ApplicationManager.getApplication().invokeLater {
                if (currentIndex in steps.indices) {
                    onAfterSelect(step, currentIndex, steps.size, virtualFile)
                }
            }
        }
    }
}
