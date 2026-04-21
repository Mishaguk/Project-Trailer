package com.github.mishaguk.projecttrailer.toolWindow.tour

import com.github.mishaguk.projecttrailer.ai.tour.TourStep
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.tree.TreeVisitor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Point
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

object TourBalloonPresenter {

    private const val CONTENT_WIDTH = 400
    private const val MAX_BOUNDS_POLLS = 30

    fun show(
        project: Project,
        step: TourStep,
        index: Int,
        total: Int,
        targetFile: VirtualFile?,
        onPrev: () -> Unit,
        onNext: () -> Unit,
        onClose: () -> Unit,
    ): AtomicReference<Balloon?> {
        val ref = AtomicReference<Balloon?>(null)
        if (project.isDisposed) return ref

        val pane = ProjectView.getInstance(project).currentProjectViewPane ?: return ref
        val tree = pane.tree ?: return ref

        if (targetFile == null) {
            showAt(ref, tree, null, step, index, total, onPrev, onNext, onClose)
            return ref
        }

        val visitor = TreeVisitor { path ->
            val file = extractVirtualFile(path)
            if (file == null) {
                TreeVisitor.Action.CONTINUE
            } else if (file == targetFile) {
                TreeVisitor.Action.INTERRUPT
            } else if (VfsUtilParentOf(file, targetFile)) {
                TreeVisitor.Action.CONTINUE
            } else {
                TreeVisitor.Action.SKIP_CHILDREN
            }
        }

        TreeUtil.promiseMakeVisible(tree, visitor).onProcessed { resolvedPath ->
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                val actualPath = resolvedPath ?: tree.selectionPath
                waitForBoundsAndShow(
                    ref,
                    tree,
                    actualPath,
                    step,
                    index,
                    total,
                    onPrev,
                    onNext,
                    onClose,
                    MAX_BOUNDS_POLLS,
                )
            }
        }
        return ref
    }

    private fun VfsUtilParentOf(maybeParent: VirtualFile, child: VirtualFile): Boolean {
        var cur: VirtualFile? = child
        while (cur != null) {
            if (cur == maybeParent) return true
            cur = cur.parent
        }
        return false
    }

    private fun waitForBoundsAndShow(
        ref: AtomicReference<Balloon?>,
        tree: JTree,
        path: TreePath?,
        step: TourStep,
        index: Int,
        total: Int,
        onPrev: () -> Unit,
        onNext: () -> Unit,
        onClose: () -> Unit,
        attemptsLeft: Int,
    ) {
        val bounds = path?.let { tree.getPathBounds(it) }
        if (bounds != null || path == null || attemptsLeft <= 0) {
            showAt(ref, tree, path, step, index, total, onPrev, onNext, onClose)
            return
        }
        tree.scrollPathToVisible(path)
        ApplicationManager.getApplication().invokeLater {
            waitForBoundsAndShow(ref, tree, path, step, index, total, onPrev, onNext, onClose, attemptsLeft - 1)
        }
    }

    private fun showAt(
        ref: AtomicReference<Balloon?>,
        tree: JTree,
        path: TreePath?,
        step: TourStep,
        index: Int,
        total: Int,
        onPrev: () -> Unit,
        onNext: () -> Unit,
        onClose: () -> Unit,
    ) {
        val content = buildContent(step, index, total, onPrev, onNext, onClose)
        val balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(content)
            .setHideOnClickOutside(false)
            .setHideOnKeyOutside(false)
            .setCloseButtonEnabled(false)
            .setFadeoutTime(0)
            .setBorderColor(BORDER_COLOR)
            .setFillColor(FILL_COLOR)
            .createBalloon()

        val bounds = path?.let { tree.getPathBounds(it) }
        val anchor: RelativePoint = if (bounds != null) {
            tree.scrollPathToVisible(path)
            RelativePoint(tree, Point(bounds.x + bounds.width, bounds.y + bounds.height / 2))
        } else {
            JBPopupFactory.getInstance().guessBestPopupLocation(tree)
        }

        balloon.show(anchor, Balloon.Position.atRight)
        ref.set(balloon)
    }

    private fun extractVirtualFile(path: TreePath): VirtualFile? {
        val last = path.lastPathComponent
        val userObject = (last as? DefaultMutableTreeNode)?.userObject ?: last
        return when (userObject) {
            is ProjectViewNode<*> -> userObject.virtualFile
            is NodeDescriptor<*> -> {
                val element = userObject.element
                (element as? ProjectViewNode<*>)?.virtualFile
                    ?: (element as? VirtualFile)
            }
            else -> null
        }
    }

    private fun buildContent(
        step: TourStep,
        index: Int,
        total: Int,
        onPrev: () -> Unit,
        onNext: () -> Unit,
        onClose: () -> Unit,
    ): JPanel {
        val html = buildString {
            append("<html><body style='font-family: sans-serif; line-height: 1.45;'>")
            append("<div style='font-size: 15px; font-weight: bold;'>")
            append(escape(step.title))
            append("</div>")
            append("<div style='color: #8a8a8a; font-size: 12px; margin-top: 3px;'>")
            append(escape(step.path))
            append("</div>")
            append("<div style='font-size: 14px; margin-top: 10px;'>")
            append(escape(step.explanation).replace("\n", "<br>"))
            append("</div>")
            append("</body></html>")
        }

        val body = JEditorPane("text/html", html).apply {
            isEditable = false
            isOpaque = false
            border = JBUI.Borders.empty()
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            putClientProperty("JEditorPane.w3cLengthUnits", true)
        }
        body.size = Dimension(CONTENT_WIDTH, Short.MAX_VALUE.toInt())
        val measuredHeight = body.preferredSize.height.coerceAtLeast(60)
        body.preferredSize = Dimension(CONTENT_WIDTH, measuredHeight)

        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply {
            isOpaque = false
            add(JBLabel("${index + 1} / $total").apply { border = JBUI.Borders.emptyRight(8) })
            add(JButton("Prev").apply {
                isEnabled = index > 0
                addActionListener { onPrev() }
            })
            add(JButton("Next").apply {
                isEnabled = index < total - 1
                addActionListener { onNext() }
            })
            add(JButton("Close").apply { addActionListener { onClose() } })
        }

        return JPanel(BorderLayout(0, 8)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(10, 12)
            add(body, BorderLayout.CENTER)
            add(buttons, BorderLayout.SOUTH)
        }
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private val BORDER_COLOR = JBColor(Color(0x49, 0x9C, 0x54), Color(0x5F, 0xB8, 0x6A))
    private val FILL_COLOR = JBColor(Color(0xE8, 0xF5, 0xE9), Color(0x2B, 0x3A, 0x2E))
}
