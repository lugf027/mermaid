package io.lugf027.github.mermaid.core.diagrams.er

import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.db.CommonDb
import io.lugf027.github.mermaid.core.types.DiagramDB

/**
 * ER 实体关系图数据存储层。
 */
class ErDb : CommonDb() {

    private val entities = mutableMapOf<String, ErEntity>()
    private val relationships = mutableListOf<ErRelationship>()

    fun addEntity(id: String, alias: String? = null) {
        if (!entities.containsKey(id)) {
            entities[id] = ErEntity(id = id, label = alias ?: id, alias = alias)
        }
    }

    fun addAttribute(entityId: String, attr: ErAttribute) {
        entities[entityId]?.attributes?.add(attr)
    }

    fun addRelationship(rel: ErRelationship) {
        // 确保两端实体存在
        addEntity(rel.entityA)
        addEntity(rel.entityB)
        relationships.add(rel)
    }

    fun getEntities(): Map<String, ErEntity> = entities
    fun getRelationships(): List<ErRelationship> = relationships

    override fun clear() {
        super.clear()
        entities.clear()
        relationships.clear()
    }

}
