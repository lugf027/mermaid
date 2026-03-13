package io.lugf027.github.mermaid.eval

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 评估报告生成器 — 生成终端友好的对比报告和可选的 JSON 结果文件
 *
 * 支持动态评分维度：不同图表类型可能有不同的评分维度，
 * 报告会自动适配每个用例的维度。
 */
object EvalReport {

    /**
     * 打印终端报告
     */
    fun printReport(results: List<CaseResult>, threshold: Double = 0.95) {
        println()
        println("═".repeat(100))
        println(" SVG Evaluation Report — mermaid-kmp vs mermaid-js")
        println(" ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
        println("═".repeat(100))
        println()

        // ── 按图表类型分组汇总 ────────────────────────
        val byType = results.groupBy { it.score?.diagramType ?: "unknown" }

        var passCount = 0
        var failCount = 0
        var errorCount = 0
        var totalScore = 0.0
        var validCount = 0

        for ((diagramType, cases) in byType) {
            println()
            println("┌─ ${diagramType.uppercase()} ${"─".repeat(maxOf(0, 90 - diagramType.length))}")
            println("│")

            // 收集此类型的所有维度名称（保持有序）
            val allDimNames = linkedSetOf<String>()
            for (r in cases) {
                r.score?.dimensions?.forEach { allDimNames += it.name }
            }

            // 构造表头
            val dimHeaders = allDimNames.joinToString(" │ ") { "%8s".format(truncate(it, 8)) }
            val header = "│ %-28s │ %6s │ %s │ %6s".format("Case", "Total", dimHeaders, "Status")
            println(header)
            println("│ ${"─".repeat(header.length - 2)}")

            for (r in cases) {
                if (r.error != null) {
                    errorCount++
                    val errorDims = allDimNames.joinToString(" │ ") { "%8s".format("-") }
                    println("│ %-28s │ %6s │ %s │ %6s".format(
                        truncate(r.name, 28), "ERROR", errorDims, "💥"
                    ))
                    continue
                }

                val s = r.score!!
                totalScore += s.total
                validCount++
                val passed = s.total >= threshold
                if (passed) passCount++ else failCount++

                val dims = s.dimensions.associateBy { it.name }
                val emoji = when {
                    s.total >= 0.99 -> "🏆"
                    s.total >= threshold -> "✅"
                    s.total >= 0.80 -> "⚠️"
                    else -> "❌"
                }

                val dimValues = allDimNames.joinToString(" │ ") { dimName ->
                    "%8.4f".format(dims[dimName]?.score ?: 0.0)
                }

                println("│ %-28s │ %6.4f │ %s │ %6s".format(
                    truncate(r.name, 28),
                    s.total,
                    dimValues,
                    emoji
                ))
            }
            println("│")
        }

        println()
        println("─".repeat(100))

        // ── 统计 ──────────────────────────────────────
        println()
        val avgScore = if (validCount > 0) totalScore / validCount else 0.0
        println("📊 Summary:")
        println("   Total cases:    ${results.size}")
        println("   Diagram types:  ${byType.keys.joinToString(", ")}")
        println("   Passed:         $passCount ✅")
        println("   Failed:         $failCount ❌")
        if (errorCount > 0) {
            println("   Errors:         $errorCount 💥")
        }
        println("   Avg score:      %.4f".format(avgScore))
        println("   Threshold:      %.2f".format(threshold))
        println()

        // ── 失败/警告详情 ──────────────────────────────
        val problems = results.filter { it.score != null && !it.score.passed(threshold) }
        if (problems.isNotEmpty()) {
            println("📋 Detail (failed cases):")
            println()
            for (r in problems) {
                println("   ▶ ${r.name} [${r.score!!.diagramType}] (score=%.4f)".format(r.score.total))
                for (dim in r.score.dimensions) {
                    val flag = if (dim.score < 0.95) "⚠" else " "
                    println("     $flag %-15s %.4f  (%.0f%%)  %s".format(
                        dim.name, dim.score, dim.weight * 100, dim.detail
                    ))
                }
                println()
            }
        }

        // ── 最终结论 ──────────────────────────────────
        println("═".repeat(100))
        if (failCount == 0 && errorCount == 0) {
            println("🎉 ALL PASSED! ($passCount/$passCount cases, avg=%.4f)".format(avgScore))
        } else {
            println("💥 REGRESSION DETECTED: $failCount failed, $errorCount errors out of ${results.size} cases")
        }
        println("═".repeat(100))
    }

    /**
     * 将结果写入 JSON 文件（供 CI/TDD 消费）
     */
    fun writeJson(results: List<CaseResult>, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("""  "timestamp": "${LocalDateTime.now()}",""")
        sb.appendLine("""  "totalCases": ${results.size},""")

        val validResults = results.filter { it.score != null }
        val avgScore = if (validResults.isNotEmpty()) validResults.sumOf { it.score!!.total } / validResults.size else 0.0
        sb.appendLine("""  "avgScore": ${"%.6f".format(avgScore)},""")
        sb.appendLine("""  "passed": ${validResults.count { it.passed }},""")
        sb.appendLine("""  "failed": ${validResults.count { !it.passed }},""")
        sb.appendLine("""  "errors": ${results.count { it.error != null }},""")

        // 按图表类型统计
        val byType = validResults.groupBy { it.score!!.diagramType }
        sb.appendLine("""  "byDiagramType": {""")
        for ((idx, entry) in byType.entries.withIndex()) {
            val (type, cases) = entry
            val typeAvg = cases.sumOf { it.score!!.total } / cases.size
            val comma = if (idx < byType.size - 1) "," else ""
            sb.appendLine("""    "$type": {"count": ${cases.size}, "avgScore": ${"%.6f".format(typeAvg)}, "passed": ${cases.count { it.passed }}}$comma""")
        }
        sb.appendLine("  },")

        sb.appendLine("""  "cases": [""")

        for ((idx, r) in results.withIndex()) {
            sb.appendLine("    {")
            sb.appendLine("""      "name": "${r.name}",""")
            sb.appendLine("""      "mmd": "${r.mmdFile.absolutePath}",""")
            if (r.error != null) {
                sb.appendLine("""      "error": "${r.error}"""")
            } else {
                val s = r.score!!
                sb.appendLine("""      "diagramType": "${s.diagramType}",""")
                sb.appendLine("""      "score": ${"%.6f".format(s.total)},""")
                sb.appendLine("""      "passed": ${r.passed},""")
                sb.appendLine("""      "dimensions": {""")
                for ((di, dim) in s.dimensions.withIndex()) {
                    val comma = if (di < s.dimensions.size - 1) "," else ""
                    sb.appendLine("""        "${dim.name}": {"score": ${"%.6f".format(dim.score)}, "weight": ${dim.weight}, "detail": "${escapeJson(dim.detail)}"}$comma""")
                }
                sb.appendLine("      }")
            }
            val comma = if (idx < results.size - 1) "," else ""
            sb.appendLine("    }$comma")
        }

        sb.appendLine("  ]")
        sb.appendLine("}")
        outputFile.writeText(sb.toString(), Charsets.UTF_8)
        println("\n📄 JSON report written to: ${outputFile.absolutePath}")
    }

    private fun truncate(s: String, maxLen: Int): String {
        return if (s.length <= maxLen) s else s.take(maxLen - 2) + ".."
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }
}
