package com.startrace.core.engine

/**
 * 节点关系计算器 — 计算两个节点之间的标签相似度和受力权重。
 *
 * 根据 domainTag / formTag / storyIds 计算吸引力加成系数：
 * - 同 domainTag → +1.0（强聚合）
 * - 同 formTag → +0.3（弱靠近）
 * - 共享 storyId → +2.0（锚定到故事节点周围）
 *
 * 纯函数，无副作用，线程安全。
 */
object NodeRelation {

    /**
     * 计算两个节点之间的吸引力权重。
     *
     * @return 0.0 表示无额外吸引（仅受全局斥力），值越大吸引力越强
     */
    fun attractionWeight(a: GraphNode, b: GraphNode): Float {
        // 两个固定节点之间不产生额外引力
        if (a.isFixed && b.isFixed) return 0f

        var weight = 0f

        // domainTag 相同 → 强引力，同一领域的碎片聚在一起
        if (a.domainTag.isNotBlank() && a.domainTag == b.domainTag) {
            weight += 1.0f
        }

        // formTag 相同 → 弱引力，相同形态的碎片稍微靠近
        if (a.formTag != null && b.formTag != null && a.formTag == b.formTag) {
            weight += 0.3f
        }

        // 共享故事 → 强锚定（多对多）
        val sharedStories = a.storyIds.intersect(b.storyIds)
        if (sharedStories.isNotEmpty()) {
            weight += 2.0f
        }

        // 碎片对其归属的故事节点产生额外引力
        if (a.storyIds.contains(b.id) && b.isFixed) {
            weight += 2.0f
        }
        if (b.storyIds.contains(a.id) && a.isFixed) {
            weight += 2.0f
        }

        // 不同类型节点之间有天然引力（故事和它的碎片走得更近）
        if (a.nodeType != b.nodeType && (a.storyIds.contains(b.id) || b.storyIds.contains(a.id))) {
            weight += 1.5f
        }

        return weight
    }

    /**
     * 判断两个节点是否共享至少一个标签维度。
     */
    fun sharesAnyTag(a: GraphNode, b: GraphNode): Boolean {
        return (a.domainTag.isNotBlank() && a.domainTag == b.domainTag) ||
                (a.formTag != null && b.formTag != null && a.formTag == b.formTag) ||
                a.storyIds.intersect(b.storyIds).isNotEmpty()
    }
}
