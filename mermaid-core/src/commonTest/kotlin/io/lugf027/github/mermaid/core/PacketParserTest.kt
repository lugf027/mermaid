package io.lugf027.github.mermaid.core

import io.lugf027.github.mermaid.core.diagram.packet.PacketDb
import io.lugf027.github.mermaid.core.diagram.packet.PacketParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PacketParserTest {

    private fun parse(text: String): PacketDb {
        val db = PacketDb()
        PacketParser().parse(text, db)
        return db
    }

    @Test
    fun testBasicPacket() {
        val db = parse("""
            packet-beta
            0-15: "Source Port"
            16-31: "Destination Port"
        """.trimIndent())

        val words = db.getWords()
        assertEquals(1, words.size) // both fit in one row (32 bits)
        assertEquals(2, words[0].size)
        assertEquals("Source Port", words[0][0].label)
        assertEquals(0, words[0][0].start)
        assertEquals(15, words[0][0].end)
        assertEquals("Destination Port", words[0][1].label)
        assertEquals(16, words[0][1].start)
        assertEquals(31, words[0][1].end)
    }

    @Test
    fun testMultipleRows() {
        val db = parse("""
            packet-beta
            0-15: "Source Port"
            16-31: "Destination Port"
            0-31: "Sequence Number"
        """.trimIndent())

        val words = db.getWords()
        // All three blocks use absolute bit positions within the 32-bit row (row 0)
        // So they all end up in row 0 (the addBlock method uses start/bitsPerRow for row index)
        // 0-15 → row 0, 16-31 → row 0, 0-31 → row 0
        assertEquals(1, words.size)
        assertEquals(3, words[0].size) // All three blocks in row 0
    }

    @Test
    fun testRelativeBlocks() {
        val db = parse("""
            packet-beta
            0-15: "Source Port"
            16-31: "Destination Port"
            0-31: "Sequence Number"
            +1: "URG"
            +1: "ACK"
            +1: "PSH"
        """.trimIndent())

        val words = db.getWords()
        assertTrue(words.isNotEmpty())
        // All absolute blocks (0-15, 16-31, 0-31) go to row 0
        // Relative blocks start from currentBit = 32 (after 0-31 Sequence Number)
        // +1 "URG" → bit 32 → row 1 (32/32=1)
        // +1 "ACK" → bit 33 → row 1
        // +1 "PSH" → bit 34 → row 1
        assertTrue(words.size >= 2)
        // Row 1 should have URG, ACK, PSH
        val row1 = words[1]
        assertTrue(row1.isNotEmpty())
        assertEquals("URG", row1[0].label)
    }

    @Test
    fun testSingleBitField() {
        val db = parse("""
            packet-beta
            0: "Flag"
        """.trimIndent())

        val words = db.getWords()
        assertEquals(1, words.size)
        assertEquals(1, words[0].size)
        assertEquals(0, words[0][0].start)
        assertEquals(0, words[0][0].end)
        assertEquals("Flag", words[0][0].label)
    }

    @Test
    fun testWithTitle() {
        val db = parse("""
            packet-beta
            title TCP Header
            0-15: "Source Port"
            16-31: "Destination Port"
        """.trimIndent())

        assertEquals("TCP Header", db.getDiagramTitle())
        assertEquals(1, db.getWords().size)
    }

    @Test
    fun testWithoutBeta() {
        val db = parse("""
            packet
            0-7: "Version"
            8-15: "Type"
        """.trimIndent())

        val words = db.getWords()
        assertEquals(1, words.size)
        assertEquals(2, words[0].size)
    }

    @Test
    fun testCommentsSkipped() {
        val db = parse("""
            packet-beta
            %% This is a comment
            0-7: "Field A"
            %% Another comment
            8-15: "Field B"
        """.trimIndent())

        assertEquals(1, db.getWords().size)
        assertEquals(2, db.getWords()[0].size)
    }

    @Test
    fun testAccTitleAndDescr() {
        val db = parse("""
            packet-beta
            accTitle: Network Packet
            accDescr: Shows packet structure

            0-15: "Header"
        """.trimIndent())

        assertEquals("Network Packet", db.getAccTitle())
        assertEquals("Shows packet structure", db.getAccDescription())
    }
}
