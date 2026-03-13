package io.lugf027.github.mermaid.core.util

/**
 * 唯一 ID 生成器 - 对标 mermaid-js uid.ts
 */
object IdGenerator {
    private var counter = 0L

    /**
     * 生成唯一 ID
     *
     * @param prefix ID 前缀
     * @return 唯一 ID 字符串
     */
    fun next(prefix: String = "id"): String {
        counter++
        return "${prefix}-${counter}"
    }

    /**
     * 重置计数器（用于测试）
     */
    fun reset() {
        counter = 0
    }

    /**
     * 生成确定性 ID（基于种子）
     */
    fun deterministicId(seed: String, index: Int): String {
        return "${seed}-${index}"
    }
}
