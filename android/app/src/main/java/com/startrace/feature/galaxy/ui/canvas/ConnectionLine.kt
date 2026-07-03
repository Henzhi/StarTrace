package com.startrace.feature.galaxy.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.startrace.core.engine.GraphNode
import com.startrace.design.theme.StarColors

/**
 * 连线渲染器 — 节点之间的贝塞尔连线
 *
 * 绘制规则：
 * - 同一 storyId 的碎片之间 → 细连线（仅在 zoom > 0.6 时显示）
 * - 碎片与其归属故事节点之间 → 稍粗连线
 * - 连线使用二次 Bezier 曲线，轻微弯曲避免直穿
 * - 半透明白色，不影响节点可见性（z-order 在节点之前绘制）
 */
object ConnectionLine {

    private const val LINE_WIDTH = 1.5f
    private const val STORY_LINE_WIDTH = 2.5f

    /**
     * 绘制所有连线。
     *
     * @param nodes 所有节点
     * @param zoom 当前缩放（低于阈值时隐藏连线以减少视觉噪音）
     * @param density 屏幕密度
     */
    fun DrawScope.drawConnections(
        nodes: List<GraphNode>,
        zoom: Float,
        density: Float
    ) {
        // 太小时不画连线，减少视觉噪音
        if (zoom < 0.6f) return

        val nodeMap = nodes.associateBy { it.id }

        // 按 storyId 分组，绘制碎片之间的连线
        val fragmentsByStory = nodes
            .filter { it.storyId != null && it.nodeType == com.startrace.core.engine.NodeType.FRAGMENT }
            .groupBy { it.storyId!! }

        fragmentsByStory.forEach { (storyId, frags) ->
            // 碎片 ↔ 故事节点连线
            val storyNode = nodeMap[storyId]
            if (storyNode != null && frags.size <= 6) {
                frags.forEach { frag ->
                    drawBezierCurve(frag, storyNode, zoom, density, STORY_LINE_WIDTH)
                }
            }

            // 同故事碎片之间连线（最多连 4 个避免杂乱）
            if (frags.size in 2..4) {
                for (i in frags.indices) {
                    for (j in i + 1 until frags.size) {
                        drawBezierCurve(frags[i], frags[j], zoom, density, LINE_WIDTH)
                    }
                }
            }
        }
    }

    private fun DrawScope.drawBezierCurve(
        from: GraphNode,
        to: GraphNode,
        zoom: Float,
        density: Float,
        width: Float
    ) {
        val start = Offset(
            from.x * zoom + size.width / 2f,
            from.y * zoom + size.height / 2f
        )
        val end = Offset(
            to.x * zoom + size.width / 2f,
            to.y * zoom + size.height / 2f
        )

        // 视野外跳过
        if ((start.x < -100 && end.x < -100) || (start.x > size.width + 100 && end.x > size.width + 100)) return

        // 二次贝塞尔：控制点在连线中点垂直偏移
        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val offset = len * 0.15f
        val cp = Offset(
            midX - dy / len * offset,
            midY + dx / len * offset
        )

        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticBezierTo(cp.x, cp.y, end.x, end.y)
        }

        // 渐隐：越远的连线越透明
        val maxDist = 2000f * zoom
        val alpha = (1f - (len / maxDist).coerceIn(0f, 1f)) * 0.6f

        drawPath(
            path = path,
            color = StarColors.NodeConnection.copy(alpha = alpha),
            style = Stroke(width = width * density)
        )
    }
}
