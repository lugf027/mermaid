package io.lugf027.github.mermaid.core.diagram.requirement

import io.lugf027.github.mermaid.core.diagram.DiagramDB
import io.lugf027.github.mermaid.core.diagram.DiagramParser

/**
 * 需求图解析器 - 对标 mermaid-js requirementDiagram.jison
 *
 * 语法：
 *   requirementDiagram
 *   requirement "Name" { id: X  text: Y  risk: low  verifyMethod: test }
 *   element "Name" { type: X  docref: Y }
 *   SrcName - satisfies -> DstName
 *   SrcName <- contains - DstName
 */
class RequirementParser : DiagramParser {

    private val RE_START = Regex("^\\s*requirementDiagram\\s*$", RegexOption.IGNORE_CASE)
    private val RE_REQ = Regex("^\\s*(requirement|functionalRequirement|interfaceRequirement|performanceRequirement|physicalRequirement|designConstraint)\\s+\"([^\"]+)\"\\s*\\{?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_ELEM = Regex("^\\s*element\\s+\"([^\"]+)\"\\s*\\{?\\s*$", RegexOption.IGNORE_CASE)
    private val RE_CLOSE = Regex("^\\s*}\\s*$")
    private val RE_ID = Regex("^\\s*id\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_TEXT = Regex("^\\s*text\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_RISK = Regex("^\\s*risk\\s*:\\s*(low|medium|high)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_VERIFY = Regex("^\\s*verifyMethod\\s*:\\s*(analysis|demonstration|inspection|test)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_TYPE = Regex("^\\s*type\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_DOCREF = Regex("^\\s*docref\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    // src - relationship -> dst
    private val RE_REL_RIGHT = Regex("^\\s*(\\S+)\\s*-\\s*(contains|copies|derives|satisfies|verifies|refines|traces)\\s*->\\s*(\\S+)\\s*$", RegexOption.IGNORE_CASE)
    // src <- relationship - dst
    private val RE_REL_LEFT = Regex("^\\s*(\\S+)\\s*<-\\s*(contains|copies|derives|satisfies|verifies|refines|traces)\\s*-\\s*(\\S+)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_DIRECTION = Regex("^\\s*direction\\s+(TB|BT|RL|LR)\\s*$", RegexOption.IGNORE_CASE)
    private val RE_ACC_TITLE = Regex("^\\s*accTitle\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val RE_ACC_DESCR = Regex("^\\s*accDescr\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    override fun parse(text: String, db: DiagramDB) {
        val rdb = db as RequirementDb
        rdb.clear()

        val lines = text.lines()
        var started = false
        var inBlock = false // inside { ... }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) continue

            if (!started) {
                if (RE_START.matches(trimmed)) started = true
                continue
            }

            // 关闭块
            if (inBlock && RE_CLOSE.matches(trimmed)) {
                inBlock = false
                continue
            }

            // 块内属性
            if (inBlock) {
                rdb.latestRequirement?.let { req ->
                    RE_ID.find(trimmed)?.let { req.requirementId = it.groupValues[1].trim(); return@let }
                    RE_TEXT.find(trimmed)?.let { req.text = it.groupValues[1].trim(); return@let }
                    RE_RISK.find(trimmed)?.let { m ->
                        req.risk = when (m.groupValues[1].lowercase()) {
                            "low" -> RequirementDb.RiskLevel.LOW
                            "medium" -> RequirementDb.RiskLevel.MEDIUM
                            "high" -> RequirementDb.RiskLevel.HIGH
                            else -> null
                        }
                    }
                    RE_VERIFY.find(trimmed)?.let { m ->
                        req.verifyMethod = when (m.groupValues[1].lowercase()) {
                            "analysis" -> RequirementDb.VerifyMethod.ANALYSIS
                            "demonstration" -> RequirementDb.VerifyMethod.DEMONSTRATION
                            "inspection" -> RequirementDb.VerifyMethod.INSPECTION
                            "test" -> RequirementDb.VerifyMethod.TEST
                            else -> null
                        }
                    }
                }
                rdb.latestElement?.let { elem ->
                    RE_TYPE.find(trimmed)?.let { elem.type = it.groupValues[1].trim(); return@let }
                    RE_DOCREF.find(trimmed)?.let { elem.docRef = it.groupValues[1].trim(); return@let }
                }
                continue
            }

            // 需求定义
            RE_REQ.find(trimmed)?.let { m ->
                val typeStr = m.groupValues[1]
                val name = m.groupValues[2]
                val reqType = parseRequirementType(typeStr)
                rdb.addRequirement(name, reqType)
                if (trimmed.endsWith("{")) inBlock = true
                return@let
            }?.also { continue }

            // 元素定义
            RE_ELEM.find(trimmed)?.let { m ->
                rdb.addElement(m.groupValues[1])
                if (trimmed.endsWith("{")) inBlock = true
                return@let
            }?.also { continue }

            // 关系（右箭头）
            RE_REL_RIGHT.find(trimmed)?.let { m ->
                val relType = parseRelationType(m.groupValues[2])
                rdb.addRelation(relType, m.groupValues[1], m.groupValues[3])
                return@let
            }?.also { continue }

            // 关系（左箭头）
            RE_REL_LEFT.find(trimmed)?.let { m ->
                val relType = parseRelationType(m.groupValues[2])
                rdb.addRelation(relType, m.groupValues[3], m.groupValues[1])
                return@let
            }?.also { continue }

            // 方向
            RE_DIRECTION.find(trimmed)?.let { m ->
                rdb.setDirection(m.groupValues[1])
                return@let
            }?.also { continue }

            // accTitle/accDescr
            RE_ACC_TITLE.find(trimmed)?.let { rdb.setAccTitle(it.groupValues[1].trim()); return@let }?.also { continue }
            RE_ACC_DESCR.find(trimmed)?.let { rdb.setAccDescription(it.groupValues[1].trim()); return@let }
        }
    }

    private fun parseRequirementType(s: String): RequirementDb.RequirementType = when (s.lowercase()) {
        "requirement" -> RequirementDb.RequirementType.REQUIREMENT
        "functionalrequirement" -> RequirementDb.RequirementType.FUNCTIONAL
        "interfacerequirement" -> RequirementDb.RequirementType.INTERFACE
        "performancerequirement" -> RequirementDb.RequirementType.PERFORMANCE
        "physicalrequirement" -> RequirementDb.RequirementType.PHYSICAL
        "designconstraint" -> RequirementDb.RequirementType.DESIGN_CONSTRAINT
        else -> RequirementDb.RequirementType.REQUIREMENT
    }

    private fun parseRelationType(s: String): RequirementDb.RelationType = when (s.lowercase()) {
        "contains" -> RequirementDb.RelationType.CONTAINS
        "copies" -> RequirementDb.RelationType.COPIES
        "derives" -> RequirementDb.RelationType.DERIVES
        "satisfies" -> RequirementDb.RelationType.SATISFIES
        "verifies" -> RequirementDb.RelationType.VERIFIES
        "refines" -> RequirementDb.RelationType.REFINES
        "traces" -> RequirementDb.RelationType.TRACES
        else -> RequirementDb.RelationType.TRACES
    }
}
