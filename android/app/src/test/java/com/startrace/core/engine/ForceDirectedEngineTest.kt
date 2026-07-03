package com.startrace.core.engine

import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ForceDirectedEngine 力导向引擎")
class ForceDirectedEngineTest {

    private lateinit var engine: ForceDirectedEngine

    @BeforeEach
    fun setUp() {
        engine = ForceDirectedEngine(ForceConfig())
    }

    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("基础功能")
    inner class BasicOperations {

        @Test
        @DisplayName("空图 simulate 不应崩溃")
        fun `empty graph should not crash on simulate`() {
            val result = engine.simulate(10)
            assertTrue(result.isEmpty(), "空图应返回空列表")
        }

        @Test
        @DisplayName("单节点模拟后位置不应大幅漂移")
        fun `single node should stay near initial position`() {
            val node = GraphNode(id = "1", x = 100f, y = 100f)
            engine.addNode(node)
            engine.simulate(20)

            val snap = engine.snapshot().first()
            val dist = sqrt(
                (snap.x - 100f) * (snap.x - 100f) +
                        (snap.y - 100f) * (snap.y - 100f)
            )
            // 单节点无斥力，但中心力会将其拉向原点
            assertTrue(dist < 100f, "单节点漂移应 < 100px, 实际: ${dist}")
        }

        @Test
        @DisplayName("添加和移除节点不应抛异常")
        fun `add and remove nodes should work`() {
            engine.addNode(GraphNode(id = "1"))
            engine.addNode(GraphNode(id = "2"))
            assertEquals(2, engine.nodes.size)

            engine.removeNode("1")
            assertEquals(1, engine.nodes.size)
            assertEquals("2", engine.nodes.first().id)
        }

        @Test
        @DisplayName("clear 后节点列表为空且状态重置")
        fun `clear should reset all state`() {
            engine.addNode(GraphNode(id = "1"))
            engine.simulate(10)
            engine.clear()

            assertTrue(engine.nodes.isEmpty())
            assertEquals(0, engine.iteration, "iteration 应重置为 0")
        }
    }

    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("斥力分离")
    inner class Repulsion {

        @Test
        @DisplayName("两个无标签节点应被斥力推开")
        fun `two unrelated nodes should repel each other`() {
            engine.addNode(GraphNode(id = "1", x = -10f, y = 0f))
            engine.addNode(GraphNode(id = "2", x = 10f, y = 0f))
            engine.simulate(50)

            val pos = engine.positions()
            val (x1, y1) = pos["1"]!!
            val (x2, y2) = pos["2"]!!

            val finalDist = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
            assertTrue(
                finalDist > 50f,
                "无关联节点在斥力下应远离到 > 50px，实际距离: $finalDist"
            )
        }

        @Test
        @DisplayName("多个无关节点应分散排布")
        fun `multiple unrelated nodes should spread out`() {
            repeat(5) { i ->
                engine.addNode(GraphNode(id = "$i", x = 0f, y = 0f))
            }
            engine.simulate(50)

            val pos = engine.positions()
            // 检查任意两个节点之间没有重叠
            val ids = pos.keys.toList()
            for (i in ids.indices) {
                for (j in i + 1 until ids.size) {
                    val (x1, y1) = pos[ids[i]]!!
                    val (x2, y2) = pos[ids[j]]!!
                    val dist = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
                    assertTrue(
                        dist > 5f,
                        "节点 ${ids[i]} 和 ${ids[j]} 距离 $dist 过近（应 > 5px）"
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("标签引力聚合")
    inner class Attraction {

        @Test
        @DisplayName("同 domainTag 节点应聚合在一起")
        fun `same domainTag nodes should cluster`() {
            // 两组不同的 domainTag
            engine.addNode(GraphNode(id = "dw1", domainTag = "world", x = -300f, y = 0f))
            engine.addNode(GraphNode(id = "dw2", domainTag = "world", x = 100f, y = 50f))
            engine.addNode(GraphNode(id = "dc1", domainTag = "character", x = 300f, y = 0f))
            engine.addNode(GraphNode(id = "dc2", domainTag = "character", x = -100f, y = -50f))

            engine.simulate(60)

            val pos = engine.positions()
            val distWithinWorld = distance(pos["dw1"]!!, pos["dw2"]!!)
            val distWithinChar = distance(pos["dc1"]!!, pos["dc2"]!!)
            val distWorldToChar1 = distance(pos["dw1"]!!, pos["dc1"]!!)
            val distWorldToChar2 = distance(pos["dw2"]!!, pos["dc2"]!!)

            // 同标签节点应比跨标签节点更近
            assertTrue(
                distWithinWorld < distWorldToChar1,
                "同 world 标签节点距离 $distWithinWorld 应小于跨标签距离 $distWorldToChar1"
            )
            assertTrue(
                distWithinChar < distWorldToChar2,
                "同 character 标签节点距离 $distWithinChar 应小于跨标签距离 $distWorldToChar2"
            )
        }

        @Test
        @DisplayName("同 formTag 节点应比不同 formTag 的更靠近")
        fun `same formTag nodes should drift closer`() {
            // 低斥力 + 强引力配置，让标签吸引力可观测
            engine = ForceDirectedEngine(
                ForceConfig(
                    repulsionStrength = 500f,
                    formTagBonus = 0.05f,
                    canvasRadius = 2000f,
                    convergenceThreshold = 0.1f
                )
            )
            val r = 250f
            engine.addNode(GraphNode(id = "f1", formTag = "scene", x = r, y = 0f))
            engine.addNode(GraphNode(id = "f2", formTag = "scene", x = -r / 2, y = r * 0.866f))
            engine.addNode(GraphNode(id = "f3", formTag = "character", x = -r / 2, y = -r * 0.866f))

            engine.simulate(100)

            val pos = engine.positions()
            val sameFormDist = distance(pos["f1"]!!, pos["f2"]!!)
            val diffFormDist1 = distance(pos["f1"]!!, pos["f3"]!!)
            val diffFormDist2 = distance(pos["f2"]!!, pos["f3"]!!)
            val avgDiffDist = (diffFormDist1 + diffFormDist2) / 2f

            assertTrue(
                sameFormDist < avgDiffDist,
                "同 formTag 距离 $sameFormDist 应小于不同 formTag 平均距离 $avgDiffDist"
            )
        }
    }

    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("固定节点")
    inner class FixedNodes {

        @Test
        @DisplayName("固定节点在模拟后位置不应改变")
        fun `fixed nodes should not move`() {
            val story = GraphNode(
                id = "story1",
                isFixed = true,
                nodeType = NodeType.STORY,
                x = 200f, y = 200f
            )
            engine.addNode(story)
            engine.addNode(GraphNode(id = "f1", storyIds = setOf("story1"), x = 300f, y = 300f))
            engine.addNode(GraphNode(id = "f2", storyIds = setOf("story1"), x = 100f, y = 100f))

            engine.simulate(50)

            val snap = engine.snapshot().find { it.id == "story1" }!!
            assertEquals(200f, snap.x, 0.01f, "固定节点 X 不应改变")
            assertEquals(200f, snap.y, 0.01f, "固定节点 Y 不应改变")
        }

        @Test
        @DisplayName("碎片应被故事节点锚定引力拉近")
        fun `fragments should be pulled toward their story node`() {
            // 低斥力 + 强 story bond 配置
            engine = ForceDirectedEngine(
                ForceConfig(
                    repulsionStrength = 500f,
                    storyBondStrength = 0.08f,
                    canvasRadius = 2000f,
                    convergenceThreshold = 0.1f
                )
            )
            val story = GraphNode(
                id = "story_a",
                isFixed = true,
                nodeType = NodeType.STORY,
                x = 0f, y = 0f
            )
            engine.addNode(story)
            val storyFrags = listOf(
                GraphNode(id = "sa1", storyIds = setOf("story_a"), x = -200f, y = 0f),
                GraphNode(id = "sa2", storyIds = setOf("story_a"), x = 200f, y = 0f)
            )
            val freeFrags = listOf(
                GraphNode(id = "fb1", storyIds = emptySet(), x = 0f, y = -200f),
                GraphNode(id = "fb2", storyIds = emptySet(), x = 0f, y = 200f)
            )
            storyFrags.forEach { engine.addNode(it) }
            freeFrags.forEach { engine.addNode(it) }

            engine.simulate(100)

            val pos = engine.positions()
            val storyPairDist = distance(pos["sa1"]!!, pos["sa2"]!!)
            val freePairDist = distance(pos["fb1"]!!, pos["fb2"]!!)

            assertTrue(
                storyPairDist < freePairDist,
                "同 story 碎片距离 $storyPairDist 应小于无归属碎片距离 $freePairDist"
            )
        }
    }

    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("收敛性")
    inner class Convergence {

        @Test
        @DisplayName("10 个节点 50 轮内应收敛且坐标在 ±1000 范围内")
        fun `10 nodes should converge within 50 iterations`() {
            repeat(10) { i ->
                engine.addNode(
                    GraphNode(
                        id = "$i",
                        domainTag = listOf("world", "character", "plot", "dialogue", "setting", "thought")[i % 6]
                    )
                )
            }

            engine.simulate(50)

            // 验证迭代次数 ≤ 50
            assertTrue(engine.convergedAt <= 50, "应在 50 轮内收敛，实际: ${engine.convergedAt}")
            assertTrue(engine.isConverged, "应标记为已收敛")

            // 验证所有非固定节点坐标在 ±canvasRadius 内
            val radius = engine.nodes.first().let {
                // 获取 config 的 canvasRadius，但 config 是 private 的
                // 使用默认值 1000
                1000f
            }
            engine.snapshot().forEach { node ->
                if (!node.isFixed) {
                    assertTrue(
                        abs(node.x) <= radius + 10f,
                        "节点 ${node.id} x=${node.x} 超出范围 ±$radius"
                    )
                    assertTrue(
                        abs(node.y) <= radius + 10f,
                        "节点 ${node.id} y=${node.y} 超出范围 ±$radius"
                    )
                }
            }
        }

        @Test
        @DisplayName("速度应在迭代后期显著减小")
        fun `velocity should decrease over iterations`() {
            repeat(5) { i ->
                engine.addNode(GraphNode(id = "$i"))
            }

            // 跑 50 轮然后检查速度
            engine.simulate(50)

            var maxSpeed = 0f
            engine.snapshot().filter { !it.isFixed }.forEach { node ->
                val s = sqrt(node.vx * node.vx + node.vy * node.vy)
                if (s > maxSpeed) maxSpeed = s
            }

            // 收敛后速度应很小
            assertTrue(
                maxSpeed < 5f,
                "收敛后最大速度应 < 5，实际: $maxSpeed"
            )
        }
    }

    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("边界情况")
    inner class EdgeCases {

        @Test
        @DisplayName("全部固定节点不应移动")
        fun `all fixed nodes should not change position`() {
            val initialPositions = mutableMapOf<String, Pos>()
            repeat(5) { i ->
                val x = i * 100f
                val y = i * 50f
                engine.addNode(
                    GraphNode(id = "$i", isFixed = true, nodeType = NodeType.STORY, x = x, y = y)
                )
                initialPositions["$i"] = Pos(x, y)
            }

            engine.simulate(50)

            engine.snapshot().forEach { node ->
                val initial = initialPositions[node.id]!!
                assertEquals(initial.x, node.x, 0.01f, "固定节点 ${node.id} X 不应改变")
                assertEquals(initial.y, node.y, 0.01f, "固定节点 ${node.id} Y 不应改变")
            }
        }

        @Test
        @DisplayName("大量节点不应崩溃")
        fun `large graph should not crash`() {
            repeat(100) { i ->
                engine.addNode(
                    GraphNode(
                        id = "$i",
                        domainTag = listOf("world", "character", "plot")[i % 3],
                        formTag = if (i % 4 == 0) "scene" else null
                    )
                )
            }

            val result = engine.simulate(30)
            assertEquals(100, result.size, "应返回 100 个节点位置")
        }

        @Test
        @DisplayName("节点不应完全重叠（最小距离保护）")
        fun `nodes should not overlap completely`() {
            // 密集初始化的节点不应完全重叠
            repeat(10) { i ->
                engine.addNode(GraphNode(id = "$i"))
            }

            engine.simulate(50)

            val pos = engine.positions().values.toList()
            for (i in pos.indices) {
                for (j in i + 1 until pos.size) {
                    val dist = distance(pos[i], pos[j])
                    assertTrue(
                        dist > 0.5f,
                        "节点 $i 和 $j 距离 $dist 过近（重叠风险）"
                    )
                }
            }
        }
    }

    // ── 辅助 ──────────────────────────────────────────

    private data class Pos(val x: Float, val y: Float)

    private fun distance(a: Pair<Float, Float>, b: Pos): Float {
        val dx = a.first - b.x
        val dy = a.second - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun distance(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        val dx = a.first - b.first
        val dy = a.second - b.second
        return sqrt(dx * dx + dy * dy)
    }

    private companion object {
        private fun sqrt(v: Float): Float = kotlin.math.sqrt(v.toDouble()).toFloat()
    }
}
