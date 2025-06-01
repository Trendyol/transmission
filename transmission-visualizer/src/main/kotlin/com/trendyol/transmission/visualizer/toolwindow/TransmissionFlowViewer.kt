package com.trendyol.transmission.visualizer.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.trendyol.transmission.visualizer.analysis.FlowAnalyzer
import com.trendyol.transmission.visualizer.analysis.TransmissionUsage
import com.trendyol.transmission.visualizer.analysis.UsageContext
import com.trendyol.transmission.visualizer.detection.DetectedTransmission
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer

class TransmissionFlowViewer : JPanel() {

    private val titleLabel = JLabel("Transmission Visualizer")
    private val statusLabel = JLabel("Click on a transmission icon to see the flow")
    private var flowTree: Tree? = null
    private var currentProject: Project? = null

    init {
        initializeUI()
    }

    private fun initializeUI() {
        layout = BorderLayout()
        preferredSize = Dimension(300, 400)

        val headerPanel = JPanel()
        headerPanel.add(titleLabel)

        val statusPanel = JPanel()
        statusPanel.add(statusLabel)

        add(headerPanel, BorderLayout.NORTH)
        add(statusPanel, BorderLayout.SOUTH)

        add(createEmptyPanel(), BorderLayout.CENTER)
    }

    private fun createEmptyPanel(): JPanel {
        val panel = JPanel()
        panel.add(JLabel("Select a transmission to view its flow"))
        return panel
    }

    fun showTransmissionFlow(transmission: DetectedTransmission, type: String, project: Project) {
        runCatching {
            currentProject = project
            statusLabel.text = "Analyzing flow for $type: ${transmission.name}..."

            ApplicationManager.getApplication().executeOnPooledThread {
                performFlowAnalysis(transmission, type, project)
            }
        }.onFailure {
            statusLabel.text = "Error analyzing flow"
        }
    }

    private fun performFlowAnalysis(
        transmission: DetectedTransmission,
        type: String,
        project: Project
    ) {
        runCatching {
            val analyzer = FlowAnalyzer(project)
            val usages = analyzer.analyzeTransmissionFlow(transmission)

            SwingUtilities.invokeLater {
                updateFlowDisplay(transmission, type, usages)
            }
        }.onFailure {
            SwingUtilities.invokeLater {
                statusLabel.text = "Failed to analyze flow"
            }
        }
    }

    private fun updateFlowDisplay(
        transmission: DetectedTransmission,
        type: String,
        usages: List<TransmissionUsage>
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater {
                updateFlowDisplay(transmission, type, usages)
            }
            return
        }

        remove(getComponent(1)) // Remove center component

        if (usages.isEmpty()) {
            val fallbackPanel = createFallbackPanel(transmission, type)
            add(JBScrollPane(fallbackPanel), BorderLayout.CENTER)
            statusLabel.text =
                "Transmission selected: ${transmission.name} (no external usages found)"
        } else {
            // Create flow tree
            val flowTreeComponent = createFlowTree(transmission, type, usages)
            add(JBScrollPane(flowTreeComponent), BorderLayout.CENTER)
            statusLabel.text = "Found ${usages.size} usage(s) for ${transmission.name}"
        }

