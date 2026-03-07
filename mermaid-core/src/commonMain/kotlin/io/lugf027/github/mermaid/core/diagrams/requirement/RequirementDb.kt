package io.lugf027.github.mermaid.core.diagrams.requirement

import io.lugf027.github.mermaid.core.db.CommonDb

data class Requirement(val name: String, val type: String = "requirement", val id: String = "", val text: String = "", val risk: String = "", val verifyMethod: String = "")
data class Element(val name: String, val type: String = "", val docRef: String = "")
data class RequirementRelation(val type: String, val src: String, val dst: String)

class RequirementDb : CommonDb() {
    private val requirements = mutableMapOf<String, Requirement>()
    private val elements = mutableMapOf<String, Element>()
    private val relations = mutableListOf<RequirementRelation>()

    fun addRequirement(r: Requirement) { requirements[r.name] = r }
    fun addElement(e: Element) { elements[e.name] = e }
    fun addRelation(r: RequirementRelation) { relations.add(r) }
    fun getRequirements() = requirements; fun getElements() = elements; fun getRelations() = relations

    override fun clear() { super.clear(); requirements.clear(); elements.clear(); relations.clear() }
}
