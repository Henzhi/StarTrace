package com.startrace.core.engine

import kotlin.math.*

/**
 * 力导向图布局引擎。
 *
 * 模拟物理力（斥力 + 引力 + 中心力 + 阻尼）在二维平面上排列节点，
 * 使相关节点聚合、无关节点分离，形成自然的星图布局。
 *
 * ## 物理模型
 *
 * ### 库伦斥力（全连通）
 * 任意两个节点之间都存在斥力：F = k_repel / d²
 *
 * ### 标签引力（相似节点）
 * 同 domainTag/formTag/storyId 的节点间产生额外引力：F = k_attr * weight
 *
 * ### 中心力
 * 所有节点被拉向画布中心，防止漂移出界。
 *
 * ### 阻尼冷却
 * 每轮迭代阻尼从 initialDamping 线性增长到 finalDamping，
 * 模拟退火：早期大步伐探索布局，后期微调收敛。
 *
 * ## 使用方式
 *
 * ```kotlin
 * val engine = ForceDirectedEngine(ForceConfig())
 * fragments.forEach { f -> engine.addNode(GraphNode(id = f.id, domainTag = f.domainTag, ...)) }
 * engine.simulate(iterations = 50)
 * val positions = engine.snapshot()
 * ```
 *
 * 纯 Kotlin 实现，无 Android 依赖，可独立单元测试。
 */
