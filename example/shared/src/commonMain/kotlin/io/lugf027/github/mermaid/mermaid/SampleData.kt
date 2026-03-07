package io.lugf027.github.mermaid.mermaid

/**
 * 所有 28 种 Mermaid 图表类型的预设示例文本。
 */
object SampleData {

    data class DiagramSample(
        val name: String,
        val type: String,
        val text: String,
    )

    val samples: List<DiagramSample> = listOf(
        DiagramSample("Flowchart", "flowchart", """
flowchart TD
    A[Start] --> B{Is it sunny?}
    B -->|Yes| C[Go to the park]
    B -->|No| D[Stay home]
    C --> E[Have fun!]
    D --> E
    E --> F[End]
        """.trimIndent()),

        DiagramSample("Sequence Diagram", "sequence", """
sequenceDiagram
    participant Alice
    participant Bob
    participant Charlie
    Alice->>Bob: Hello Bob, how are you?
    Bob-->>Alice: I'm good, thanks!
    Alice->>Charlie: Hi Charlie
    Charlie->>Bob: Hey Bob
    Bob-->>Charlie: Hello!
        """.trimIndent()),

        DiagramSample("Class Diagram", "classDiagram", """
classDiagram
    Animal <|-- Duck
    Animal <|-- Fish
    Animal : +int age
    Animal : +String gender
    Animal: +isMammal()
    Animal: +mate()
    class Duck{
        +String beakColor
        +swim()
        +quack()
    }
    class Fish{
        -int sizeInFeet
        -canEat()
    }
        """.trimIndent()),

        DiagramSample("State Diagram", "stateDiagram", """
stateDiagram-v2
    [*] --> Still
    Still --> [*]
    Still --> Moving
    Moving --> Still
    Moving --> Crash
    Crash --> [*]
        """.trimIndent()),

        DiagramSample("ER Diagram", "erDiagram", """
erDiagram
    CUSTOMER ||--o{ ORDER : places
    ORDER ||--|{ LINE-ITEM : contains
    CUSTOMER {
        string name
        string custNumber
        string sector
    }
    ORDER {
        int orderNumber
        string deliveryAddress
    }
    LINE-ITEM {
        string productCode
        int quantity
        float pricePerUnit
    }
        """.trimIndent()),

        DiagramSample("Gantt Chart", "gantt", """
gantt
    title A Gantt Diagram
    dateFormat YYYY-MM-DD
    section Section
        A task          :a1, 2024-01-01, 30d
        Another task    :after a1, 20d
    section Another
        Task in Another :2024-01-12, 12d
        another task    :24d
        """.trimIndent()),

        DiagramSample("Pie Chart", "pie", """
pie title Pets adopted by volunteers
    "Dogs" : 386
    "Cats" : 85
    "Rats" : 15
        """.trimIndent()),

        DiagramSample("Git Graph", "gitGraph", """
gitGraph
    commit
    commit
    branch develop
    checkout develop
    commit
    commit
    checkout main
    merge develop
    commit
    commit
        """.trimIndent()),

        DiagramSample("Mindmap", "mindmap", """
mindmap
    root((Central Topic))
        Origins
            Long history
            Popularisation
        Research
            On effectiveness
            On features
        Tools
            Pen and paper
            Mermaid
        """.trimIndent()),

        DiagramSample("Timeline", "timeline", """
timeline
    title History of Social Media Platform
    2002 : LinkedIn
    2004 : Facebook : Google
    2005 : YouTube
    2006 : Twitter
    2010 : Instagram
        """.trimIndent()),

        DiagramSample("Kanban", "kanban", """
kanban
    column1[Todo]
        task1[Create parser]
        task2[Design UI]
    column2[In Progress]
        task3[Implement renderer]
    column3[Done]
        task4[Setup project]
        """.trimIndent()),

        DiagramSample("C4 Diagram", "C4Context", """
C4Context
    title System Context diagram for Internet Banking
    Person(customer, "Banking Customer", "A customer of the bank")
    System(banking, "Internet Banking System", "Allows customers to view balances")
    System_Ext(mail, "E-mail System", "Sendgrid")
    Rel(customer, banking, "Uses")
    Rel(banking, mail, "Sends e-mails using")
        """.trimIndent()),

        DiagramSample("Quadrant Chart", "quadrantChart", """
quadrantChart
    title Reach and engagement of campaigns
    x-axis Low Reach --> High Reach
    y-axis Low Engagement --> High Engagement
    quadrant-1 We should expand
    quadrant-2 Need to promote
    quadrant-3 Re-evaluate
    quadrant-4 May be improved
    Campaign A: [0.3, 0.6]
    Campaign B: [0.45, 0.23]
    Campaign C: [0.57, 0.69]
    Campaign D: [0.78, 0.34]
        """.trimIndent()),

        DiagramSample("XY Chart", "xychart-beta", """
xychart-beta
    title "Sales Revenue"
    x-axis [jan, feb, mar, apr, may, jun]
    y-axis "Revenue (in $)" 4000 --> 11000
    bar [5000, 6000, 7500, 8200, 9800, 10500]
    line [5000, 6000, 7500, 8200, 9800, 10500]
        """.trimIndent()),

        DiagramSample("Requirement Diagram", "requirementDiagram", """
requirementDiagram
    requirement test_req {
        id: 1
        text: the test text.
        risk: high
        verifymethod: test
    }
    element test_entity {
        type: simulation
    }
    test_entity - satisfies -> test_req
        """.trimIndent()),

        DiagramSample("User Journey", "journey", """
journey
    title My working day
    section Go to work
        Make tea: 5: Me
        Go upstairs: 3: Me
        Do work: 1: Me, Cat
    section Go home
        Go downstairs: 5: Me
        Sit down: 5: Me
        """.trimIndent()),

        DiagramSample("Sankey Diagram", "sankey-beta", """
sankey-beta

Agricultural "ichthyol",Electricity,1.025
Agricultural "ichthyol",Livestock and Poultry,6.84
Bio-conversion,Liquid,1.915
Bio-conversion,Losses,4.61
Bio-conversion,Solid,10.33
Bio-conversion,Gas,2.51
        """.trimIndent()),

        DiagramSample("Block Diagram", "block-beta", """
block-beta
    columns 3
    A["Frontend"] B["API Gateway"] C["Backend"]
    D["Database"]
    A --> B
    B --> C
    C --> D
        """.trimIndent()),

        DiagramSample("Packet Diagram", "packet-beta", """
packet-beta
    0-15: "Source Port"
    16-31: "Destination Port"
    32-63: "Sequence Number"
    64-95: "Acknowledgment Number"
    96-99: "Data Offset"
    100-105: "Reserved"
    106-111: "Flags"
    112-127: "Window Size"
    128-143: "Checksum"
    144-159: "Urgent Pointer"
        """.trimIndent()),

        DiagramSample("Architecture Diagram", "architecture-beta", """
architecture-beta
    service api(server)[API Service]
    service disk1(disk)[Storage]
    service disk2(disk)[Storage]
    service server(server)[Server]

    api:R -- L:disk1
    api:B -- T:server
    disk1:R -- L:disk2
        """.trimIndent()),

        DiagramSample("Info Diagram", "info", """
info
        """.trimIndent()),

        DiagramSample("Radar Chart", "radar-beta", """
radar-beta
    title Skill Assessment
    axis Programming, Design, Communication, Leadership, Problem Solving
    curve Developer A
        Programming: 9
        Design: 5
        Communication: 7
        Leadership: 4
        Problem Solving: 8
    curve Developer B
        Programming: 6
        Design: 8
        Communication: 9
        Leadership: 7
        Problem Solving: 6
        """.trimIndent()),

        DiagramSample("Fishbone (Ishikawa)", "ishikawa", """
ishikawa
    title Why Production is Failing
    Materials
        Low quality steel
        Inconsistent supply
    Methods
        Outdated process
        No documentation
    Machines
        Old equipment
        Poor maintenance
        """.trimIndent()),

        DiagramSample("Venn Diagram", "venn", """
venn
    title Technology Skills
    set A[Frontend]
    set B[Backend]
    set C[DevOps]
    overlap A,B[Full Stack]
    overlap A,C[JAMstack]
    overlap B,C[Cloud Native]
    overlap A,B,C[Unicorn]
        """.trimIndent()),

        DiagramSample("Treemap", "treemap", """
treemap
    title Project Budget
    Marketing 40
        Social Media 15
        SEO 10
        Content 15
    Engineering 35
        Frontend 15
        Backend 20
    Operations 25
        HR 10
        Finance 15
        """.trimIndent()),
    )

    /** 获取默认示例（第一个 Flowchart） */
    val defaultSample: DiagramSample get() = samples.first()
}
