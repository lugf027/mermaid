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

    private fun assignRanks() {
        // Use topological sort to assign ranks
        val visited = mutableSetOf<String>()
        val inDegree = mutableMapOf<String, Int>()
        
        // Calculate in-degrees
        for (nodeId in data.vertices.keys) {
            inDegree[nodeId] = inEdges[nodeId]?.size ?: 0
        }

        // Find nodes with no incoming edges (sources)
        val queue = ArrayDeque<String>()
        for (nodeId in data.vertices.keys) {
            if ((inDegree[nodeId] ?: 0) == 0) {
                queue.add(nodeId)
                nodeRanks[nodeId] = 0
            }
        }

        // Process queue
        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            val rank = nodeRanks[nodeId] ?: 0
            
            for (targetId in outEdges[nodeId] ?: emptyList()) {
                val newRank = rank + 1
                val currentRank = nodeRanks[targetId]
                
                if (currentRank == null || newRank > currentRank) {
                    nodeRanks[targetId] = newRank
                }
                
                inDegree[targetId] = (inDegree[targetId] ?: 1) - 1
                if ((inDegree[targetId] ?: 0) == 0 && targetId !in visited) {
                    queue.add(targetId)
                    visited.add(targetId)
                }
            }
        }

        // Handle any remaining unvisited nodes (disconnected or cycles)
        for (nodeId in data.vertices.keys) {
            if (nodeId !in nodeRanks) {
                nodeRanks[nodeId] = 0
            }
        }

        // Build rank -> nodes mapping
        for ((nodeId, rank) in nodeRanks) {
            rankNodes.getOrPut(rank) { mutableListOf() }.add(nodeId)
        }
    }

    private fun orderNodesInRanks() {
        // Simple ordering: maintain insertion order for now
        // A more sophisticated algorithm would minimize edge crossings
        
        // For each rank, try to order nodes to reduce crossings
        val ranks = rankNodes.keys.sorted()
        
        for (i in 1 until ranks.size) {
            val rank = ranks[i]
            val nodes = rankNodes[rank] ?: continue
            val prevRankNodes = rankNodes[ranks[i - 1]] ?: continue
            
            // Sort by average position of predecessors
            nodes.sortBy { nodeId ->
                val predecessors = inEdges[nodeId] ?: emptyList()
                if (predecessors.isEmpty()) {
                    0.0
                } else {
                    predecessors.mapNotNull { predId ->
                        prevRankNodes.indexOf(predId).takeIf { it >= 0 }
                    }.average().takeIf { !it.isNaN() } ?: 0.0
                }
            }
        }
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
        val ranks = rankNodes.keys.sorted()
        
        // Calculate max dimension per rank
        val rankSizes = mutableMapOf<Int, Float>()
        for (rank in ranks) {
            val nodes = rankNodes[rank] ?: continue
            val maxSize = nodes.maxOfOrNull { nodeId ->
                val vertex = data.vertices[nodeId] ?: return@maxOfOrNull 0f
                if (isHorizontal) vertex.bounds.width else vertex.bounds.height
            } ?: 0f
            rankSizes[rank] = maxSize
        }
        
        // Calculate rank positions
        val rankPositions = mutableMapOf<Int, Float>()
        var position = config.diagramPadding
        for (rank in ranks) {
            rankPositions[rank] = position
            val size = rankSizes[rank] ?: 0f
            position += size + (if (isHorizontal) config.nodeSpacingX else config.nodeSpacingY)
        }
        
        // Position nodes within each rank
        for (rank in ranks) {
            val nodes = rankNodes[rank] ?: continue
            val rankPos = rankPositions[rank] ?: 0f
            
            // Calculate total cross dimension
            var crossPosition = config.diagramPadding
            
            for (nodeId in nodes) {
                val vertex = data.vertices[nodeId] ?: continue
                
                val x: Float
                val y: Float
                
                if (isHorizontal) {
                    // Horizontal layout: ranks are columns
                    x = rankPos + (rankSizes[rank] ?: 0f) / 2 - vertex.bounds.width / 2
                    y = crossPosition
                    crossPosition += vertex.bounds.height + config.nodeSpacingY
                } else {
                    // Vertical layout: ranks are rows
                    x = crossPosition
                    y = rankPos + (rankSizes[rank] ?: 0f) / 2 - vertex.bounds.height / 2
                    crossPosition += vertex.bounds.width + config.nodeSpacingX
                }
                
                vertex.bounds = vertex.bounds.copy(x = x, y = y)
            }
        }
        
        // Center nodes within their rank
        centerNodesInRanks(isHorizontal)
    }

    private fun centerNodesInRanks(isHorizontal: Boolean) {
        // Find max cross dimension
        val maxCross = data.vertices.values.maxOfOrNull { v ->
            if (isHorizontal) v.bounds.bottom else v.bounds.right
        } ?: 0f
        
        // Center each rank
        for (rank in rankNodes.keys) {
            val nodes = rankNodes[rank] ?: continue
            val currentMax = nodes.maxOfOrNull { nodeId ->
                val v = data.vertices[nodeId] ?: return@maxOfOrNull 0f
                if (isHorizontal) v.bounds.bottom else v.bounds.right
            } ?: continue
            
            val currentMin = nodes.minOfOrNull { nodeId ->
                val v = data.vertices[nodeId] ?: return@minOfOrNull 0f
                if (isHorizontal) v.bounds.y else v.bounds.x
            } ?: continue
            
            val offset = (maxCross - (currentMax - currentMin + config.diagramPadding * 2)) / 2
            
            if (offset > 0) {
                for (nodeId in nodes) {
                    val vertex = data.vertices[nodeId] ?: continue
                    vertex.bounds = if (isHorizontal) {
                        vertex.bounds.copy(y = vertex.bounds.y + offset)
                    } else {
                        vertex.bounds.copy(x = vertex.bounds.x + offset)
                    }
                }
            }
        }
    }

    private fun routeEdges() {
        for (edge in data.edges) {
            val sourceVertex = data.vertices[edge.sourceId] ?: continue
            val targetVertex = data.vertices[edge.targetId] ?: continue
            
            // Simple direct edge routing
            val points = calculateEdgePoints(sourceVertex, targetVertex)
            edge.points = points
        }
    }

    private fun calculateEdgePoints(source: FlowVertex, target: FlowVertex): List<Point> {
        val isHorizontal = data.direction.isHorizontal
        
        val sourceCenter = Point(source.bounds.centerX, source.bounds.centerY)
        val targetCenter = Point(target.bounds.centerX, target.bounds.centerY)
        
        // Calculate connection points on node boundaries
        val sourcePoint = getConnectionPoint(source, targetCenter, isHorizontal, isSource = true)
        val targetPoint = getConnectionPoint(target, sourceCenter, isHorizontal, isSource = false)
        
        return listOf(sourcePoint, targetPoint)
    }

    private fun getConnectionPoint(
        vertex: FlowVertex,
        towardPoint: Point,
        isHorizontal: Boolean,
        isSource: Boolean
    ): Point {
        val bounds = vertex.bounds
        
        return when (vertex.shape) {
            NodeShape.CIRCLE, NodeShape.DOUBLE_CIRCLE -> {
                // Calculate intersection with circle
                val cx = bounds.centerX
                val cy = bounds.centerY
                val radius = minOf(bounds.width, bounds.height) / 2
                val angle = kotlin.math.atan2(
                    (towardPoint.y - cy).toDouble(),
                    (towardPoint.x - cx).toDouble()
                )
                Point(
                    cx + (radius * kotlin.math.cos(angle)).toFloat(),
                    cy + (radius * kotlin.math.sin(angle)).toFloat()
                )
            }
            NodeShape.DIAMOND -> {
                // Simplified: use center of edge
                if (isHorizontal) {
                    if (isSource) Point(bounds.right, bounds.centerY)
                    else Point(bounds.x, bounds.centerY)
                } else {
                    if (isSource) Point(bounds.centerX, bounds.bottom)
                    else Point(bounds.centerX, bounds.y)
                }
            }
            else -> {
                // Rectangle and other shapes: connect to edge midpoint
                if (isHorizontal) {
                    if (isSource) Point(bounds.right, bounds.centerY)
                    else Point(bounds.x, bounds.centerY)
                } else {
                    if (isSource) Point(bounds.centerX, bounds.bottom)
                    else Point(bounds.centerX, bounds.y)
                }
            }
        }
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
