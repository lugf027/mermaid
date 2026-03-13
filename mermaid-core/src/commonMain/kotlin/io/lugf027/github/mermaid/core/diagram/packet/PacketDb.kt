package io.lugf027.github.mermaid.core.diagram.packet

import io.lugf027.github.mermaid.core.diagram.DiagramDB

/**
 * 数据包图数据库 - 对标 mermaid-js packet/db.ts
 *
 * 存储数据包的行（word）和块（block），每一行有多个位字段块。
 * 支持显式位范围（0-15）和相对位宽（+1）两种语法。
 */
class PacketDb : DiagramDB {

    /** 数据包块 */
    data class PacketBlock(
        val start: Int,
        val end: Int,
        val label: String
    )

    /** 一行 = 多个块 */
    typealias PacketWord = List<PacketBlock>

    // --- 内部状态 ---
    private val words = mutableListOf<PacketWord>()
    private var bitsPerRow = 32
    private var currentBit = 0

    // --- DiagramDB ---
    private var diagramTitle = ""
    private var accTitle = ""
    private var accDescription = ""

    override fun clear() {
        words.clear()
        bitsPerRow = 32
        currentBit = 0
        diagramTitle = ""
        accTitle = ""
        accDescription = ""
    }

    override fun setDiagramTitle(title: String) { diagramTitle = title }
    override fun getDiagramTitle(): String = diagramTitle
    override fun setAccTitle(title: String) { accTitle = title }
    override fun getAccTitle(): String = accTitle
    override fun setAccDescription(desc: String) { accDescription = desc }
    override fun getAccDescription(): String = accDescription

    // --- 操作 ---

    /**
     * 添加显式位范围块：start-end: "label"
     */
    fun addBlock(start: Int, end: Int, label: String) {
        val currentRow = mutableListOf<PacketBlock>()

        // 处理跨行拆分
        var s = start
        var e = end
        while (s <= e) {
            val rowStart = s % bitsPerRow
            val rowEnd = minOf(e % bitsPerRow, bitsPerRow - 1)
            val sameRow = (s / bitsPerRow) == (e / bitsPerRow)

            if (sameRow) {
                addBlockToWord(s / bitsPerRow, PacketBlock(s % bitsPerRow, e % bitsPerRow, label))
                break
            } else {
                addBlockToWord(s / bitsPerRow, PacketBlock(s % bitsPerRow, bitsPerRow - 1, label))
                s = (s / bitsPerRow + 1) * bitsPerRow
            }
        }
        currentBit = end + 1
    }

    /**
     * 添加相对位宽块：+bits: "label"
     */
    fun addRelativeBlock(bits: Int, label: String) {
        addBlock(currentBit, currentBit + bits - 1, label)
    }

    private fun addBlockToWord(rowIndex: Int, block: PacketBlock) {
        while (words.size <= rowIndex) {
            words.add(emptyList())
        }
        words[rowIndex] = words[rowIndex] + block
    }

    fun setBitsPerRow(bits: Int) { bitsPerRow = bits }

    // --- 查询 ---

    fun getWords(): List<PacketWord> = words.toList()
    fun getBitsPerRow(): Int = bitsPerRow
}
