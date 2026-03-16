package io.lugf027.github.mermaid.core.layout.elk

/**
 * 公共祖先查找工具 - 精确对标 mermaid-js find-common-ancestor.ts
 *
 * 用于在子图树中查找两个节点的最近公共祖先，
 * 以便正确处理跨子图边的 ELK 层级策略。
 */

/**
 * 子图树数据结构 - 对标 TreeData
 */
data class TreeData(
    /** 节点ID → 父节点ID 的映射 */
    val parentById: MutableMap<String, String> = mutableMapOf(),
    /** 节点ID → 子节点ID列表 的映射 */
    val childrenById: MutableMap<String, MutableList<String>> = mutableMapOf(),
)

/**
 * 查找两个节点的最近公共祖先 - 精确对标 find-common-ancestor.ts findCommonAncestor
 *
 * 算法：
 * 1. 从 id1 开始沿 parent 链向上遍历，记录所有访问过的节点
 * 2. 从 id2 开始沿 parent 链向上遍历，第一个在已访问集合中的节点就是 LCA
 * 3. 自环边（id1 == id2）特殊处理：返回其父节点
 *
 * @param id1 第一个节点 ID
 * @param id2 第二个节点 ID
 * @param treeData 子图树数据
 * @return 最近公共祖先的节点 ID，如果没找到返回 "root"
 */
fun findCommonAncestor(id1: String, id2: String, treeData: TreeData): String {
    val visited = mutableSetOf<String>()

    // 自环边特殊处理
    if (id1 == id2) {
        return treeData.parentById[id1] ?: "root"
    }

    // 从 id1 向上遍历，记录所有祖先
    var currentId: String? = id1
    while (currentId != null) {
        visited.add(currentId)
        if (currentId == id2) {
            return currentId
        }
        currentId = treeData.parentById[currentId]
    }

    // 从 id2 向上遍历，找到第一个已访问的节点
    currentId = id2
    while (currentId != null) {
        if (currentId in visited) {
            return currentId
        }
        currentId = treeData.parentById[currentId]
    }

    return "root"
}

/**
 * 从节点列表构建子图树数据 - 对标 render.ts addSubGraphs
 *
 * @param nodes 所有节点列表（包含 isGroup 标记和 parentId 关系）
 * @return 子图树数据
 */
fun buildTreeData(nodes: List<ElkNode>): TreeData {
    val treeData = TreeData()
    val subgraphs = nodes.filter { it.isGroup }

    for (subgraph in subgraphs) {
        val children = nodes.filter { it.parentId == subgraph.id }
        for (child in children) {
            treeData.parentById[child.id] = subgraph.id
            treeData.childrenById.getOrPut(subgraph.id) { mutableListOf() }.add(child.id)
        }
    }

    return treeData
}
