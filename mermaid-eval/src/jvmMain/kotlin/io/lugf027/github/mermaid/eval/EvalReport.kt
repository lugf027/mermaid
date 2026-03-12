package io.lugf027.github.mermaid.eval

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 评估报告生成器 — 生成终端友好的对比报告和可选的 JSON 结果文件
 */
object EvalReport {

    /**
     * 打印终端报告
     */
    fun printReport(results: List<CaseResult>, threshold: Double = 0.95) {
        println()
        println("═".repeat(80))
        println(" SVG Evaluation Report — mermaid-kmp vs mermaid-js")
        println(" ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
        println("═".repeat(80))
        println()

        // ── 汇总表 ────────────────────────────────────
        val header = "%-30s │ %6s │ %8s │ %8s │ %8s │ %8s │ %8s │ %6s".format(
            "Case", "Total", "viewBox", "nodes", "edges", "css", "markers", "Status"
        )
        println(header)
        println("─".repeat(header.length))

        var passCount = 0
        var failCount = 0
        var errorCount = 0
        var totalScore = 0.0

        for (r in results) {
            if (r.error != null) {
                errorCount++
                println("%-30s │ %6s │ %8s │ %8s │ %8s │ %8s │ %8s │ %6s".format(
                    truncate(r.name, 30), "ERROR", "-", "-", "-", "-", "-", "💥"
                ))
                continue
            }

            val s = r.score!!
            totalScore += s.total
            val passed = s.total >= threshold
            if (passed) passCount++ else failCount++

            val dims = s.dimensions.associateBy { it.name }
            val emoji = when {
                s.total >= 0.99 -> "🏆"
                s.total >= threshold -> "✅"
                s.total >= 0.80 -> "⚠️"
                else -> "❌"
            }

            println("%-30s │ %6.4f │ %8.4f │ %8.4f │ %8.4f │ %8.4f │ %8.4f │ %6s".format(
                truncate(r.name, 30),
                s.total,
                dims["viewBox"]?.score ?: 0.0,
                dims["nodes"]?.score ?: 0.0,
                dims["edges"]?.score ?: 0.0,
                dims["css"]?.score ?: 0.0,
                dims["markers"]?.score ?: 0.0,
                emoji
            ))
        }

        println("─".repeat(header.length))

        // ── 统计 ──────────────────────────────────────
        println()
        val validCount = passCount + failCount
        val avgScore = if (validCount > 0) totalScore / validCount else 0.0
        println("📊 Summary:")
        println("   Total cases:  ${results.size}")
        println("   Passed:       $passCount ✅")
        println("   Failed:       $failCount ❌")
        if (errorCount > 0) {
            println("   Errors:       $errorCount 💥")
        }
        println("   Avg score:    %.4f".format(avgScore))
        println("   Threshold:    %.2f".format(threshold))
        println()

        // ── 失败/警告详情 ──────────────────────────────
        val problems = results.filter { it.score != null && !it.score.passed(threshold) }
        if (problems.isNotEmpty()) {
            println("📋 Detail (failed cases):")
            println()
            for (r in problems) {
                println("   ▶ ${r.name}  (score=%.4f)".format(r.score!!.total))
                for (dim in r.score.dimensions) {
                    val flag = if (dim.score < 0.95) "⚠" else " "
                    println("     $flag %-12s %.4f  (%.0f%%)  %s".format(
                        dim.name, dim.score, dim.weight * 100, dim.detail
                    ))
                }
                println()
            }
        }

        // ── 最终结论 ──────────────────────────────────
        println("═".repeat(80))
        if (failCount == 0 && errorCount == 0) {
            println("🎉 ALL PASSED! ($passCount/$passCount cases, avg=%.4f)".format(avgScore))
        } else {
            println("💥 REGRESSION DETECTED: $failCount failed, $errorCount errors out of ${results.size} cases")
        }
        println("═".repeat(80))
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
        sb.appendLine("""  "cases": [""")

        for ((idx, r) in results.withIndex()) {
            sb.appendLine("    {")
            sb.appendLine("""      "name": "${r.name}",""")
            sb.appendLine("""      "mmd": "${r.mmdFile.absolutePath}",""")
            if (r.error != null) {
                sb.appendLine("""      "error": "${r.error}"""")
            } else {
                val s = r.score!!
                sb.appendLine("""      "score": ${"%.6f".format(s.total)},""")
                sb.appendLine("""      "passed": ${r.passed},""")
                sb.appendLine("""      "dimensions": {""")
                for ((di, dim) in s.dimensions.withIndex()) {
                    val comma = if (di < s.dimensions.size - 1) "," else ""
                    sb.appendLine("""        "${dim.name}": {"score": ${"%.6f".format(dim.score)}, "weight": ${dim.weight}, "detail": "${dim.detail}"}$comma""")
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
}
