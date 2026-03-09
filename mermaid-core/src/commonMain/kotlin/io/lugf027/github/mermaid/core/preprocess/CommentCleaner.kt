package io.lugf027.github.mermaid.core.preprocess

/**
 * 注释清理器 - 对标 mermaid-js comments.ts
 *
 * 移除 Mermaid 中的 %% 单行注释。
 */
object CommentCleaner {

    /**
     * 移除注释（%% 到行尾的内容），保留字符串内的 %%
     */
    fun removeComments(text: String): String {
        val result = StringBuilder()

        for (line in text.lines()) {
            val cleanedLine = removeLineComment(line)
            result.appendLine(cleanedLine)
        }

        // 移除最后多余的换行
        return result.toString().trimEnd('\n')
    }

    private fun removeLineComment(line: String): String {
        var i = 0
        var inString = false
        var stringChar = ' '

        while (i < line.length) {
            val ch = line[i]

            if (inString) {
                if (ch == stringChar) {
                    inString = false
                }
            } else {
                if (ch == '"' || ch == '\'') {
                    inString = true
                    stringChar = ch
                } else if (ch == '%' && i + 1 < line.length && line[i + 1] == '%') {
                    // 检查是否是指令开头 %%{
                    if (i + 2 < line.length && line[i + 2] == '{') {
                        // 跳过指令，不当作注释
                        i++
                        continue
                    }
                    // 这是注释，截断到此处
                    return line.substring(0, i).trimEnd()
                }
            }
            i++
        }

        return line
    }
}
