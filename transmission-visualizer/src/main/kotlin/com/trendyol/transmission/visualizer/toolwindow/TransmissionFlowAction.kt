package com.trendyol.transmission.visualizer.toolwindow

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.trendyol.transmission.visualizer.detection.DetectedTransmission
import java.awt.event.MouseEvent

class TransmissionFlowAction(
    private val transmission: DetectedTransmission,
    private val type: String
) : GutterIconNavigationHandler<PsiElement> {

    override fun navigate(e: MouseEvent?, element: PsiElement?) {
        runCatching {
            element?.project?.let { project ->
                showTransmissionFlow(project, transmission, type)
            }
        }
    }

    private fun showTransmissionFlow(project: Project, transmission: DetectedTransmission, type: String) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID)
        
        if (toolWindow != null) {
            toolWindow.show()
            toolWindow.activate(null)
            
            val content = toolWindow.contentManager.contents.firstOrNull()
            val flowViewer = content?.component as? TransmissionFlowViewer
            
            if (flowViewer != null) {
                ApplicationManager.getApplication().invokeLater {
                    flowViewer.showTransmissionFlow(transmission, type, project)
                }
            }
        }
    }

    companion object {
        const val TOOL_WINDOW_ID = "Transmission Visualizer"
    }
}
