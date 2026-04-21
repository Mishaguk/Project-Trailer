package com.github.mishaguk.projecttrailer.toolWindow.IDE.guide

import com.github.mishaguk.projecttrailer.ProjectTrailerBundle
import com.github.mishaguk.projecttrailer.ai.IDE.Guide.IdeGuideService
import com.github.mishaguk.projecttrailer.ai.IDE.Guide.IdeGuideStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.swing.border.CompoundBorder
import javax.swing.border.MatteBorder

class IdeGuidePanel(private val project: Project) {

    fun createPanel(): JPanel {
        val accentColor = JBColor(Color(0x34, 0x78, 0xF6), Color(0x58, 0xA6, 0xFF))
        val cardBg = JBColor(Color(0xEE, 0xF4, 0xFF), Color(0x2C, 0x33, 0x42))

        val guideInput = JBTextField().apply {
            emptyText.text = ProjectTrailerBundle.message("guide.placeholder")
            font = JBFont.label()
        }
        val btnAsk = JButton(ProjectTrailerBundle.message("guide.ask")).apply {
            putClientProperty("JButton.buttonType", "roundRect")
        }

        val stepsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            isVisible = false
        }

        fun showSteps(steps: List<IdeGuideStep>) {
            stepsPanel.removeAll()
            steps.forEachIndexed { index, step ->
                stepsPanel.add(createStepCard(index + 1, step, accentColor))
                stepsPanel.add(Box.createVerticalStrut(6))
            }
            stepsPanel.isVisible = true
            stepsPanel.revalidate()
            stepsPanel.repaint()
        }

        fun askGuide() {
            val question = guideInput.text?.trim().orEmpty()
            if (question.isEmpty()) return
            guideInput.isEnabled = false
            btnAsk.isEnabled = false
            stepsPanel.removeAll()
            stepsPanel.isVisible = true
            val loadingLabel = JLabel(ProjectTrailerBundle.message("guide.loading")).apply {
                foreground = JBColor.GRAY
                font = JBFont.small()
                border = JBUI.Borders.empty(4)
            }
            stepsPanel.add(loadingLabel)
            stepsPanel.revalidate()

            ApplicationManager.getApplication().executeOnPooledThread {
                val result = IdeGuideService.getInstance(project).generate(question)
                ApplicationManager.getApplication().invokeLater {
                    result.onSuccess { steps -> showSteps(steps) }
                        .onFailure { e ->
                            stepsPanel.removeAll()
                            stepsPanel.add(JLabel(ProjectTrailerBundle.message("guide.error", e.message ?: "")).apply {
                                foreground = JBColor(Color(0xB4, 0x21, 0x2A), Color(0xF8, 0x51, 0x49))
                                font = JBFont.small()
                                border = JBUI.Borders.empty(4)
                            })
                            stepsPanel.revalidate()
                        }
                    guideInput.isEnabled = true
                    btnAsk.isEnabled = true
                }
            }
        }

        btnAsk.addActionListener { askGuide() }
        guideInput.addActionListener { askGuide() }

        val title = JLabel(ProjectTrailerBundle.message("guide.header.title")).apply {
            font = JBFont.label().biggerOn(3f).asBold()
            foreground = accentColor
            border = JBUI.Borders.empty(0, 0, 2, 0)
        }

        val subtitle = JLabel(ProjectTrailerBundle.message("guide.header.subtitle")).apply {
            font = JBFont.small()
            foreground = JBColor.GRAY
        }

        val headerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JPanel().apply {
                isOpaque = false
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(title)
                add(subtitle)
            }, BorderLayout.CENTER)
        }

        val inputRow = JPanel(BorderLayout(6, 0)).apply {
            isOpaque = false
            add(guideInput, BorderLayout.CENTER)
            add(btnAsk, BorderLayout.EAST)
        }

        val card = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = cardBg
            border = CompoundBorder(
                JBUI.Borders.empty(4, 10, 6, 10),
                CompoundBorder(
                    MatteBorder(0, 3, 0, 0, accentColor),
                    JBUI.Borders.empty(10, 12, 10, 12)
                )
            )
            add(headerPanel)
            add(Box.createVerticalStrut(8))
            add(inputRow)
            add(Box.createVerticalStrut(6))
            add(stepsPanel)
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 0, 6, 0)
            add(card, BorderLayout.NORTH)
        }
    }

    private fun createStepCard(number: Int, step: IdeGuideStep, accentColor: JBColor): JPanel {
        val stepBg = JBColor(Color(0xF8, 0xFA, 0xFF), Color(0x2E, 0x32, 0x3A))

        val badge = JLabel("  $number  ").apply {
            isOpaque = true
            background = accentColor
            foreground = JBColor.WHITE
            font = JBFont.small().asBold()
            horizontalAlignment = SwingConstants.CENTER
        }

        val titleLabel = JLabel(step.title).apply {
            font = JBFont.label().asBold()
            border = JBUI.Borders.emptyLeft(8)
        }

        val topRow = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            add(badge)
            add(titleLabel)
        }

        val instructionArea = JTextArea(step.instruction).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            background = stepBg
            border = JBUI.Borders.empty(4, 0, 0, 0)
            font = JBFont.label()
        }

        val card = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = stepBg
            border = JBUI.Borders.empty(8, 10, 8, 10)
            add(topRow)
            add(instructionArea)
        }

        if (step.actionId != null) {
            val actionBtn = JButton(step.actionLabel ?: ProjectTrailerBundle.message("guide.doIt")).apply {
                putClientProperty("JButton.buttonType", "roundRect")
                font = JBFont.small().asBold()
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addActionListener {
                    val success = IdeGuideExecutor.execute(project, step.actionId)
                    if (!success) {
                        Messages.showErrorDialog(
                            project,
                            ProjectTrailerBundle.message("guide.actionNotFound", step.actionId),
                            "IDE Guide"
                        )
                    }
                }
            }
            val btnRow = JPanel(FlowLayout(FlowLayout.LEFT, 0, 4)).apply {
                isOpaque = false
                add(actionBtn)
            }
            card.add(btnRow)
        }

        card.maximumSize = Dimension(Int.MAX_VALUE, card.preferredSize.height + 200)
        return card
    }
}