        revalidate()
        repaint()
    }

    private fun createFlowTree(
        transmission: DetectedTransmission,
        type: String,
        usages: List<TransmissionUsage>
    ): Tree {
        val rootNode = DefaultMutableTreeNode("$type: ${transmission.name}")
        val groupedUsages = usages.groupBy { it.context }
        val contextOrder = listOf(
            UsageContext.CREATION,
            UsageContext.ROUTING,
            UsageContext.PROCESSING,
            UsageContext.PUBLICATION,
            UsageContext.CONSUMPTION
        )

        for (context in contextOrder) {
            val contextUsages = groupedUsages[context] ?: continue
            val contextNode = DefaultMutableTreeNode(getContextDisplayName(context))

            for (usage in contextUsages) {
                val usageNode = DefaultMutableTreeNode(FlowUsageNode(usage))
                contextNode.add(usageNode)
            }
            rootNode.add(contextNode)
        }

        val treeModel = DefaultTreeModel(rootNode)
        val tree = Tree(treeModel)

        tree.cellRenderer = FlowTreeCellRenderer()
        tree.isRootVisible = true
        tree.showsRootHandles = true
        TreeUIHelper.getInstance().installTreeSpeedSearch(tree)

        tree.addTreeSelectionListener { event ->
            val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            selectedNode?.userObject?.let { userObject ->
                if (userObject is FlowUsageNode) {
                    navigateToUsage(userObject.usage)
                }
            }
        }

        expandAllNodes(tree, rootNode)

        flowTree = tree
        return tree
    }

    private fun expandAllNodes(tree: Tree, node: DefaultMutableTreeNode) {
        // First expand this node
        val path = javax.swing.tree.TreePath(node.path)
        tree.expandPath(path)

        // Then recursively expand all child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChildAt(i) as DefaultMutableTreeNode
            expandAllNodes(tree, childNode)
        }
    }

    private fun navigateToUsage(usage: TransmissionUsage) {
        runCatching {
            val project = currentProject ?: return
            val virtualFile = usage.element.containingFile?.virtualFile

            if (virtualFile != null && virtualFile.exists()) {
                ApplicationManager.getApplication().invokeLater {
                    val descriptor =
                        OpenFileDescriptor(project, virtualFile, usage.lineNumber - 1, 0)
                    val fileEditorManager = FileEditorManager.getInstance(project)
                    fileEditorManager.openTextEditor(descriptor, true)
                }
            } else {
                statusLabel.text = "File not found: ${usage.fileName}"
            }
        }.onFailure {
            statusLabel.text = "Error opening file: ${usage.fileName}"
        }
    }

    private fun createFallbackPanel(transmission: DetectedTransmission, type: String): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)

        val titleLabel = JLabel("$type: ${transmission.name}")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
        titleLabel.alignmentX = Component.CENTER_ALIGNMENT

        val infoLabel = JLabel("Line: ${transmission.lineNumber}")
        infoLabel.alignmentX = Component.CENTER_ALIGNMENT

        val messageLabel =
            JLabel("<html><center>This transmission is defined but no external usages were found.<br/>This could mean:<br/>• It's used within the same file<br/>• It's not yet implemented<br/>• The usage analysis needs refinement</center></html>")
        messageLabel.alignmentX = Component.CENTER_ALIGNMENT

        panel.add(titleLabel)
        panel.add(Box.createVerticalStrut(10))
        panel.add(infoLabel)
        panel.add(Box.createVerticalStrut(20))
        panel.add(messageLabel)
        panel.add(Box.createVerticalGlue())

        return panel
    }

    private fun getContextDisplayName(context: UsageContext): String {
        return when (context) {
            UsageContext.CREATION -> "📝 Created"
            UsageContext.ROUTING -> "🔄 Routed"
            UsageContext.PROCESSING -> "⚙️ Processed"
            UsageContext.PUBLICATION -> "📤 Published"
            UsageContext.CONSUMPTION -> "📥 Consumed"
        }
    }

    data class FlowUsageNode(val usage: TransmissionUsage) {
        override fun toString(): String {
            val className = if (!usage.className.isNullOrEmpty()) " in ${usage.className}" else ""
            return "${usage.fileName}:${usage.lineNumber}$className"
        }
    }

    private class FlowTreeCellRenderer : TreeCellRenderer {
        private val defaultRenderer = JLabel()

        override fun getTreeCellRendererComponent(
            tree: JTree?,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {

            val node = value as? DefaultMutableTreeNode
            val userObject = node?.userObject

            defaultRenderer.text = when (userObject) {
                is FlowUsageNode -> {
                    val usage = userObject.usage
                    "${usage.fileName}:${usage.lineNumber} ${if (!usage.className.isNullOrEmpty()) "in ${usage.className}" else ""}"
                }

                else -> userObject?.toString() ?: ""
            }

            defaultRenderer.isOpaque = selected

            val text = userObject?.toString() ?: ""
            when {
                text.contains("📝") || text.contains("🔄") ||
                        text.contains("⚙️") || text.contains("📤") || text.contains("📥") -> {
                    defaultRenderer.font = defaultRenderer.font.deriveFont(Font.BOLD)
                    defaultRenderer.toolTipText = null
                }
                userObject is FlowUsageNode -> {
                    defaultRenderer.font = defaultRenderer.font.deriveFont(Font.PLAIN)
                    defaultRenderer.toolTipText =
                        "Click to navigate to ${userObject.usage.fileName}:${userObject.usage.lineNumber}"
                }
                else -> {
                    defaultRenderer.font = defaultRenderer.font.deriveFont(Font.PLAIN)
                    defaultRenderer.toolTipText = null
                }
            }

            return defaultRenderer
        }
    }
}
