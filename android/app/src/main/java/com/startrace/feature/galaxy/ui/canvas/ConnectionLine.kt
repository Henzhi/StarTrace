package com.startrace.feature.galaxy.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.startrace.core.engine.GraphNode
import com.startrace.core.engine.NodeType
import com.startrace.design.theme.StarColors

/**
 * 连线渲染器 — 节点之间的贝塞尔连线（多对多支持）
 */
object ConnectionLine {

    private const val LINE_WIDTH = 1.5f
    private const val STORY_LINE_WIDTH = 2.5f

    fun DrawScope.drawConnections(
        nodes: List<GraphNode>,
        zoom: Float,
        density: Float
    ) {
        if (zoom < 0.6f) return
        val storyNodes = nodes.filter { it.nodeType == NodeType.STORY }.associateBy { it.id }
        val fragmentNodes = nodes.filter { it.nodeType == NodeType.FRAGMENT && it.storyIds.isNotEmpty() }
        val drawn = mutableSetOf<Pair<String, String>>()

        fragmentNodes.filter { it.storyIds.size <= 8 }.forEach { frag ->
            frag.storyIds.forEach { storyId ->
                val story = storyNodes[storyId] ?: return@forEach
                val key = if (frag.id < story.id) Pair(frag.id, story.id) else Pair(story.id, frag.id)
                if (key !in drawn) {
                    drawn.add(key)
                    drawBezierCurve(frag, story, zoom, density, STORY_LINE_WIDTH)
                }
            }
        }

        storyNodes.keys.forEach { sid ->
            val sameStoryFrags = fragmentNodes.filter { sid in it.storyIds }
            if (sameStoryFrags.size in 2..4) {
                for (i in sameStoryFrags.indices) for (j in i + 1 until sameStoryFrags.size)
                    drawBezierCurve(sameStoryFrags[i], sameStoryFrags[j], zoom, density, LINE_WIDTH)
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
        val start = Offset(from.x * zoom + size.width / 2f, from.y * zoom + size.height / 2f)
        val end = Offset(to.x * zoom + size.width / 2f, to.y * zoom + size.height / 2f)
        if ((start.x < -100 && end.x < -100) || (start.x > size.width + 100 && end.x > size.width + 100)) return

        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val cp = Offset(midX - dy / len * len * 0.15f, midY + dx / len * len * 0.15f)

        val path = Path().apply { moveTo(start.x, start.y); quadraticBezierTo(cp.x, cp.y, end.x, end.y) }
        val maxDist = 2000f * zoom
        val alpha = (1f - (len / maxDist).coerceIn(0f, 1f)) * 0.6f
        drawPath(path, StarColors.NodeConnection.copy(alpha = alpha), style = Stroke(width * density))
    }
}
