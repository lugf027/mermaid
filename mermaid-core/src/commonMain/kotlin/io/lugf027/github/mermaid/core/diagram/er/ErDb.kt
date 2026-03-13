package io.lugf027.github.mermaid.core.diagram.er

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.layout.LayoutData
import io.lugf027.github.mermaid.core.layout.LayoutEdge
import io.lugf027.github.mermaid.core.layout.LayoutNode
import io.lugf027.github.mermaid.core.util.Logger

/**
 * ER 图数据库 - 对标 mermaid-js erDb.ts
 *
 * 管理实体(Entity)、属性(Attribute)和关系(Relationship)。
 */
class ErDb : DiagramDB {

    private val log = Logger("ErDb")

    // ── 状态 ──────────────────────────────────────────
    private val entities: LinkedHashMap<String, Entity> = linkedMapOf()
    private val relationships: MutableList<Relationship> = mutableListOf()

    // ── DiagramDB 基础接口 ────────────────────────────
    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescription: String = ""
    private var direction: String = "TB"

    override fun clear() {
        entities.clear()
        relationships.clear()
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
        direction = "TB"
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription
    override fun getDirection(): String = direction
    override fun setDirection(direction: String) { this.direction = direction }

    // ── 实体管理 ──────────────────────────────────────

    fun addEntity(name: String, alias: String? = null) {
        if (!entities.containsKey(name)) {
            entities[name] = Entity(
                name = name,
                alias = alias,
                attributes = mutableListOf()
            )
            log.debug("Added entity: $name")
        } else if (alias != null) {
            entities[name]?.alias = alias
        }
    }

    fun addAttribute(entityName: String, attribute: Attribute) {
        addEntity(entityName)
        entities[entityName]?.attributes?.add(attribute)
    }

    fun getEntities(): Map<String, Entity> = entities

    // ── 关系管理 ──────────────────────────────────────

    fun addRelationship(
        entityA: String,
        entityB: String,
        relSpec: RelSpec,
        roleLabel: String = ""
    ) {
        addEntity(entityA)
        addEntity(entityB)
        relationships.add(Relationship(
            entityA = entityA,
            entityB = entityB,
            relSpec = relSpec,
            roleLabel = roleLabel
        ))
        log.debug("Added relationship: $entityA ${relSpec.cardA} -- ${relSpec.cardB} $entityB : $roleLabel")
    }

    fun getRelationships(): List<Relationship> = relationships

    // ── LayoutData 构建 ──────────────────────────────

    fun getData(config: MermaidConfig): LayoutData {
        val erConfig = config.er
        val nodeSpacing = erConfig?.nodeSpacing ?: 140
        val rankSpacing = erConfig?.rankSpacing ?: 80
        val diagramPadding = erConfig?.diagramPadding ?: 20

        val nodes = mutableListOf<LayoutNode>()
        val edges = mutableListOf<LayoutEdge>()

        // 构建实体节点
        for ((idx, entry) in entities.entries.withIndex()) {
            val (name, entity) = entry
            val label = entity.alias ?: name
            nodes.add(LayoutNode(
                id = name,
                label = label,
                shape = "erBox",
                isGroup = false,
                width = 0.0,
                height = 0.0,
                domId = "entity-${name}-${idx}",
                cssClasses = "default"
            ))
        }

        // 构建关系边
        for ((idx, rel) in relationships.withIndex()) {
            edges.add(LayoutEdge(
                id = "e${idx}-${rel.entityA}-${rel.entityB}",
                start = rel.entityA,
                end = rel.entityB,
                label = rel.roleLabel,
                arrowTypeStart = rel.relSpec.cardB.lowercase(),  // 注意交叉
                arrowTypeEnd = rel.relSpec.cardA.lowercase(),
                pattern = if (rel.relSpec.relType == "IDENTIFYING") "solid" else "dashed",
                stroke = "normal",
                thickness = "normal",
                labelpos = "c"
            ))
        }

        return LayoutData(
            nodes = nodes,
            edges = edges,
            config = config,
            direction = direction,
            nodeSpacing = nodeSpacing,
            rankSpacing = rankSpacing,
            diagramPadding = diagramPadding,
            markers = listOf("only_one", "zero_or_one", "one_or_more", "zero_or_more")
        )
    }
}

// ════════════════════════════════════════════════════════
//  数据模型
// ════════════════════════════════════════════════════════

/**
 * 实体数据类
 */
data class Entity(
    val name: String,
    var alias: String? = null,
    val attributes: MutableList<Attribute> = mutableListOf()
)

/**
 * 属性数据类
 */
data class Attribute(
    val type: String,        // 数据类型（如 "string", "int"）
    val name: String,        // 属性名
    val keys: List<String> = emptyList(),   // 键类型: PK, FK, UK
    val comment: String = "" // 注释
)

/**
 * 关系数据类
 */
data class Relationship(
    val entityA: String,
    val entityB: String,
    val relSpec: RelSpec,
    val roleLabel: String = ""
)

/**
 * 关系规格
 */
data class RelSpec(
    val cardA: String,      // 实体A的基数
    val cardB: String,      // 实体B的基数
    val relType: String     // "IDENTIFYING" 或 "NON_IDENTIFYING"
)

/**
 * 基数类型常量
 */
object Cardinality {
    const val ZERO_OR_ONE = "ZERO_OR_ONE"
    const val ZERO_OR_MORE = "ZERO_OR_MORE"
    const val ONE_OR_MORE = "ONE_OR_MORE"
    const val ONLY_ONE = "ONLY_ONE"
    const val MD_PARENT = "MD_PARENT"
}

/**
 * 识别类型常量
 */
object Identification {
    const val IDENTIFYING = "IDENTIFYING"
    const val NON_IDENTIFYING = "NON_IDENTIFYING"
}