class ForceDirectedEngine(
    private val config: ForceConfig = ForceConfig()
) {
    /** 所有节点 */
    private val _nodes = mutableListOf<GraphNode>()
    val nodes: List<GraphNode> get() = _nodes.toList()

    /** 当前迭代轮次 */
    var iteration: Int = 0
        private set

    /** 是否已收敛 */
    var isConverged: Boolean = false
        private set

    /** 收敛时的迭代轮次 */
    var convergedAt: Int = -1
        private set

    // ── 添加/移除节点 ──────────────────────────────────

    /**
     * 添加节点到图中。
     * 固定节点的初始位置保持不变。
     */
    fun addNode(node: GraphNode) {
        _nodes.add(node)
    }

    /** 批量添加节点 */
    fun addNodes(newNodes: List<GraphNode>) {
        _nodes.addAll(newNodes)
    }

    /** 移除指定节点 */
    fun removeNode(id: String) {
        _nodes.removeAll { it.id == id }
    }

    /** 清空所有节点 */
    fun clear() {
        _nodes.clear()
        iteration = 0
        isConverged = false
        convergedAt = -1
    }

    /** 用户手动移动节点（拖拽）。仅更新位置，不影响物理模拟状态。 */
    fun moveNode(id: String, dx: Float, dy: Float) {
        val idx = _nodes.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val node = _nodes[idx]
            _nodes[idx] = node.copy(x = node.x + dx, y = node.y + dy)
        }
    }

    // ── 模拟 ──────────────────────────────────────────

    /**
     * 完整模拟流程。
     *
     * @param maxIterations 最大迭代轮次（默认 50）
     * @return 节点的最终位置快照
     */
    fun simulate(maxIterations: Int = 50): List<GraphNode> {
        initializePositions()
        isConverged = false
        convergedAt = -1

        for (i in 0 until maxIterations) {
            iteration = i + 1

            // 线性冷却：阻尼从 initial → final
            val progress = i.toFloat() / maxIterations
            val damping = config.initialDamping +
                    (config.finalDamping - config.initialDamping) * progress

            step(damping)

            // 收敛检测
            if (checkConvergence()) {
                convergedAt = iteration
                isConverged = true
                break
            }
        }

        // 如果达到最大迭代仍未收敛
        if (!isConverged) {
            convergedAt = maxIterations
            isConverged = true
        }

        return snapshot()
    }

    /**
     * 单步迭代。
     *
     * @param damping 当前阻尼系数（0-1，越小移动越快）
     */
    fun step(damping: Float) {
        val n = _nodes.size
        if (n <= 1) return

        // ── 第一阶段：计算合力（斥力 + 引力）───────────
        val forces = Array(n) { FloatArray(2) } // [fx, fy] per node

        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val a = _nodes[i]
                val b = _nodes[j]

                var dx = b.x - a.x
                var dy = b.y - a.y
                var dist = sqrt(dx * dx + dy * dy)
                if (dist < config.minDistance) {
                    dist = config.minDistance
                    dx = if (dx == 0f) 0.1f else dx
                    dy = if (dy == 0f) 0.1f else dy
                    dist = sqrt(dx * dx + dy * dy)
                }

                val dirX = dx / dist
                val dirY = dy / dist

                // ── 库伦斥力 ───────────────────────────
                val repelForce = config.repulsionStrength / (dist * dist)

                // ── 标签引力 ───────────────────────────
                val weight = NodeRelation.attractionWeight(a, b)
                val attrForce = if (weight > 0f) {
                    weight * config.attractionStrength * dist  // 线性弹簧：距离越远拉力越大
                } else 0f

                // domainTag 专属加成
                val domainBonus = if (a.domainTag.isNotBlank() && a.domainTag == b.domainTag) {
                    config.domainTagBonus * dist
                } else 0f

                // formTag 专属加成
                val formBonus = if (a.formTag != null && b.formTag != null && a.formTag == b.formTag) {
                    config.formTagBonus * dist
                } else 0f

                // story 锚定加成（多对多）
                val sharedStories = a.storyIds.intersect(b.storyIds)
                val storyBond = if (sharedStories.isNotEmpty()) {
                    config.storyBondStrength * dist
                } else if (a.storyIds.contains(b.id) || b.storyIds.contains(a.id)) {
                    config.storyBondStrength * dist * 1.5f
                } else 0f

                val netForce = attrForce + domainBonus + formBonus + storyBond - repelForce
                val forceX = netForce * dirX
                val forceY = netForce * dirY

                forces[i][0] += forceX
                forces[i][1] += forceY
                forces[j][0] -= forceX
                forces[j][1] -= forceY
            }
        }

        // ── 第二阶段：应用中心力 + 速度 + 位移 ──────────
        for (i in 0 until n) {
            val node = _nodes[i]
            if (node.isFixed) continue

            // 中心力：拉向原点
            val distFromCenter = sqrt(node.x * node.x + node.y * node.y)
            if (distFromCenter > 1f) {
                forces[i][0] -= config.centeringForce * node.x
                forces[i][1] -= config.centeringForce * node.y
            }

            // 速度更新（质量归一化 + 阻尼）
            node.vx = (node.vx + forces[i][0] / node.mass) * damping
            node.vy = (node.vy + forces[i][1] / node.mass) * damping

            // 速度限制
            val speed = sqrt(node.vx * node.vx + node.vy * node.vy)
            if (speed > config.maxVelocity) {
                node.vx = node.vx / speed * config.maxVelocity
                node.vy = node.vy / speed * config.maxVelocity
            }

            // 位置更新
            node.x += node.vx
            node.y += node.vy

            // 边界限制
            val dist = sqrt(node.x * node.x + node.y * node.y)
            if (dist > config.canvasRadius) {
                node.x = node.x / dist * config.canvasRadius
                node.y = node.y / dist * config.canvasRadius
            }
        }
    }

    // ── 初始化 ────────────────────────────────────────

    /**
     * 初始化节点位置。
     * 已有坐标的保持不变，未设置坐标的随机散布在画布内。
     */
    private fun initializePositions() {
        val radius = config.canvasRadius * 0.5f
        _nodes.forEachIndexed { index, node ->
            if (!node.isFixed && node.x == 0f && node.y == 0f) {
                // 使用黄金角度螺旋初始化，避免完全随机导致的初始重叠
                val angle = index * 2.399963f  // 黄金角 ≈ 137.5°
                val r = radius * sqrt(index.toFloat() / _nodes.size.coerceAtLeast(1))
                node.x = r * cos(angle)
                node.y = r * sin(angle)
                node.vx = 0f
                node.vy = 0f
            }
        }
    }

    // ── 收敛检测 ──────────────────────────────────────

    /**
     * 检查是否已收敛：所有非固定节点的平均速度低于阈值。
     */
    private fun checkConvergence(): Boolean {
        val movable = _nodes.filter { !it.isFixed }
        if (movable.isEmpty()) return true
        val avgSpeed = movable.sumOf { sqrt((it.vx * it.vx + it.vy * it.vy).toDouble()) } / movable.size
        return avgSpeed < config.convergenceThreshold
    }

    // ── 快照 ──────────────────────────────────────────

    /**
     * 返回当前所有节点位置的深拷贝快照。
     */
    fun snapshot(): List<GraphNode> {
        return _nodes.map { it.copy() }
    }

    /**
     * 获取节点位置映射。
     */
    fun positions(): Map<String, Pair<Float, Float>> {
        return _nodes.associate { it.id to (it.x to it.y) }
    }
}
