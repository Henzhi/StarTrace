package com.startrace.feature.galaxy.ui.canvas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.startrace.core.engine.GraphNode
import com.startrace.core.engine.NodeType
import com.startrace.design.theme.StarColors
import kotlin.math.sqrt

/**
 * 节点渲染器 — 在画布上绘制碎片/故事节点
 *
 * ### 视觉规范
 * - 碎片节点：星蓝色实心圆 (r=6dp)，外圈光晕 (r=12dp)
 * - 故事节点：暖金色实心圆 (r=12dp)，双倍尺寸
 * - 选中节点：脉冲光环动画
 * - 半透明节点：非高亮标签匹配时
 */
object NodeRenderer {

    /** 节点基础半径（dp → px 转换由调用方处理） */
    private const val FRAGMENT_RADIUS = 6f
    private const val STORY_RADIUS = 12f
    private const val GLOW_RADIUS = 14f
    private const val SELECTION_RING = 18f

    /**
     * 绘制所有节点。
     *
     * @param nodes 节点列表
     * @param selectedId 选中节点 ID
     * @param highlightedTag 高亮标签（null = 全部正常显示）
     * @param zoom 当前缩放
     * @param selectionPulse 选中脉冲值（0..1）
     * @param density 屏幕密度（dp → px）
     */
    fun DrawScope.drawNodes(
        nodes: List<GraphNode>,
        selectedId: String?,
        highlightedTag: String?,
        zoom: Float,
        selectionPulse: Float,
        density: Float
    ) {
        nodes.forEach { node ->
            val screenPos = Offset(
                node.x * zoom + size.width / 2f,
                node.y * zoom + size.height / 2f
            )

            // 视口裁剪：跳过屏幕外的节点
            if (screenPos.x < -50 || screenPos.x > size.width + 50 ||
                screenPos.y < -50 || screenPos.y > size.height + 50
            ) return@forEach

            val isSelected = node.id == selectedId
            val isDimmed = highlightedTag != null && node.domainTag != highlightedTag
            val alpha = if (isDimmed) 0.15f else 1f

            val baseRadius = when (node.nodeType) {
                NodeType.STORY -> STORY_RADIUS * density
                NodeType.FRAGMENT -> FRAGMENT_RADIUS * density
            }

            val nodeColor = when (node.nodeType) {
                NodeType.STORY -> StarColors.NodeStory
                NodeType.FRAGMENT -> StarColors.NodeFragment
            }

            // ── 光晕 ──────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        nodeColor.copy(alpha = 0.3f * alpha),
                        Color.Transparent
                    ),
                    center = screenPos,
                    radius = GLOW_RADIUS * density
                ),
                radius = GLOW_RADIUS * density,
                center = screenPos
            )

            // ── 节点主体 ──────────────────
            drawCircle(
                color = nodeColor.copy(alpha = alpha),
                radius = baseRadius,
                center = screenPos
            )

            // ── 选中脉冲环 ───────────────
            if (isSelected) {
                val ringAlpha = (0.4f + selectionPulse * 0.4f) * alpha
                drawCircle(
                    color = Color.White.copy(alpha = ringAlpha),
                    radius = SELECTION_RING * density + selectionPulse * 4f * density,
                    center = screenPos,
                    style = Stroke(width = 2f * density)
                )
            }
        }
    }
}

/** DrawScope 的 center 扩展 */
private val DrawScope.center: Offset get() = Offset(size.width / 2f, size.height / 2f)
