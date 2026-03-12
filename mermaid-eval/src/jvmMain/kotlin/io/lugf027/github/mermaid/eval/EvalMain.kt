package io.lugf027.github.mermaid.eval

import java.io.File
import kotlin.system.exitProcess

/**
 * mermaid-eval CLI — SVG 差异评估工具
 *
 * 用法:
 *   mermaid-eval [options] <input-dir>
 *
 * 遍历 <input-dir>（含子目录）下的所有 .mmd 文件，分别通过 mermaid-kmp 和 mmdc
 * 生成 SVG，然后计算差异得分并输出报告。
 */
fun main(args: Array<String>) {
    val config = parseEvalArgs(args)

    val inputDir = File(config.inputDir)
    if (!inputDir.isDirectory) {
        System.err.println("Error: 目录不存在: ${config.inputDir}")
        exitProcess(1)
    }

    val outputDir = config.outputDir?.let { File(it) }

    // 运行评估
    val results = MmdTestRunner.run(
        inputDir = inputDir,
        outputDir = outputDir,
        mmdcPath = config.mmdcPath,
        force = config.force
    )

    // 输出报告
    EvalReport.printReport(results, config.threshold)

    // 输出 JSON（如果指定）
    if (config.jsonOutput != null) {
        EvalReport.writeJson(results, File(config.jsonOutput))
    }

    // 根据结果设置退出码（用于 CI/TDD）
    val hasFailure = results.any { it.error != null || (it.score != null && !it.score.passed(config.threshold)) }
    if (hasFailure) {
        exitProcess(1)
    }
}

// ── 参数解析 ────────────────────────────────────────

private data class EvalConfig(
    val inputDir: String,
    val outputDir: String?,
    val mmdcPath: String,
    val threshold: Double,
    val force: Boolean,
    val jsonOutput: String?
)

private fun parseEvalArgs(args: Array<String>): EvalConfig {
    var inputDir: String? = null
    var outputDir: String? = null
    var mmdcPath = "mmdc"
    var threshold = 0.95
    var force = false
    var jsonOutput: String? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-d", "--dir" -> {
                i++; inputDir = args.getOrNull(i)
            }
            "-o", "--output" -> {
                i++; outputDir = args.getOrNull(i)
            }
            "--mmdc" -> {
                i++; mmdcPath = args.getOrNull(i) ?: "mmdc"
            }
            "-t", "--threshold" -> {
                i++; threshold = args.getOrNull(i)?.toDoubleOrNull() ?: 0.95
            }
            "-f", "--force" -> {
                force = true
            }
            "--json" -> {
                i++; jsonOutput = args.getOrNull(i)
            }
            "-h", "--help" -> {
                printEvalUsage(); exitProcess(0)
            }
            else -> {
                if (inputDir == null && !args[i].startsWith("-")) {
                    inputDir = args[i]
                } else {
                    System.err.println("Error: Unknown option: ${args[i]}")
                    printEvalUsage(); exitProcess(1)
                }
            }
        }
        i++
    }

    if (inputDir == null) {
        System.err.println("Error: 未指定输入目录")
        printEvalUsage(); exitProcess(1)
    }

    return EvalConfig(inputDir, outputDir, mmdcPath, threshold, force, jsonOutput)
}

private fun printEvalUsage() {
    println("""
        |
        |Usage: mermaid-eval [options] <input-dir>
        |
        |Evaluate SVG rendering difference between mermaid-kmp and mermaid-js.
        |
        |Options:
        |  -d, --dir <path>        Input directory containing .mmd files (required)
        |  -o, --output <path>     Output directory for generated SVGs (default: same as input)
        |  --mmdc <path>           Path to mmdc executable (default: mmdc)
        |  -t, --threshold <val>   Pass/fail threshold, 0.0-1.0 (default: 0.95)
        |  -f, --force             Force regenerate SVGs even if they already exist
        |  --json <path>           Write JSON report to file
        |  -h, --help              Show this help message
        |
        |Examples:
        |  mermaid-eval ./tests
        |  mermaid-eval -d ./tests -f --json report.json
        |  mermaid-eval -d ./tests -t 0.98 --mmdc /usr/local/bin/mmdc
        |
    """.trimMargin())
}
