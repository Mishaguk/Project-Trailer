package com.github.mishaguk.projecttrailer.toolWindow

import com.github.mishaguk.projecttrailer.ProjectTrailerBundle
import com.github.mishaguk.projecttrailer.ai.chat.ChatService
import com.github.mishaguk.projecttrailer.ai.tour.TourService
import com.github.mishaguk.projecttrailer.toolWindow.IDE.guide.IdeGuidePanel
import com.github.mishaguk.projecttrailer.toolWindow.chat.ChatPanelBridge
import com.github.mishaguk.projecttrailer.toolWindow.chat.MessageBubble
import com.github.mishaguk.projecttrailer.toolWindow.chat.Role
import com.github.mishaguk.projecttrailer.toolWindow.tour.TourBalloonPresenter
import com.github.mishaguk.projecttrailer.toolWindow.tour.TourController
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.CompoundBorder
import javax.swing.border.MatteBorder

class ProjectTrailerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = ProjectTrailerToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class ProjectTrailerToolWindow(toolWindow: ToolWindow) {
        private val project = toolWindow.project

        fun getContent(): JComponent {
            val topPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(createTourBar())
                add(IdeGuidePanel(project).createPanel())
            }
            val root = JPanel(BorderLayout())
            root.add(topPanel, BorderLayout.NORTH)
            root.add(ChatPanel().component, BorderLayout.CENTER)
            return root
        }

        private fun createTourBar(): JPanel {
            val accentColor = JBColor(Color(0x28, 0xA7, 0x45), Color(0x3F, 0xB9, 0x50))
            val cardBg = JBColor(Color(0xF7, 0xFB, 0xF7), Color(0x2A, 0x2E, 0x2A))

            val btnStartTour = JButton(ProjectTrailerBundle.message("tour.start")).apply {
                font = JBFont.label().asBold()
                putClientProperty("JButton.buttonType", "roundRect")
            }
            val focusField = JBTextField().apply {
                emptyText.text = ProjectTrailerBundle.message("tour.focus.placeholder")
                font = JBFont.label()
            }
            val btnFocusTour = JButton(ProjectTrailerBundle.message("tour.focus.start")).apply {
                putClientProperty("JButton.buttonType", "roundRect")
            }

            var activeBalloonRef: java.util.concurrent.atomic.AtomicReference<Balloon?>? = null
            lateinit var controller: TourController
            controller = TourController(
                project = project,
                onStepChanged = { _, _, _ ->
                    activeBalloonRef?.get()?.hide()
                    activeBalloonRef = null
                },
                onTourClosed = {
                    activeBalloonRef?.get()?.hide()
                    activeBalloonRef = null
                    btnStartTour.isEnabled = true
                    btnFocusTour.isEnabled = true
                },
                onAfterSelect = { step, currentIndex, totalSteps, file ->
                    activeBalloonRef?.get()?.hide()
                    activeBalloonRef = TourBalloonPresenter.show(
                        project = project,
                        step = step,
                        index = currentIndex,
                        total = totalSteps,
                        targetFile = file,
                        onPrev = { controller.prev() },
                        onNext = { controller.next() },
                        onClose = { controller.close() },
                    )
                },
            )

            fun launchTour(focusQuery: String?) {
                btnStartTour.isEnabled = false
                btnFocusTour.isEnabled = false
                ApplicationManager.getApplication().executeOnPooledThread {
                    val result = TourService.getInstance(project).generate(focusQuery)
                    ApplicationManager.getApplication().invokeLater {
                        result.onSuccess { steps ->
                            if (steps.isNotEmpty()) {
                                controller.start(steps)
                            } else {
                                btnStartTour.isEnabled = true
                                btnFocusTour.isEnabled = true
                                Messages.showInfoMessage(project, "Tour is empty.", "Project Tour")
                            }
                        }.onFailure { e ->
                            btnStartTour.isEnabled = true
                            btnFocusTour.isEnabled = true
                            Messages.showErrorDialog(project, e.message ?: "Error", "Tour Error")
                        }
                    }
                }
            }

            btnStartTour.addActionListener { launchTour(null) }

            btnFocusTour.addActionListener {
                val query = focusField.text?.trim().orEmpty()
                if (query.isEmpty()) {
                    Messages.showWarningDialog(project, ProjectTrailerBundle.message("tour.focus.empty"), "Tour")
                    return@addActionListener
                }
                launchTour(query)
            }
            focusField.addActionListener { btnFocusTour.doClick() }

            val title = JLabel(ProjectTrailerBundle.message("tour.header.title")).apply {
                font = JBFont.label().biggerOn(3f).asBold()
                foreground = accentColor
                border = JBUI.Borders.empty(0, 0, 2, 0)
            }

            val subtitle = JLabel(ProjectTrailerBundle.message("tour.header.subtitle")).apply {
                font = JBFont.small()
                foreground = JBColor.GRAY
            }

            val headerRow = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(JPanel().apply {
                    isOpaque = false
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    add(title)
                    add(subtitle)
                }, BorderLayout.CENTER)
                add(btnStartTour, BorderLayout.EAST)
            }

            val divider = object : JPanel() {
                override fun getPreferredSize() = Dimension(super.getPreferredSize().width, 1)
                override fun getMaximumSize() = Dimension(Int.MAX_VALUE, 1)
            }.apply {
                background = JBColor.border()
            }

            val focusLabel = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(JLabel(ProjectTrailerBundle.message("tour.focus.label")).apply {
                    font = JBFont.small().asBold()
                    foreground = JBColor.GRAY
                    border = JBUI.Borders.empty(0, 0, 4, 0)
                }, BorderLayout.WEST)
            }

            val focusRow = JPanel(BorderLayout(6, 0)).apply {
                isOpaque = false
                add(focusField, BorderLayout.CENTER)
                add(btnFocusTour, BorderLayout.EAST)
            }

            val card = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = cardBg
                border = CompoundBorder(
                    JBUI.Borders.empty(8, 10, 6, 10),
                    CompoundBorder(
                        MatteBorder(0, 3, 0, 0, accentColor),
                        JBUI.Borders.empty(10, 12, 10, 12)
                    )
                )
                add(headerRow)
                add(Box.createVerticalStrut(8))
                add(divider)
                add(Box.createVerticalStrut(8))
                add(focusLabel)
                add(focusRow)
            }

            return JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(6, 0, 2, 0)
                add(card, BorderLayout.NORTH)
            }
        }

        private inner class ChatPanel {
            private val messages = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = JBUI.Borders.empty(8)
                background = JBColor.background()
            }
            private val scroll = JBScrollPane(wrap(messages)).apply {
                verticalScrollBar.unitIncrement = 16
                border = JBUI.Borders.empty()
            }
            private val input = JBTextField().apply { emptyText.text = ProjectTrailerBundle.message("chat.placeholder") }
            private val sendBtn = JButton(ProjectTrailerBundle.message("chat.send"))
            private val resetBtn = JButton(ProjectTrailerBundle.message("chat.reset"))

            val component: JComponent

            init {
                sendBtn.addActionListener { send() }
                input.addActionListener { send() }
                resetBtn.addActionListener { reset() }

                val south = JPanel(BorderLayout(6, 0)).apply {
                    border = JBUI.Borders.empty(6, 8)
                    add(resetBtn, BorderLayout.WEST)
                    add(input, BorderLayout.CENTER)
                    add(sendBtn, BorderLayout.EAST)
                }
                component = JPanel(BorderLayout()).apply {
                    add(scroll, BorderLayout.CENTER)
                    add(south, BorderLayout.SOUTH)
                }

                ChatPanelBridge.getInstance(project).onExplainRequest = { userLabel, question ->
                    submitQuestion(userLabel, question)
                }
            }

            fun submitQuestion(userLabel: String, question: String) {
                addBubble(Role.USER, userLabel)
                val thinkingBubble = addBubble(Role.ASSISTANT, ProjectTrailerBundle.message("chat.thinking"))
                setInputEnabled(false)

                ApplicationManager.getApplication().executeOnPooledThread {
                    val result = ChatService.getInstance(project).ask(question)
                    ApplicationManager.getApplication().invokeLater {
                        result.onSuccess { reply -> thinkingBubble.setText(reply) }
                            .onFailure { e -> thinkingBubble.setError(ProjectTrailerBundle.message("chat.error", e.message ?: "")) }
                        setInputEnabled(true)
                    }
                }
            }

            private fun wrap(content: JComponent): JComponent = JPanel(BorderLayout()).apply {
                add(content, BorderLayout.NORTH)
                background = JBColor.background()
            }

            private fun send() {
                val text = input.text?.trim().orEmpty()
                if (text.isEmpty()) return
                input.text = ""
                setInputEnabled(false)

                addBubble(Role.USER, text)
                val thinkingBubble = addBubble(Role.ASSISTANT, ProjectTrailerBundle.message("chat.thinking"))

                ApplicationManager.getApplication().executeOnPooledThread {
                    val result = ChatService.getInstance(project).ask(text)
                    ApplicationManager.getApplication().invokeLater {
                        result.onSuccess { reply -> thinkingBubble.setText(reply) }
                            .onFailure { e -> thinkingBubble.setError(ProjectTrailerBundle.message("chat.error", e.message ?: "")) }
                        setInputEnabled(true)
                        input.requestFocusInWindow()
                    }
                }
            }

            private fun reset() {
                ChatService.getInstance(project).reset()
                messages.removeAll()
                messages.revalidate()
                messages.repaint()
                addSystemLine(ProjectTrailerBundle.message("chat.resetDone"))
            }

            private fun setInputEnabled(enabled: Boolean) {
                input.isEnabled = enabled
                sendBtn.isEnabled = enabled
            }

            private fun addBubble(role: Role, text: String): MessageBubble {
                val bubble = MessageBubble(role, text)
                messages.add(bubble)
                messages.add(Box.createVerticalStrut(6))
                messages.revalidate()
                scrollToBottom()
                return bubble
            }

            private fun addSystemLine(text: String) {
                val label = JLabel(text, SwingConstants.CENTER).apply {
                    alignmentX = Component.CENTER_ALIGNMENT
                    foreground = JBColor.GRAY
                    font = JBFont.small()
                    border = JBUI.Borders.empty(4)
                }
                messages.add(label)
                messages.revalidate()
                scrollToBottom()
            }

            private fun scrollToBottom() {
                ApplicationManager.getApplication().invokeLater {
                    val bar = scroll.verticalScrollBar
                    bar.value = bar.maximum
                }
            }
        }

    }
}
