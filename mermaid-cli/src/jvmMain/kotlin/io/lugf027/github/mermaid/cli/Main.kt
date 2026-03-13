package io.lugf027.github.mermaid.cli

import io.lugf027.github.mermaid.core.MermaidApi
import io.lugf027.github.mermaid.core.config.MermaidConfig
import io.lugf027.github.mermaid.core.rendering.svg.SvgSerializer
import java.io.File
import kotlin.system.exitProcess

/**
 * mermaid-kmp CLI - 对标 @mermaid-js/mermaid-cli (mmdc)
 *
 * 将 Mermaid .mmd 图表文件转换为 SVG 文件。
 *
 * 用法：
 *   mermaid-cli -i <input.mmd> [-o <output.svg>] [--theme <theme>] [--indent]
 *
 * 示例：
 *   mermaid-cli -i diagram.mmd -o diagram.svg
 *   mermaid-cli -i diagram.mmd                    # 输出到 diagram.svg（自动推导）
 *   mermaid-cli -i diagram.mmd --theme dark
 *   mermaid-cli -i diagram.mmd --indent            # 格式化 SVG 输出
 */
fun main(args: Array<String>) {
    val config = parseArgs(args)

    // 读取输入文件
    val inputFile = File(config.inputPath)
    if (!inputFile.exists()) {
        System.err.println("Error: Input file not found: ${config.inputPath}")
        exitProcess(1)
    }
    if (!inputFile.isFile) {
        System.err.println("Error: Input path is not a file: ${config.inputPath}")
        exitProcess(1)
    }

    val mermaidText = inputFile.readText(Charsets.UTF_8)
    if (mermaidText.isBlank()) {
        System.err.println("Error: Input file is empty: ${config.inputPath}")
        exitProcess(1)
    }

    // 推导输出路径（默认与输入同目录，扩展名改为 .svg）
    val outputPath = config.outputPath ?: run {
        val name = inputFile.nameWithoutExtension
        val parent = inputFile.parentFile?.path ?: "."
        "$parent${File.separator}${name}.svg"
    }
    val outputFile = File(outputPath)

    // 初始化 MermaidApi
    val mermaidConfig = if (config.theme != null) {
        MermaidConfig(theme = config.theme)
    } else {
        null
    }
    MermaidApi.initialize(mermaidConfig)

    // 渲染
    println("Rendering: ${inputFile.absolutePath}")
    try {
        val diagram = MermaidApi.parse(mermaidText)
        val svgRoot = MermaidApi.render(diagram)
        val svgString = SvgSerializer.serialize(svgRoot, config.indent)

        // 确保输出目录存在
        outputFile.parentFile?.mkdirs()

        // 写入输出文件
        outputFile.writeText(svgString, Charsets.UTF_8)
        println("Output: ${outputFile.absolutePath}")
    } catch (e: Exception) {
        System.err.println("Error: Failed to render diagram: ${e.message}")
        if (config.verbose) {
            e.printStackTrace()
        }
        exitProcess(2)
    }
}

/** CLI 配置 */
private data class CliConfig(
    val inputPath: String,
    val outputPath: String?,
    val theme: String?,
    val indent: Boolean,
    val verbose: Boolean,
)

/** 解析命令行参数 */
private fun parseArgs(args: Array<String>): CliConfig {
    var inputPath: String? = null
    var outputPath: String? = null
    var theme: String? = null
    var indent = false
    var verbose = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-i", "--input" -> {
                i++
                if (i >= args.size) {
                    System.err.println("Error: Missing value for ${args[i - 1]}")
                    exitProcess(1)
                }
                inputPath = args[i]
            }

            "-o", "--output" -> {
                i++
                if (i >= args.size) {
                    System.err.println("Error: Missing value for ${args[i - 1]}")
                    exitProcess(1)
                }
                outputPath = args[i]
            }

            "-t", "--theme" -> {
                i++
                if (i >= args.size) {
                    System.err.println("Error: Missing value for ${args[i - 1]}")
                    exitProcess(1)
                }
                theme = args[i]
            }

            "--indent" -> {
                indent = true
            }

            "-v", "--verbose" -> {
                verbose = true
            }

            "-h", "--help" -> {
                printUsage()
                exitProcess(0)
            }

            "--version" -> {
                println("mermaid-kmp CLI v1.0.0")
                exitProcess(0)
            }

            else -> {
                // 如果没有指定 -i，第一个未知参数作为输入文件
                if (inputPath == null && !args[i].startsWith("-")) {
                    inputPath = args[i]
                } else {
                    System.err.println("Error: Unknown option: ${args[i]}")
                    printUsage()
                    exitProcess(1)
                }
            }
        }
        i++
    }

    if (inputPath == null) {
        System.err.println("Error: No input file specified")
        printUsage()
        exitProcess(1)
    }

    return CliConfig(
        inputPath = inputPath,
        outputPath = outputPath,
        theme = theme,
        indent = indent,
        verbose = verbose,
    )
}

/** 打印使用说明 */
private fun printUsage() {
    println(
        """
        |
        |Usage: mermaid-cli [options] [-i] <input.mmd>
        |
        |Convert Mermaid diagram files to SVG.
        |
        |Options:
        |  -i, --input <file>     Input .mmd file (required)
        |  -o, --output <file>    Output .svg file (default: <input-name>.svg)
        |  -t, --theme <theme>    Mermaid theme (default, dark, forest, neutral)
        |  --indent               Format SVG output with indentation
        |  -v, --verbose          Show detailed error information
        |  -h, --help             Show this help message
        |  --version              Show version information
        |
        |Examples:
        |  mermaid-cli -i diagram.mmd -o output.svg
        |  mermaid-cli -i diagram.mmd --theme dark
        |  mermaid-cli diagram.mmd                      # input shorthand
        |
        """.trimMargin()
    )
}
