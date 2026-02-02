/**
 * Sample Mermaid diagrams for demonstration.
 */
package io.github.lugf027.mermaid.example

/**
 * Collection of sample Mermaid diagram definitions.
 */
public object SampleDiagrams {
    
    /**
     * Simple flowchart example.
     */
    public val simpleFlowchart: String = """
        flowchart TD
            A[Start] --> B{Is it working?}
            B -->|Yes| C[Great!]
            B -->|No| D[Debug]
            D --> B
            C --> E[End]
    """.trimIndent()

    /**
     * Left-to-right flowchart example.
     */
    public val lrFlowchart: String = """
        flowchart LR
            A[Input] --> B[Process]
            B --> C[Output]
            B --> D[Log]
    """.trimIndent()

    /**
     * Flowchart with subgraphs.
     */
    public val subgraphFlowchart: String = """
        flowchart TB
            subgraph Frontend
                A[Web App] --> B[Mobile App]
            end
            subgraph Backend
                C[API Server] --> D[Database]
            end
            A --> C
            B --> C
    """.trimIndent()

    /**
     * Flowchart with different node shapes.
     */
    public val shapesFlowchart: String = """
        flowchart TD
            A[Rectangle] --> B(Rounded)
            B --> C([Stadium])
            C --> D[[Subroutine]]
            D --> E[(Database)]
            E --> F((Circle))
            F --> G>Flag]
            G --> H{Diamond}
            H --> I{{Hexagon}}
            I --> J[/Parallelogram/]
    """.trimIndent()

    /**
     * Get all sample diagrams as a list.
     */
    public fun getAllSamples(): List<Pair<String, String>> = listOf(
        "Simple Flowchart" to simpleFlowchart,
        "LR Flowchart" to lrFlowchart,
        "Subgraph Flowchart" to subgraphFlowchart,
        "Node Shapes" to shapesFlowchart,
    )
}
