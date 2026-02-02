/**
 * FlowchartLayout - Layout engine for flowchart diagrams.
 */
package io.github.lugf027.mermaid.layout.flowchart

import io.github.lugf027.mermaid.layout.LayoutConfig
import io.github.lugf027.mermaid.layout.LayoutEngine
import io.github.lugf027.mermaid.layout.TextMeasureProvider
import io.github.lugf027.mermaid.layout.TextMeasurer
import io.github.lugf027.mermaid.layout.TextSize
import io.github.lugf027.mermaid.model.Bounds
import io.github.lugf027.mermaid.model.Direction
import io.github.lugf027.mermaid.model.flowchart.*

/**
 * Layout engine for flowchart diagrams.
 * 
 * Uses a simplified hierarchical layout algorithm:
 * 1. Build a graph from edges
 * 2. Assign nodes to ranks (layers)
 * 3. Order nodes within ranks
 * 4. Calculate positions
 * 5. Route edges
 */
public object FlowchartLayout : LayoutEngine<FlowchartData> {

    override fun layout(data: FlowchartData, config: LayoutConfig): FlowchartData {
        val layouter = FlowchartLayouter(data, config, textMeasureProvider = null, fontSize = 14f)
        return layouter.layout()
    }
    
    /**
     * Layout with precise text measurement using TextMeasureProvider.
     * 
     * @param data The flowchart data to layout
     * @param config Layout configuration
     * @param textMeasureProvider Provider for precise text measurement (e.g., Compose TextMeasurer wrapper)
     * @param fontSize Font size for text measurement (in sp)
     * @return The updated flowchart data with positions calculated
     */
    public fun layout(
        data: FlowchartData, 
        config: LayoutConfig,
        textMeasureProvider: TextMeasureProvider?,
        fontSize: Float
    ): FlowchartData {
        val layouter = FlowchartLayouter(data, config, textMeasureProvider, fontSize)
        return layouter.layout()
    }
}

/**
 * Internal implementation of flowchart layout.
 * 
 * @param data The flowchart data to layout
 * @param config Layout configuration
 * @param textMeasureProvider Optional provider for precise text measurement
 * @param fontSize Font size for text measurement (in sp)
 */
