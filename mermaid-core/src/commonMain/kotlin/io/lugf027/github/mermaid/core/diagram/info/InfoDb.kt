package io.lugf027.github.mermaid.core.diagram.info

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * Info 图表数据库 - 对标 mermaid-js infoDb.ts
 *
 * 极简图表，仅显示 mermaid-kmp 版本信息。
 */
class InfoDb : DiagramDB {
    private var diagramTitle: String = ""
    private var accTitle: String = ""
    private var accDescr: String = ""
    private var version: String = "0.1.0"

    override fun clear() {
        diagramTitle = ""
        accTitle = ""
        accDescr = ""
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescr = desc }
    override fun getAccDescription(): String = accDescr

    fun getVersion(): String = version
}
