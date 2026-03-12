package io.lugf027.github.mermaid.eval

import io.lugf027.github.mermaid.core.MermaidApi
import io.lugf027.github.mermaid.core.rendering.svg.SvgSerializer
import java.io.File

/**
 * MMD 测试运行器 — 遍历目录下所有 .mmd 文件，生成 KMP/JS SVG 并调用评分器
 */
object MmdTestRunner {

    /**
     * 运行评估
     *
     * @param inputDir  包含 .mmd 文件的目录（含子目录）
     * @param outputDir SVG 输出目录（null 则与 .mmd 同目录）
     * @param mmdcPath  mmdc 可执行文件路径
     * @param force     是否强制重新生成已存在的 SVG
     * @return 每个 .mmd 文件的评估结果列表
     */
    fun run(
        inputDir: File,
        outputDir: File? = null,
        mmdcPath: String = "mmdc",
        force: Boolean = false
    ): List<CaseResult> {
        require(inputDir.isDirectory) { "inputDir 不是目录: $inputDir" }

        // 递归收集所有 .mmd 文件
        val mmdFiles = inputDir.walkTopDown()
            .filter { it.isFile && it.extension == "mmd" }
            .sortedBy { it.name }
            .toList()

        if (mmdFiles.isEmpty()) {
            println("⚠️  在 $inputDir 下未找到 .mmd 文件")
            return emptyList()
        }

        println("📁 扫描到 ${mmdFiles.size} 个 .mmd 文件")
        println()

        // 初始化 KMP 引擎（只需一次）
        MermaidApi.initialize()

        return mmdFiles.map { mmdFile ->
            processSingleCase(mmdFile, outputDir, mmdcPath, force)
        }
    }

    private fun processSingleCase(
        mmdFile: File,
        outputDir: File?,
        mmdcPath: String,
        force: Boolean
    ): CaseResult {
        val baseName = mmdFile.nameWithoutExtension
        val targetDir = outputDir ?: mmdFile.parentFile
        val kmpSvg = File(targetDir, "${baseName}_kmp.svg")
        val jsSvg = File(targetDir, "${baseName}_js.svg")

        println("── $baseName ──")

        // ① 生成 KMP SVG
        val kmpOk = generateKmpSvg(mmdFile, kmpSvg, force)

        // ② 生成 JS SVG（通过 mmdc）
        val jsOk = generateJsSvg(mmdFile, jsSvg, mmdcPath, force)

        if (!kmpOk || !jsOk) {
            val reason = buildString {
                if (!kmpOk) append("KMP generation failed; ")
                if (!jsOk) append("JS (mmdc) generation failed; ")
            }
            println("   ❌ $reason")
            return CaseResult(
                name = baseName,
                mmdFile = mmdFile,
                kmpSvgFile = kmpSvg,
                jsSvgFile = jsSvg,
                score = null,
                error = reason.trim()
            )
        }

        // ③ 评分
        val jsSvgContent = jsSvg.readText(Charsets.UTF_8)
        val kmpSvgContent = kmpSvg.readText(Charsets.UTF_8)
        val score = SvgScorer.score(jsSvgContent, kmpSvgContent)

        val emoji = when {
            score.total >= 0.99 -> "🏆"
            score.total >= 0.95 -> "✅"
            score.total >= 0.80 -> "⚠️"
            else -> "❌"
        }
        println("   $emoji score = %.4f".format(score.total))

        return CaseResult(
            name = baseName,
            mmdFile = mmdFile,
            kmpSvgFile = kmpSvg,
            jsSvgFile = jsSvg,
            score = score,
            error = null
        )
    }

    // ── KMP SVG 生成 ───────────────────────────────────

    private fun generateKmpSvg(mmdFile: File, outFile: File, force: Boolean): Boolean {
        if (!force && outFile.exists() && outFile.length() > 0) {
            return true // 已存在，跳过
        }
        return try {
            val text = mmdFile.readText(Charsets.UTF_8)
            // 每次渲染前 reset IdGenerator 保证 id 一致
            io.lugf027.github.mermaid.core.util.IdGenerator.reset()
            val diagram = MermaidApi.parse(text)
            val svgRoot = MermaidApi.render(diagram)
            val svgString = SvgSerializer.serialize(svgRoot)
            outFile.parentFile?.mkdirs()
            outFile.writeText(svgString, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            System.err.println("   KMP error: ${e.message}")
            false
        }
    }

    // ── JS SVG 生成（mmdc）──────────────────────────────

    private fun generateJsSvg(mmdFile: File, outFile: File, mmdcPath: String, force: Boolean): Boolean {
        if (!force && outFile.exists() && outFile.length() > 0) {
            return true // 已存在，跳过
        }
        return try {
            outFile.parentFile?.mkdirs()
            val process = ProcessBuilder(
                mmdcPath,
                "-q",
                "-i", mmdFile.absolutePath,
                "-o", outFile.absolutePath
            )
                .redirectErrorStream(true)
                .start()
            // 先读完输出流再 waitFor，防止管道阻塞
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                System.err.println("   mmdc exit=$exitCode: $output")
                false
            } else {
                outFile.exists() && outFile.length() > 0
            }
        } catch (e: Exception) {
            System.err.println("   mmdc error: ${e.message}")
            false
        }
    }
}

/** 单个用例的评估结果 */
data class CaseResult(
    val name: String,
    val mmdFile: File,
    val kmpSvgFile: File,
    val jsSvgFile: File,
    val score: ScoreResult?,
    val error: String?
) {
    val passed: Boolean get() = score?.passed() == true
}
