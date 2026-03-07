package io.lugf027.github.mermaid.core.db

import io.lugf027.github.mermaid.core.types.DiagramDB

/**
 * 公共 DB 基类实现。
 * 提供 title/accTitle/accDescription 等通用字段的存取。
 * 对应 mermaid-js 的 commonDb.js。
 * 各图表 DB 继承此类。
 */
abstract class CommonDb : DiagramDB {
    private var _diagramTitle: String = ""
    private var _accTitle: String = ""
    private var _accDescription: String = ""

    override fun clear() {
        _diagramTitle = ""
        _accTitle = ""
        _accDescription = ""
    }

    override fun setDiagramTitle(title: String) {
        _diagramTitle = title
    }

    override fun getDiagramTitle(): String = _diagramTitle

    override fun setAccTitle(title: String) {
        _accTitle = title
    }

    override fun getAccTitle(): String = _accTitle

    override fun setAccDescription(desc: String) {
        _accDescription = desc
    }

    override fun getAccDescription(): String = _accDescription
}
