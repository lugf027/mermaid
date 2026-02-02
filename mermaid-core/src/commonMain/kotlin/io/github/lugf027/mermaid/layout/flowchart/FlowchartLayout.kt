/**
 * FlowchartLayout - Layout engine for flowchart diagrams.
 */
package io.github.lugf027.mermaid.layout.flowchart

import io.github.lugf027.mermaid.layout.LayoutConfig
import io.github.lugf027.mermaid.layout.LayoutEngine
import io.github.lugf027.mermaid.layout.TextMeasurer
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
        val layouter = FlowchartLayouter(data, config)
        return layouter.layout()
    }
}

/**
 * Internal implementation of flowchart layout.
 */
internal class FlowchartLayouter(
    private val data: FlowchartData,
    private val config: LayoutConfig
) {
    // Node ID -> rank (layer)
    private val nodeRanks = mutableMapOf<String, Int>()
    
    // Rank -> list of node IDs in order
    private val rankNodes = mutableMapOf<Int, MutableList<String>>()
    
    // Adjacency lists
    private val outEdges = mutableMapOf<String, MutableList<String>>()
    private val inEdges = mutableMapOf<String, MutableList<String>>()

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

        // Step 6: Route edges
        routeEdges()

        // Step 7: Layout subgraphs
        layoutSubgraphs()

        // Step 8: Calculate overall bounds
        calculateBounds()

        return data
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
            val textSize = TextMeasurer.measure(vertex.label, config)
            
            // Adjust size based on shape
            val (width, height) = when (vertex.shape) {
                NodeShape.CIRCLE, NodeShape.DOUBLE_CIRCLE -> {
                    val size = maxOf(textSize.width, textSize.height) * 1.2f
                    size to size
                }
                NodeShape.DIAMOND -> {
                    // Diamond needs more space
                    (textSize.width * 1.5f) to (textSize.height * 1.5f)
                }
                NodeShape.HEXAGON -> {
                    (textSize.width * 1.3f) to textSize.height
                }
                else -> textSize.width to textSize.height
            }
            
            vertex.bounds = Bounds(
                x = 0f,
                y = 0f,
                width = width,
                height = height
            )
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
        val rankPositions = mutableMapOf<Int, Float>()
        var mainPosition = config.diagramPadding
        val mainSpacing = if (isHorizontal) config.nodeSpacingX else config.nodeSpacingY
        
        for (rank in ranks) {
            rankPositions[rank] = mainPosition
            val size = rankSizes[rank] ?: 0f
            mainPosition += size + mainSpacing
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
        for (edge in data.edges) {
            val sourceVertex = data.vertices[edge.sourceId] ?: continue
            val targetVertex = data.vertices[edge.targetId] ?: continue
            
            // Check if this is a back edge (cycle edge)
            val isBackEdge = backEdges.contains(edge.sourceId to edge.targetId)
            
            // Calculate edge points with proper routing
            val points = calculateEdgePoints(sourceVertex, targetVertex, isBackEdge)
            edge.points = points
        }
    }

    private fun calculateEdgePoints(
        source: FlowVertex, 
        target: FlowVertex, 
        isBackEdge: Boolean
    ): List<Point> {
        val isHorizontal = data.direction.isHorizontal
        
        val sourceCenter = Point(source.bounds.centerX, source.bounds.centerY)
        val targetCenter = Point(target.bounds.centerX, target.bounds.centerY)
        
        // Determine the direction from source to target
        val dx = targetCenter.x - sourceCenter.x
        val dy = targetCenter.y - sourceCenter.y
        
        // Calculate connection points based on relative positions
        val sourcePoint = getConnectionPointByDirection(source, dx, dy, isHorizontal, isSource = true)
        val targetPoint = getConnectionPointByDirection(target, dx, dy, isHorizontal, isSource = false)
        
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
     * Create a path for back edges (edges that go against the flow direction).
     * Uses a curved path that goes around to avoid crossing other nodes.
     */
    private fun createBackEdgePath(
        sourcePoint: Point,
        targetPoint: Point,
        source: FlowVertex,
        target: FlowVertex,
        isHorizontal: Boolean
    ): List<Point> {
        val offset = config.nodeSpacingX.coerceAtLeast(config.nodeSpacingY) * 0.6f
        
        return if (isHorizontal) {
            // For horizontal layout, route the back edge above or below
            val routeAbove = sourcePoint.y >= targetPoint.y
            val routeY = if (routeAbove) {
                minOf(source.bounds.y, target.bounds.y) - offset
            } else {
                maxOf(source.bounds.bottom, target.bounds.bottom) + offset
            }
            
            listOf(
                sourcePoint,
                Point(sourcePoint.x, routeY),
                Point(targetPoint.x, routeY),
                targetPoint
            )
        } else {
            // For vertical layout, route the back edge to the left or right
            val routeRight = sourcePoint.x >= targetPoint.x
            val routeX = if (routeRight) {
                maxOf(source.bounds.right, target.bounds.right) + offset
            } else {
                minOf(source.bounds.x, target.bounds.x) - offset
            }
            
            listOf(
                sourcePoint,
                Point(routeX, sourcePoint.y),
                Point(routeX, targetPoint.y),
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
        // Calculate bounds for each subgraph
        for (subgraph in data.subgraphs.values) {
            var bounds = Bounds.EMPTY
            
            // Include all vertices in this subgraph
            for (vertexId in subgraph.vertexIds) {
                val vertex = data.vertices[vertexId] ?: continue
                bounds = bounds.union(vertex.bounds)
            }
            
            // Include nested subgraphs
            for (subgraphId in subgraph.subgraphIds) {
                val nested = data.subgraphs[subgraphId] ?: continue
                bounds = bounds.union(nested.bounds)
            }
            
            // Add padding
            if (bounds.width > 0 && bounds.height > 0) {
                subgraph.bounds = bounds.expand(config.subgraphPadding)
            }
        }
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
