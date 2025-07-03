package com.trendyol.transmission.visualizer.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class TransmissionFlowToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        runCatching {
            val flowViewer = TransmissionFlowViewer()
            val contentFactory = ContentFactory.getInstance()
            val content = contentFactory.createContent(flowViewer, "", false)
            val contentManager = toolWindow.contentManager

            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
        }
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    override fun init(toolWindow: ToolWindow) {
        super.init(toolWindow)
        toolWindow.stripeTitle = "Transmission Visualizer"
    }
}