internal class FlowchartLayouter(
    private val data: FlowchartData,
    private val config: LayoutConfig,
    private val textMeasureProvider: TextMeasureProvider?,
    private val fontSize: Float
) {
    // Node ID -> rank (layer)
    private val nodeRanks = mutableMapOf<String, Int>()
    
    // Rank -> list of node IDs in order
    private val rankNodes = mutableMapOf<Int, MutableList<String>>()
    
    // Adjacency lists
    private val outEdges = mutableMapOf<String, MutableList<String>>()
    private val inEdges = mutableMapOf<String, MutableList<String>>()
    
    /**
     * Connection direction for edge port allocation.
     */
    private enum class ConnectionDirection {
        TOP, BOTTOM, LEFT, RIGHT
    }
    
    /**
     * Port allocator for assigning different connection points to multiple edges on the same node.
     * This prevents edges from overlapping at the same connection point.
     */
    private class EdgePortAllocator {
        // nodeId -> direction -> list of edge indices connecting in that direction
        private val nodePortAssignments = mutableMapOf<String, MutableMap<ConnectionDirection, MutableList<Int>>>()
        
        /**
         * Register an edge connection at a specific node and direction.
         * @return the port index for this edge (0-based)
         */
        fun registerPort(nodeId: String, direction: ConnectionDirection, edgeIndex: Int): Int {
            val directionPorts = nodePortAssignments
                .getOrPut(nodeId) { mutableMapOf() }
                .getOrPut(direction) { mutableListOf() }
            val portIndex = directionPorts.size
            directionPorts.add(edgeIndex)
            return portIndex
        }
        
        /**
         * Get the total number of ports used in a specific direction for a node.
         */
        fun getPortCount(nodeId: String, direction: ConnectionDirection): Int {
            return nodePortAssignments[nodeId]?.get(direction)?.size ?: 0
        }
        
        /**
         * Calculate the offset for a specific port on a node's edge.
         * @param nodeId the node ID
         * @param direction the connection direction
         * @param portIndex the port index (0-based)
         * @param edgeLength the length of the edge where ports are distributed
         * @return the offset from the center of the edge (-edgeLength/2 to +edgeLength/2)
         */
        fun calculatePortOffset(
            nodeId: String, 
            direction: ConnectionDirection, 
            portIndex: Int,
            edgeLength: Float
        ): Float {
            val portCount = getPortCount(nodeId, direction)
            if (portCount <= 1) return 0f
            
            // Distribute ports evenly along the edge, with some margin at the ends
            val usableLength = edgeLength * 0.7f  // Use 70% of the edge to leave margins
            val spacing = usableLength / (portCount - 1)
            val startOffset = -usableLength / 2
            
            return startOffset + portIndex * spacing
        }
    }
    
    // Port allocator instance for managing edge connections
    private val portAllocator = EdgePortAllocator()

    fun layout(): FlowchartData {
        if (data.vertices.isEmpty()) {
            return data
        }

        // Step 1: Build adjacency lists
        buildGraph()

        // Step 2: Assign ranks
        assignRanks()

        // Step 3: Order nodes within ranks
        orderNodesInRanks()

        // Step 4: Calculate node sizes
        calculateNodeSizes()

        // Step 5: Calculate positions
        calculatePositions()
        
        // Step 5.5: Apply subgraph title offset to nodes inside subgraphs
        applySubgraphTitleOffset()
        
        // Step 5.6: Align single-child nodes with their parents
        alignSingleChildNodes()

        // Step 6: Route edges
        routeEdges()

        // Step 7: Layout subgraphs (calculate bounds including title)
        layoutSubgraphs()

        // Step 8: Calculate overall bounds
        calculateBounds()

        return data
    }
    
    /**
     * Apply vertical offset to nodes inside subgraphs to make room for the subgraph title.
     * This ensures that nodes don't overlap with the subgraph title text.
     */
    private fun applySubgraphTitleOffset() {
        val isHorizontal = data.direction.isHorizontal
        
        // Calculate the total title offset (top margin + title height + bottom margin)
        val titleOffset = config.subgraphTitleTopMargin + config.subgraphTitleHeight + config.subgraphTitleBottomMargin
        
        // Process each subgraph and offset its contained nodes
        for (subgraph in data.subgraphs.values) {
            // Skip subgraphs without titles
            if (subgraph.title.isNullOrEmpty()) continue
            
            // Offset all vertices in this subgraph
            for (vertexId in subgraph.vertexIds) {
                val vertex = data.vertices[vertexId] ?: continue
                
                // Apply offset based on layout direction
                if (isHorizontal) {
                    // For horizontal layout (LR/RL), offset in X direction
                    vertex.bounds = vertex.bounds.copy(x = vertex.bounds.x + titleOffset)
                } else {
                    // For vertical layout (TB/BT), offset in Y direction (down)
                    vertex.bounds = vertex.bounds.copy(y = vertex.bounds.y + titleOffset)
                }
            }
            
            // Also offset nested subgraph bounds (they will be recalculated later, but we need to offset their nodes too)
            for (nestedSubgraphId in subgraph.subgraphIds) {
                val nestedSubgraph = data.subgraphs[nestedSubgraphId] ?: continue
                for (vertexId in nestedSubgraph.vertexIds) {
                    val vertex = data.vertices[vertexId] ?: continue
                    
                    if (isHorizontal) {
                        vertex.bounds = vertex.bounds.copy(x = vertex.bounds.x + titleOffset)
                    } else {
                        vertex.bounds = vertex.bounds.copy(y = vertex.bounds.y + titleOffset)
                    }
                }
            }
        }
    }
    
    /**
     * Align nodes that have a single parent to be directly below/beside their parent.
     * This improves visual alignment for chains like C -> E where E should be under C.
     */
    private fun alignSingleChildNodes() {
        val isHorizontal = data.direction.isHorizontal
        
        // Find nodes with single parent (single incoming non-back edge)
        for (vertex in data.vertices.values) {
            val nonBackInEdges = (inEdges[vertex.id] ?: emptyList()).filter { sourceId ->
                !backEdges.contains(sourceId to vertex.id)
            }
            
            // Only process nodes with exactly one parent
            if (nonBackInEdges.size != 1) continue
            
            val parentId = nonBackInEdges.first()
            val parent = data.vertices[parentId] ?: continue
            
            // Check if parent is in the previous rank
            val parentRank = nodeRanks[parentId] ?: continue
            val vertexRank = nodeRanks[vertex.id] ?: continue
            
            // Only align if parent is exactly one rank above
            if (vertexRank != parentRank + 1) continue
            
            // Check if this node has siblings (other children of the same parent)
            val siblings = (outEdges[parentId] ?: emptyList()).filter { targetId ->
                !backEdges.contains(parentId to targetId) && 
                nodeRanks[targetId] == vertexRank
            }
            
            // Only align if this is the only child in this rank from this parent
            // or if there's only one sibling and we're already close to alignment
            if (siblings.size > 1) continue
            
            // Align this node with its parent
            val newBounds = if (isHorizontal) {
                // For horizontal layout, align Y coordinate
                vertex.bounds.copy(y = parent.bounds.y + (parent.bounds.height - vertex.bounds.height) / 2)
            } else {
                // For vertical layout, align X coordinate (center under parent)
                vertex.bounds.copy(x = parent.bounds.x + (parent.bounds.width - vertex.bounds.width) / 2)
            }
            
            // Check for collision with other nodes in the same rank
            val rankNodes = rankNodes[vertexRank] ?: continue
            var hasCollision = false
            
            for (otherNodeId in rankNodes) {
                if (otherNodeId == vertex.id) continue
                val otherNode = data.vertices[otherNodeId] ?: continue
                
                // Check for overlap
                val overlap = if (isHorizontal) {
                    val otherTop = otherNode.bounds.y
                    val otherBottom = otherNode.bounds.bottom
                    val newTop = newBounds.y
                    val newBottom = newBounds.bottom
                    !(newBottom < otherTop - config.nodeSpacingY || newTop > otherBottom + config.nodeSpacingY)
                } else {
                    val otherLeft = otherNode.bounds.x
                    val otherRight = otherNode.bounds.right
                    val newLeft = newBounds.x
                    val newRight = newBounds.right
                    !(newRight < otherLeft - config.nodeSpacingX || newLeft > otherRight + config.nodeSpacingX)
                }
                
                if (overlap) {
                    hasCollision = true
                    break
                }
            }
            
            // Only apply alignment if there's no collision
            if (!hasCollision) {
                vertex.bounds = newBounds
            }
        }
    }

    private fun buildGraph() {
        // Initialize adjacency lists
        for (nodeId in data.vertices.keys) {
            outEdges[nodeId] = mutableListOf()
            inEdges[nodeId] = mutableListOf()
        }

        // Build adjacency lists from edges
        for (edge in data.edges) {
            outEdges.getOrPut(edge.sourceId) { mutableListOf() }.add(edge.targetId)
            inEdges.getOrPut(edge.targetId) { mutableListOf() }.add(edge.sourceId)
        }
    }

    // Set to track back edges (edges that point to an ancestor in DFS tree)
    private val backEdges = mutableSetOf<Pair<String, String>>()

    private fun assignRanks() {
        // Use Longest Path algorithm with back edge detection
        // This handles cycles correctly by identifying and ignoring back edges
        
        // Step 1: Detect back edges using DFS
        detectBackEdges()
        
        // Step 2: Build a DAG by ignoring back edges, then use longest path algorithm
        // Initialize all nodes with rank -1 (unvisited)
        for (nodeId in data.vertices.keys) {
            nodeRanks[nodeId] = -1
        }
        
        // Find source nodes (nodes with no incoming non-back edges)
        val sources = mutableListOf<String>()
        for (nodeId in data.vertices.keys) {
            val nonBackInEdges = (inEdges[nodeId] ?: emptyList()).filter { sourceId ->
                !backEdges.contains(sourceId to nodeId)
            }
            if (nonBackInEdges.isEmpty()) {
                sources.add(nodeId)
            }
        }
        
        // If no sources found (all nodes are in cycles), pick the first node
        if (sources.isEmpty() && data.vertices.isNotEmpty()) {
            sources.add(data.vertices.keys.first())
        }
        
        // Step 3: Use BFS-based longest path from sources
        // Process nodes in topological order (ignoring back edges)
        val inDegreeMap = mutableMapOf<String, Int>()
        for (nodeId in data.vertices.keys) {
            val nonBackInEdges = (inEdges[nodeId] ?: emptyList()).filter { sourceId ->
                !backEdges.contains(sourceId to nodeId)
            }
            inDegreeMap[nodeId] = nonBackInEdges.size
        }
        
        val queue = ArrayDeque<String>()
        for (source in sources) {
            nodeRanks[source] = 0
            queue.add(source)
        }
        
        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            val currentRank = nodeRanks[nodeId] ?: 0
            
            for (targetId in outEdges[nodeId] ?: emptyList()) {
                // Skip back edges when calculating ranks
                if (backEdges.contains(nodeId to targetId)) {
                    continue
                }
                
                // Update target rank to be at least currentRank + 1 (longest path)
                val newRank = currentRank + 1
                if (nodeRanks[targetId] == -1 || newRank > nodeRanks[targetId]!!) {
                    nodeRanks[targetId] = newRank
                }
                
                // Decrease in-degree and add to queue when all predecessors processed
                inDegreeMap[targetId] = (inDegreeMap[targetId] ?: 1) - 1
                if (inDegreeMap[targetId] == 0) {
                    queue.add(targetId)
                }
            }
        }
        
        // Handle any remaining unvisited nodes (disconnected components)
        for (nodeId in data.vertices.keys) {
            if (nodeRanks[nodeId] == -1) {
                nodeRanks[nodeId] = 0
            }
        }
        
        // Build rank -> nodes mapping
        for ((nodeId, rank) in nodeRanks) {
            rankNodes.getOrPut(rank) { mutableListOf() }.add(nodeId)
        }
    }
    
    /**
     * Detect back edges using DFS.
     * A back edge is an edge from a node to one of its ancestors in the DFS tree.
     */
    private fun detectBackEdges() {
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()  // Nodes currently in the DFS path
        
        fun dfs(nodeId: String) {
            visited.add(nodeId)
            inStack.add(nodeId)
            
            for (targetId in outEdges[nodeId] ?: emptyList()) {
                if (targetId in inStack) {
                    // Found a back edge (cycle)
                    backEdges.add(nodeId to targetId)
                } else if (targetId !in visited) {
                    dfs(targetId)
                }
            }
            
            inStack.remove(nodeId)
        }
        
        // Run DFS from all unvisited nodes
        for (nodeId in data.vertices.keys) {
            if (nodeId !in visited) {
                dfs(nodeId)
            }
        }
    }

    private fun orderNodesInRanks() {
        // Use barycenter heuristic to minimize edge crossings
        // Iterate multiple times for better results
        val ranks = rankNodes.keys.sorted()
        val iterations = 4  // Number of iterations (like dagre)
        
        for (iteration in 0 until iterations) {
            // Forward pass (top to bottom)
            for (i in 1 until ranks.size) {
                val rank = ranks[i]
                orderRankByBarycenter(rank, ranks[i - 1], useInEdges = true)
            }
            
            // Backward pass (bottom to top)
            for (i in (ranks.size - 2) downTo 0) {
                val rank = ranks[i]
                orderRankByBarycenter(rank, ranks[i + 1], useInEdges = false)
            }
        }
    }
    
    /**
     * Order nodes in a rank using barycenter heuristic.
     * The barycenter of a node is the average position of its connected nodes in the reference rank.
     */
    private fun orderRankByBarycenter(rank: Int, refRank: Int, useInEdges: Boolean) {
        val nodes = rankNodes[rank] ?: return
        val refNodes = rankNodes[refRank] ?: return
        
        // Create position map for reference rank
        val refPositions = refNodes.withIndex().associate { it.value to it.index }
        
        // Calculate barycenter for each node
        val barycenters = mutableMapOf<String, Double>()
        for (nodeId in nodes) {
            val connectedNodes = if (useInEdges) {
                // Get predecessors (nodes pointing to this node)
                (inEdges[nodeId] ?: emptyList()).filter { sourceId ->
                    // Only consider edges from the reference rank, excluding back edges
                    nodeRanks[sourceId] == refRank && !backEdges.contains(sourceId to nodeId)
                }
            } else {
                // Get successors (nodes this node points to)
                (outEdges[nodeId] ?: emptyList()).filter { targetId ->
                    // Only consider edges to the reference rank, excluding back edges
                    nodeRanks[targetId] == refRank && !backEdges.contains(nodeId to targetId)
                }
            }
            
            if (connectedNodes.isEmpty()) {
                // No connected nodes in reference rank, keep current relative position
                barycenters[nodeId] = nodes.indexOf(nodeId).toDouble()
            } else {
                // Calculate barycenter as average position of connected nodes
                val sum = connectedNodes.sumOf { refPositions[it]?.toDouble() ?: 0.0 }
                barycenters[nodeId] = sum / connectedNodes.size
            }
        }
        
        // Sort nodes by barycenter
        nodes.sortBy { barycenters[it] ?: 0.0 }
    }

    private fun calculateNodeSizes() {
        for (vertex in data.vertices.values) {
            // Measure text size - use precise measurement if provider available
            val textSize = TextMeasurer.measureWithFallback(
                vertex.label,
                fontSize,
                config,
                textMeasureProvider
            )
            
            // Calculate node size based on shape type (following mermaid-js logic)
            val (width, height) = calculateShapeSize(vertex.shape, textSize, config)
            
            vertex.bounds = Bounds(
                x = 0f,
                y = 0f,
                width = width.coerceAtLeast(config.nodeMinWidth),
                height = height.coerceAtLeast(config.nodeMinHeight)
            )
        }
    }
    
    /**
     * Calculate node dimensions based on shape type and text size.
     * Follows mermaid-js sizing logic for each shape.
     * 
     * @param shape The node shape type
     * @param textSize The measured text dimensions
     * @param config Layout configuration with padding values
     * @return Pair of (width, height) for the node
     */
    private fun calculateShapeSize(
        shape: NodeShape,
        textSize: TextSize,
        config: LayoutConfig
    ): Pair<Float, Float> {
        val padding = config.textPadding
        val textWidth = textSize.width
        val textHeight = textSize.height
        
        return when (shape) {
            // Diamond (菱形): mermaid-js question.ts
            // s = w + h where w = bbox.width + padding, h = bbox.height + padding
            // The diamond size is the sum of text width and height plus padding
            NodeShape.DIAMOND -> {
                val w = textWidth + padding
                val h = textHeight + padding
                val s = w + h
                s to s
            }
            
            // Circle: diameter = max(textWidth, textHeight) + padding * 2
            // Ensure text fits inside the circle
            NodeShape.CIRCLE -> {
                val diameter = maxOf(textWidth, textHeight) + padding * 2
                diameter to diameter
            }
            
            // Double Circle: slightly larger than single circle
            NodeShape.DOUBLE_CIRCLE -> {
                val innerDiameter = maxOf(textWidth, textHeight) + padding * 2
                // Add extra space for the outer circle (typically 6-8px gap)
                val diameter = innerDiameter + 8f
                diameter to diameter
            }
            
            // Hexagon (六边形): mermaid-js hexagon.ts
            // h = bbox.height + padding
            // m = h / 4 (the slope margin)
            // w = bbox.width + 2 * m + padding
            NodeShape.HEXAGON -> {
                val h = textHeight + padding * 2
                val m = h / 4
                val w = textWidth + 2 * m + padding * 2
                w to h
            }
            
            // Stadium (体育场形/胶囊形): mermaid-js stadium.ts
            // h = bbox.height + padding
            // w = bbox.width + h / 4 + padding (extra width for rounded ends)
            NodeShape.STADIUM -> {
                val h = textHeight + padding * 2
                val w = textWidth + h / 4 + padding * 2
                w to h
            }
            
            // Parallelogram (平行四边形): need extra width for the slant
            // Slant is typically 1/4 of the height
            NodeShape.PARALLELOGRAM, NodeShape.PARALLELOGRAM_ALT -> {
                val h = textHeight + padding * 2
                val slant = h / 4
                val w = textWidth + slant * 2 + padding * 2
                w to h
            }
            
            // Trapezoid (梯形): wider at bottom, narrower at top
            // Need extra width for the inward slope
            NodeShape.TRAPEZOID, NodeShape.TRAPEZOID_ALT -> {
                val h = textHeight + padding * 2
                val inset = h / 4
                val w = textWidth + inset * 2 + padding * 2
                w to h
            }
            
            // Subroutine (子程序): has double vertical bars on sides
            // Extra width for the bars
            NodeShape.SUBROUTINE -> {
                val barWidth = 8f
                val w = textWidth + padding * 2 + barWidth * 2
                val h = textHeight + padding * 2
                w to h
            }
            
            // Cylinder (圆柱体): extra height for top and bottom ellipses
            NodeShape.CYLINDER -> {
                val w = textWidth + padding * 2
                val ellipseHeight = 10f // Height of each ellipse cap
                val h = textHeight + padding * 2 + ellipseHeight * 2
                w to h
            }
            
            // Asymmetric (非对称/旗帜形): extra width for the flag tail
            NodeShape.ASYMMETRIC -> {
                val h = textHeight + padding * 2
                val tailWidth = h / 4
                val w = textWidth + tailWidth + padding * 2
                w to h
            }
            
            // Rectangle, Rounded, and other standard shapes
            // w = textWidth + padding * 2
            // h = textHeight + padding * 2
            NodeShape.RECTANGLE, NodeShape.ROUNDED -> {
                val w = textWidth + padding * 2
                val h = textHeight + padding * 2
                w to h
            }
            
            // Default for any other shapes
            else -> {
                val w = textWidth + padding * 2
                val h = textHeight + padding * 2
                w to h
            }
        }
    }

    private fun calculatePositions() {
        val isHorizontal = data.direction.isHorizontal
        val isReversed = data.direction == Direction.BOTTOM_TO_TOP || 
                         data.direction == Direction.RIGHT_TO_LEFT
        
        val ranks = if (isReversed) rankNodes.keys.sortedDescending() else rankNodes.keys.sorted()
        
        // Calculate max dimension per rank (in the main direction)
        val rankSizes = mutableMapOf<Int, Float>()
        for (rank in ranks) {
            val nodes = rankNodes[rank] ?: continue
            val maxSize = nodes.maxOfOrNull { nodeId ->
                val vertex = data.vertices[nodeId] ?: return@maxOfOrNull 0f
                if (isHorizontal) vertex.bounds.width else vertex.bounds.height
            } ?: 0f
            rankSizes[rank] = maxSize
        }
        
        // Calculate cross dimension total for each rank (perpendicular to main direction)
        val rankCrossSize = mutableMapOf<Int, Float>()
        for (rank in ranks) {
            val nodes = rankNodes[rank] ?: continue
            val crossSpacing = if (isHorizontal) config.nodeSpacingY else config.nodeSpacingX
            var totalCross = 0f
            for (nodeId in nodes) {
                val vertex = data.vertices[nodeId] ?: continue
                totalCross += if (isHorizontal) vertex.bounds.height else vertex.bounds.width
            }
            totalCross += (nodes.size - 1).coerceAtLeast(0) * crossSpacing
            rankCrossSize[rank] = totalCross
        }
        
        // Find the maximum cross dimension across all ranks (for centering)
        val maxCrossSize = rankCrossSize.values.maxOrNull() ?: 0f
        
        // Calculate rank positions along the main axis
        // Use rankSpacing for distance between different ranks (layers)
        val rankPositions = mutableMapOf<Int, Float>()
        var mainPosition = config.diagramPadding
        
        for (rank in ranks) {
            rankPositions[rank] = mainPosition
            val size = rankSizes[rank] ?: 0f
            // Use rankSpacing for distance between different ranks
            mainPosition += size + config.rankSpacing
        }
        
        // Position nodes within each rank
        for (rank in ranks) {
            val nodes = rankNodes[rank] ?: continue
            val rankMainPos = rankPositions[rank] ?: 0f
            val rankMaxSize = rankSizes[rank] ?: 0f
            val crossSize = rankCrossSize[rank] ?: 0f
            
            // Center this rank's nodes in the cross dimension
            val crossOffset = config.diagramPadding + (maxCrossSize - crossSize) / 2
            var crossPosition = crossOffset
            val crossSpacing = if (isHorizontal) config.nodeSpacingY else config.nodeSpacingX
            
            for (nodeId in nodes) {
                val vertex = data.vertices[nodeId] ?: continue
                
                val x: Float
                val y: Float
                
                if (isHorizontal) {
                    // Horizontal layout (LR/RL): ranks are columns
                    // Center node within the rank's column
                    x = rankMainPos + (rankMaxSize - vertex.bounds.width) / 2
                    y = crossPosition
                    crossPosition += vertex.bounds.height + crossSpacing
                } else {
                    // Vertical layout (TB/BT): ranks are rows
                    // Center node within the rank's row
                    x = crossPosition
                    y = rankMainPos + (rankMaxSize - vertex.bounds.height) / 2
                    crossPosition += vertex.bounds.width + crossSpacing
                }
                
                vertex.bounds = vertex.bounds.copy(x = x, y = y)
            }
        }
    }

    private fun routeEdges() {
        // First pass: allocate ports for all edges
        allocateEdgePorts()
        
        // Second pass: calculate edge points using allocated ports
        for ((edgeIndex, edge) in data.edges.withIndex()) {
            val sourceVertex = data.vertices[edge.sourceId] ?: continue
            val targetVertex = data.vertices[edge.targetId] ?: continue
            
            // Check if this is a back edge (cycle edge)
            val isBackEdge = backEdges.contains(edge.sourceId to edge.targetId)
            
            // Calculate edge points with proper routing and port allocation
            val points = calculateEdgePoints(sourceVertex, targetVertex, isBackEdge, edgeIndex)
            edge.points = points
        }
    }
    
    /**
     * Allocate ports for all edges to avoid overlapping connections.
     */
    private fun allocateEdgePorts() {
        val isHorizontal = data.direction.isHorizontal
        
        for ((edgeIndex, edge) in data.edges.withIndex()) {
            val sourceVertex = data.vertices[edge.sourceId] ?: continue
            val targetVertex = data.vertices[edge.targetId] ?: continue
            val isBackEdge = backEdges.contains(edge.sourceId to edge.targetId)
            
            // Determine connection directions for source and target
            val (sourceDir, targetDir) = determineConnectionDirections(
                sourceVertex, targetVertex, isHorizontal, isBackEdge
            )
            
            // Register ports for both source and target
            portAllocator.registerPort(edge.sourceId, sourceDir, edgeIndex)
            portAllocator.registerPort(edge.targetId, targetDir, edgeIndex)
        }
    }
    
    /**
     * Determine the connection directions for source and target vertices.
     */
    private fun determineConnectionDirections(
        source: FlowVertex,
        target: FlowVertex,
        isHorizontal: Boolean,
        isBackEdge: Boolean
    ): Pair<ConnectionDirection, ConnectionDirection> {
        val dx = target.bounds.centerX - source.bounds.centerX
        val dy = target.bounds.centerY - source.bounds.centerY
        
        if (isBackEdge) {
            // For back edges, route around the nodes
            return if (isHorizontal) {
                if (source.bounds.centerY >= target.bounds.centerY) {
                    ConnectionDirection.TOP to ConnectionDirection.TOP
                } else {
                    ConnectionDirection.BOTTOM to ConnectionDirection.BOTTOM
                }
            } else {
                if (source.bounds.centerX >= target.bounds.centerX) {
                    ConnectionDirection.RIGHT to ConnectionDirection.RIGHT
                } else {
                    ConnectionDirection.LEFT to ConnectionDirection.LEFT
                }
            }
        }
        
        // Normal edges: determine direction based on relative positions
        val absDx = kotlin.math.abs(dx)
        val absDy = kotlin.math.abs(dy)
        
        // Bias toward the layout direction
        val biasedAbsDx = absDx * (if (isHorizontal) 1.5f else 1f)
        val biasedAbsDy = absDy * (if (!isHorizontal) 1.5f else 1f)
        
        return if (biasedAbsDx > biasedAbsDy) {
            // Horizontal connection
            if (dx > 0) {
                ConnectionDirection.RIGHT to ConnectionDirection.LEFT
            } else {
                ConnectionDirection.LEFT to ConnectionDirection.RIGHT
            }
        } else {
            // Vertical connection
            if (dy > 0) {
                ConnectionDirection.BOTTOM to ConnectionDirection.TOP
            } else {
                ConnectionDirection.TOP to ConnectionDirection.BOTTOM
            }
        }
    }

    private fun calculateEdgePoints(
        source: FlowVertex, 
        target: FlowVertex, 
        isBackEdge: Boolean,
        edgeIndex: Int
    ): List<Point> {
        val isHorizontal = data.direction.isHorizontal
        
        val sourceCenter = Point(source.bounds.centerX, source.bounds.centerY)
        val targetCenter = Point(target.bounds.centerX, target.bounds.centerY)
        
        // Determine the direction from source to target
        val dx = targetCenter.x - sourceCenter.x
        val dy = targetCenter.y - sourceCenter.y
        
        // Determine connection directions and get port offsets
        val (sourceDir, targetDir) = determineConnectionDirections(source, target, isHorizontal, isBackEdge)
        
        // Calculate the edge length for port distribution
        val sourceEdgeLength = when (sourceDir) {
            ConnectionDirection.TOP, ConnectionDirection.BOTTOM -> source.bounds.width
            ConnectionDirection.LEFT, ConnectionDirection.RIGHT -> source.bounds.height
        }
        val targetEdgeLength = when (targetDir) {
            ConnectionDirection.TOP, ConnectionDirection.BOTTOM -> target.bounds.width
            ConnectionDirection.LEFT, ConnectionDirection.RIGHT -> target.bounds.height
        }
        
        // Get port indices for this edge
        val sourcePortIndex = getPortIndex(source.id, sourceDir, edgeIndex)
        val targetPortIndex = getPortIndex(target.id, targetDir, edgeIndex)
        
        // Calculate port offsets
        val sourcePortOffset = portAllocator.calculatePortOffset(source.id, sourceDir, sourcePortIndex, sourceEdgeLength)
        val targetPortOffset = portAllocator.calculatePortOffset(target.id, targetDir, targetPortIndex, targetEdgeLength)
        
        // Calculate connection points with port offsets
        val sourcePoint = getConnectionPointWithOffset(source, sourceDir, sourcePortOffset, isBackEdge)
        val targetPoint = getConnectionPointWithOffset(target, targetDir, targetPortOffset, isBackEdge)
        
        // For back edges or same-rank edges, add control points to create a curved path
        if (isBackEdge) {
            return createBackEdgePath(sourcePoint, targetPoint, source, target, isHorizontal)
        }
        
        // For normal edges spanning multiple ranks, add a middle control point for smooth curves
        val sourceRank = nodeRanks[source.id] ?: 0
        val targetRank = nodeRanks[target.id] ?: 0
        
        if (kotlin.math.abs(sourceRank - targetRank) > 1 || 
            (sourceRank == targetRank && source.id != target.id)) {
            // Same rank or skipping ranks - add control points
            val midX = (sourcePoint.x + targetPoint.x) / 2
            val midY = (sourcePoint.y + targetPoint.y) / 2
            return listOf(sourcePoint, Point(midX, midY), targetPoint)
        }
        
        // Direct connection with one control point for smooth curve
        val midX = (sourcePoint.x + targetPoint.x) / 2
        val midY = (sourcePoint.y + targetPoint.y) / 2
        return listOf(sourcePoint, Point(midX, midY), targetPoint)
    }
    
    /**
     * Get the port index for a specific edge at a node's direction.
     */
    private fun getPortIndex(nodeId: String, direction: ConnectionDirection, edgeIndex: Int): Int {
        // Find the port index by looking up the registration order
        // Since we registered ports in order, we need to find which index this edge got
        var portIndex = 0
        for ((idx, edge) in data.edges.withIndex()) {
            if (idx == edgeIndex) break
            val vertex = data.vertices[nodeId] ?: continue
            val otherVertex = if (edge.sourceId == nodeId) {
                data.vertices[edge.targetId]
            } else if (edge.targetId == nodeId) {
                data.vertices[edge.sourceId]
            } else {
                continue
            } ?: continue
            
            val isBackEdge = backEdges.contains(edge.sourceId to edge.targetId)
            val (srcDir, tgtDir) = determineConnectionDirections(
                data.vertices[edge.sourceId] ?: continue,
                data.vertices[edge.targetId] ?: continue,
                data.direction.isHorizontal,
                isBackEdge
            )
            
            val dir = if (edge.sourceId == nodeId) srcDir else tgtDir
            if (dir == direction) {
                portIndex++
            }
        }
        return portIndex
    }
    
    /**
     * Calculate connection point with port offset.
     */
    private fun getConnectionPointWithOffset(
        vertex: FlowVertex,
        direction: ConnectionDirection,
        portOffset: Float,
        isBackEdge: Boolean
    ): Point {
        val bounds = vertex.bounds
        val cx = bounds.centerX
        val cy = bounds.centerY
        
        // For diamond shape, calculate the connection point on the diamond edge
        if (vertex.shape == NodeShape.DIAMOND) {
            return getDiamondConnectionPointWithOffset(bounds, direction, portOffset, isBackEdge)
        }
        
        // For other shapes, use rectangle-based connection points
        return when (direction) {
            ConnectionDirection.TOP -> Point(cx + portOffset, bounds.y)
            ConnectionDirection.BOTTOM -> Point(cx + portOffset, bounds.bottom)
            ConnectionDirection.LEFT -> Point(bounds.x, cy + portOffset)
            ConnectionDirection.RIGHT -> Point(bounds.right, cy + portOffset)
        }
    }
    
    /**
     * Calculate diamond connection point with port offset.
     */
    private fun getDiamondConnectionPointWithOffset(
        bounds: Bounds,
        direction: ConnectionDirection,
        portOffset: Float,
        isBackEdge: Boolean
    ): Point {
        val cx = bounds.centerX
        val cy = bounds.centerY
        val halfW = bounds.width / 2
        val halfH = bounds.height / 2
        
        // For diamond, the connection point is at the diamond's vertices or edges
        // With port offset, we move along the edge
        return when (direction) {
            ConnectionDirection.TOP -> {
                // Top vertex of diamond, offset moves along the top-left or top-right edge
                val offsetRatio = portOffset / (halfW * 0.7f)  // Normalize offset
                Point(cx + offsetRatio * halfW * 0.3f, cy - halfH + kotlin.math.abs(offsetRatio) * halfH * 0.2f)
            }
            ConnectionDirection.BOTTOM -> {
                // Bottom vertex of diamond
                val offsetRatio = portOffset / (halfW * 0.7f)
                Point(cx + offsetRatio * halfW * 0.3f, cy + halfH - kotlin.math.abs(offsetRatio) * halfH * 0.2f)
            }
            ConnectionDirection.LEFT -> {
                // Left vertex of diamond
                val offsetRatio = portOffset / (halfH * 0.7f)
                Point(cx - halfW + kotlin.math.abs(offsetRatio) * halfW * 0.2f, cy + offsetRatio * halfH * 0.3f)
            }
            ConnectionDirection.RIGHT -> {
                // Right vertex of diamond
                val offsetRatio = portOffset / (halfH * 0.7f)
                Point(cx + halfW - kotlin.math.abs(offsetRatio) * halfW * 0.2f, cy + offsetRatio * halfH * 0.3f)
            }
        }
    }
    
    /**
     * Create a path for back edges (edges that go against the flow direction).
     * Uses a curved path that goes around to avoid crossing other nodes.
     * Optimized to provide adequate spacing for arrow heads and avoid overlap.
     */
    private fun createBackEdgePath(
        sourcePoint: Point,
        targetPoint: Point,
        source: FlowVertex,
        target: FlowVertex,
        isHorizontal: Boolean
    ): List<Point> {
        // Increased offset factor from 0.6 to 0.8 for better spacing
        val baseOffset = config.nodeSpacingX.coerceAtLeast(config.nodeSpacingY) * 0.8f
        // Additional margin for arrow head (considering ARROW_SIZE = 10f in EdgeShapes)
        val arrowMargin = 15f
        
        return if (isHorizontal) {
            // For horizontal layout, route the back edge above or below
            val routeAbove = sourcePoint.y >= targetPoint.y
            
            // Find all nodes that might be in the path between source and target
            val minX = minOf(source.bounds.x, target.bounds.x)
            val maxX = maxOf(source.bounds.right, target.bounds.right)
            val nodesInPath = data.vertices.values.filter { vertex ->
                vertex.id != source.id && vertex.id != target.id &&
                vertex.bounds.right >= minX && vertex.bounds.x <= maxX
            }
            
            // Calculate route position that clears all nodes in the path
            val routeY = if (routeAbove) {
                val minY = nodesInPath.minOfOrNull { it.bounds.y } ?: source.bounds.y
                minOf(minY, source.bounds.y, target.bounds.y) - baseOffset
            } else {
                val maxY = nodesInPath.maxOfOrNull { it.bounds.bottom } ?: source.bounds.bottom
                maxOf(maxY, source.bounds.bottom, target.bounds.bottom) + baseOffset
            }
            
            // Add extra control points for smoother curve and better arrow positioning
            // Adjust source and target connection points to include arrow margin
            val adjustedSourcePoint = Point(
                sourcePoint.x,
                if (routeAbove) sourcePoint.y - arrowMargin else sourcePoint.y + arrowMargin
            )
            val adjustedTargetPoint = Point(
                targetPoint.x,
                if (routeAbove) targetPoint.y - arrowMargin else targetPoint.y + arrowMargin
            )
            
            listOf(
                sourcePoint,
                adjustedSourcePoint,
                Point(adjustedSourcePoint.x, routeY),
                Point(adjustedTargetPoint.x, routeY),
                adjustedTargetPoint,
                targetPoint
            )
        } else {
            // For vertical layout (TB/BT), route the back edge to the left or right
            // Determine which side to route based on source position relative to target
            val sourceIsRight = source.bounds.centerX > target.bounds.centerX
            
            // Find all nodes that might be in the path between source and target vertically
            // For back edges, we want to route around ALL nodes between source and target ranks
            val sourceRank = nodeRanks[source.id] ?: 0
            val targetRank = nodeRanks[target.id] ?: 0
            val minRank = minOf(sourceRank, targetRank)
            val maxRank = maxOf(sourceRank, targetRank)
            
            // Get all nodes in ranks between source and target (inclusive)
            val nodesInPath = mutableListOf<FlowVertex>()
            for (rank in minRank..maxRank) {
                val nodesInRank = rankNodes[rank] ?: emptyList()
                for (nodeId in nodesInRank) {
                    val vertex = data.vertices[nodeId]
                    if (vertex != null && vertex.id != source.id && vertex.id != target.id) {
                        nodesInPath.add(vertex)
                    }
                }
            }
            
            // Calculate route position that clears all nodes
            val routeX = if (sourceIsRight) {
                // Source is on the right side, route on the right (further right)
                val maxX = if (nodesInPath.isNotEmpty()) {
                    nodesInPath.maxOf { it.bounds.right }
                } else {
                    maxOf(source.bounds.right, target.bounds.right)
                }
                maxOf(maxX, source.bounds.right, target.bounds.right) + baseOffset
            } else {
                // Source is on the left side, route on the left (further left)
                val minX = if (nodesInPath.isNotEmpty()) {
                    nodesInPath.minOf { it.bounds.x }
                } else {
                    minOf(source.bounds.x, target.bounds.x)
                }
                minOf(minX, source.bounds.x, target.bounds.x) - baseOffset
            }
            
            // Add extra control points for smoother curve and better arrow positioning
            val adjustedSourcePoint = Point(
                if (sourceIsRight) sourcePoint.x + arrowMargin else sourcePoint.x - arrowMargin,
                sourcePoint.y
            )
            val adjustedTargetPoint = Point(
                if (sourceIsRight) targetPoint.x + arrowMargin else targetPoint.x - arrowMargin,
                targetPoint.y
            )
            
            listOf(
                sourcePoint,
                adjustedSourcePoint,
                Point(routeX, adjustedSourcePoint.y),
                Point(routeX, adjustedTargetPoint.y),
                adjustedTargetPoint,
                targetPoint
            )
        }
    }
    
    /**
     * Calculate the connection point on a node based on the direction to the other node.
     * This provides more accurate edge endpoints than using a fixed direction.
     */
    private fun getConnectionPointByDirection(
        vertex: FlowVertex,
        dx: Float,
        dy: Float,
        isHorizontal: Boolean,
        isSource: Boolean
    ): Point {
        val bounds = vertex.bounds
        val cx = bounds.centerX
        val cy = bounds.centerY
        
        // Handle special shapes
        when (vertex.shape) {
            NodeShape.CIRCLE, NodeShape.DOUBLE_CIRCLE -> {
                // Calculate intersection with circle
                val radius = minOf(bounds.width, bounds.height) / 2
                val dirX = if (isSource) dx else -dx
                val dirY = if (isSource) dy else -dy
                val len = kotlin.math.sqrt((dirX * dirX + dirY * dirY).toDouble()).toFloat()
                if (len > 0) {
                    return Point(
                        cx + radius * dirX / len,
                        cy + radius * dirY / len
                    )
                }
                return Point(cx, cy)
            }
            NodeShape.DIAMOND -> {
                // Diamond: calculate intersection with diamond edges
                return getDiamondConnectionPoint(bounds, dx, dy, isSource)
            }
            else -> {
                // Rectangle and other shapes: determine which edge to connect to
                return getRectangleConnectionPoint(bounds, dx, dy, isHorizontal, isSource)
            }
        }
    }
    
    /**
     * Get connection point on a diamond shape.
     */
    private fun getDiamondConnectionPoint(
        bounds: Bounds,
        dx: Float,
        dy: Float,
        isSource: Boolean
    ): Point {
        val cx = bounds.centerX
        val cy = bounds.centerY
        val halfW = bounds.width / 2
        val halfH = bounds.height / 2
        
        // Determine which edge of the diamond to use based on direction
        val dirX = if (isSource) dx else -dx
        val dirY = if (isSource) dy else -dy
        
        val absDx = kotlin.math.abs(dirX)
        val absDy = kotlin.math.abs(dirY)
        
        // Normalize to find intersection with diamond
        val ratioX = if (absDx > 0) halfW / absDx else Float.MAX_VALUE
        val ratioY = if (absDy > 0) halfH / absDy else Float.MAX_VALUE
        val ratio = minOf(ratioX, ratioY)
        
        return Point(
            cx + dirX * ratio,
            cy + dirY * ratio
        )
    }
    
    /**
     * Get connection point on a rectangle.
     * Chooses the edge based on the direction to the other node.
     */
    private fun getRectangleConnectionPoint(
        bounds: Bounds,
        dx: Float,
        dy: Float,
        isHorizontal: Boolean,
        isSource: Boolean
    ): Point {
        val cx = bounds.centerX
        val cy = bounds.centerY
        
        val dirX = if (isSource) dx else -dx
        val dirY = if (isSource) dy else -dy
        
        // Determine which edge to use based on the dominant direction
        val absDx = kotlin.math.abs(dirX)
        val absDy = kotlin.math.abs(dirY)
        
        // Bias toward the layout direction for cleaner edges
        val biasedAbsDx = absDx * (if (isHorizontal) 1.5f else 1f)
        val biasedAbsDy = absDy * (if (!isHorizontal) 1.5f else 1f)
        
        return if (biasedAbsDx > biasedAbsDy) {
            // Connect to left or right edge
            if (dirX > 0) {
                Point(bounds.right, cy)
            } else {
                Point(bounds.x, cy)
            }
        } else {
            // Connect to top or bottom edge
            if (dirY > 0) {
                Point(cx, bounds.bottom)
            } else {
                Point(cx, bounds.y)
            }
        }
    }

    private fun getConnectionPoint(
        vertex: FlowVertex,
        towardPoint: Point,
        isHorizontal: Boolean,
        isSource: Boolean
    ): Point {
        val dx = towardPoint.x - vertex.bounds.centerX
        val dy = towardPoint.y - vertex.bounds.centerY
        return getConnectionPointByDirection(vertex, dx, dy, isHorizontal, isSource)
    }

    private fun layoutSubgraphs() {
        val isHorizontal = data.direction.isHorizontal
        
        // Calculate the total title space needed
        val titleSpace = config.subgraphTitleTopMargin + config.subgraphTitleHeight + config.subgraphTitleBottomMargin
        
        // Process subgraphs in reverse dependency order (nested first, then parents)
        // This ensures nested subgraph bounds are calculated before parent subgraphs
        val processOrder = getSubgraphProcessOrder()
        
        for (subgraphId in processOrder) {
            val subgraph = data.subgraphs[subgraphId] ?: continue
            var bounds = Bounds.EMPTY
            
            // Include all vertices in this subgraph
            for (vertexId in subgraph.vertexIds) {
                val vertex = data.vertices[vertexId] ?: continue
                bounds = bounds.union(vertex.bounds)
            }
            
            // Include nested subgraphs
            for (nestedId in subgraph.subgraphIds) {
                val nested = data.subgraphs[nestedId] ?: continue
                bounds = bounds.union(nested.bounds)
            }
            
            // Add padding
            if (bounds.width > 0 && bounds.height > 0) {
                // First expand with padding
                var expandedBounds = bounds.expand(config.subgraphPadding)
                
                // Then add extra space for the title at the top (or left for horizontal layout)
                if (!subgraph.title.isNullOrEmpty()) {
                    if (isHorizontal) {
                        // For horizontal layout, title is at the left
                        // We already offset nodes, so just ensure minimum width includes title space
                        val minWidth = expandedBounds.width + titleSpace
                        if (expandedBounds.width < minWidth) {
                            expandedBounds = expandedBounds.copy(width = minWidth)
                        }
                    } else {
                        // For vertical layout, title is at the top
                        // We already offset nodes, so the bounds should naturally include the title space
                        // Add a bit more top padding to ensure title fits
                        expandedBounds = expandedBounds.copy(
                            y = expandedBounds.y - titleSpace,
                            height = expandedBounds.height + titleSpace
                        )
                    }
                }
                
                subgraph.bounds = expandedBounds
            }
        }
    }
    
    /**
     * Get the order to process subgraphs (nested first, then parents).
     */
    private fun getSubgraphProcessOrder(): List<String> {
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        
        fun visit(subgraphId: String) {
            if (subgraphId in visited) return
            
            val subgraph = data.subgraphs[subgraphId] ?: return
            
            // Visit nested subgraphs first
            for (nestedId in subgraph.subgraphIds) {
                visit(nestedId)
            }
            
            visited.add(subgraphId)
            result.add(subgraphId)
        }
        
        // Start from root subgraphs
        for (rootId in data.rootSubgraphIds) {
            visit(rootId)
        }
        
        return result
    }

    private fun calculateBounds() {
        var bounds = Bounds.EMPTY
        
        // Include all vertices
        for (vertex in data.vertices.values) {
            bounds = bounds.union(vertex.bounds)
        }
        
        // Include all subgraphs
        for (subgraph in data.subgraphs.values) {
            bounds = bounds.union(subgraph.bounds)
        }
        
        // Add diagram padding
        if (bounds.width > 0 && bounds.height > 0) {
            data.bounds = bounds.expand(config.diagramPadding)
        }
    }
}
