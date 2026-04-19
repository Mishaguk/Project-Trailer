package com.github.mishaguk.projecttrailer.toolWindow

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class TourHighlightRegistry(private val project: Project) {

    @Volatile
    private var order: List<VirtualFile> = emptyList()

    @Volatile
    private var current: VirtualFile? = null

    fun set(files: List<VirtualFile>, current: VirtualFile?) {
        this.order = files
        this.current = current
        refreshProjectView()
    }

    fun clear() {
        this.order = emptyList()
        this.current = null
        refreshProjectView()
    }

    fun indexOf(file: VirtualFile): Int? {
        val i = order.indexOf(file)
        return if (i >= 0) i else null
    }

    fun isCurrent(file: VirtualFile): Boolean = current == file

    private fun refreshProjectView() {
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                ProjectView.getInstance(project).refresh()
            }
        }
    }

    companion object {
        fun getInstance(project: Project): TourHighlightRegistry = project.service()
    }
}
