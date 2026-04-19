package com.github.mishaguk.projecttrailer.toolWindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ui.JBColor
import java.awt.Color

class TourNodeDecorator : ProjectViewNodeDecorator {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val project = node.project ?: return
        if (project.isDisposed) return
        val file = node.virtualFile ?: return
        val registry = TourHighlightRegistry.getInstance(project)
        registry.indexOf(file) ?: return

        val background = if (registry.isCurrent(file)) CURRENT_BG else TOUR_BG
        data.background = background
    }

    companion object {
        private val TOUR_BG: Color = JBColor(Color(0xE8, 0xF5, 0xE9), Color(0x2B, 0x3A, 0x2E))
        private val CURRENT_BG: Color = JBColor(Color(0xC8, 0xE6, 0xC9), Color(0x3D, 0x55, 0x42))
    }
}
