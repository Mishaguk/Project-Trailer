package com.github.mishaguk.projecttrailer.toolWindow.chat

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.border.CompoundBorder
import javax.swing.border.MatteBorder

class MessageBubble(role: Role, text: String) : JPanel(BorderLayout()) {
    private val body: JTextArea

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        background = role.background
        border = CompoundBorder(MatteBorder(0, 3, 0, 0, role.accent), JBUI.Borders.empty(8, 10))

        val header = JLabel(role.label).apply {
            font = JBFont.label().asBold()
            foreground = role.accent
            border = JBUI.Borders.emptyBottom(4)
        }


        body = JTextArea(text).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            background = role.background
            border = JBUI.Borders.empty()
            font = JBFont.label()
        }

        header.alignmentX = Component.LEFT_ALIGNMENT
        header.maximumSize = Dimension(Int.MAX_VALUE, header.preferredSize.height)
        header.horizontalAlignment = JLabel.LEFT
        body.alignmentX = Component.LEFT_ALIGNMENT

        val column = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(header)
            add(body)
        }
        add(column, BorderLayout.CENTER)
        maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE / 2)
    }

    fun setText(text: String) {
        body.text = text
        revalidate()
        repaint()
    }

    fun setError(text: String) {
        body.text = text
        body.foreground = JBColor(Color(0xB4, 0x21, 0x2A), Color(0xF8, 0x51, 0x49))
        revalidate()
        repaint()
    }
}
