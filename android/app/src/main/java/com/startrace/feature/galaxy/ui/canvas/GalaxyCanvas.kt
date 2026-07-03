package com.startrace.feature.galaxy.ui.canvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.startrace.core.engine.GraphNode
import com.startrace.core.engine.NodeType
import com.startrace.design.theme.StarColors
import com.startrace.feature.galaxy.viewmodel.GalaxyViewModel

/**
 * 星系画布 — 深空背景 + 连线 + 节点渲染 + 手势交互
 *
 * 手势支持：
 * - 单指拖拽 → 平移视口
 * - 双指缩放 → 缩放视口 (0.3x – 5x)
 * - 点击 → 选中节点 / 取消选中
 */
@Composable
fun GalaxyCanvas(
    viewModel: GalaxyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current.density

    // 选中脉冲
    val pulse = rememberInfiniteTransition(label = "pulse").let { t ->
        val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), "p")
        v
    }

    // 快照 UI 状态以避免 lambda 捕获不稳定
    val nodes = uiState.nodes
    val offsetX = uiState.offsetX
    val offsetY = uiState.offsetY
    val zoom = uiState.zoom
    val selectedId = uiState.selectedNodeId
    val highlightedTag = uiState.highlightedTag

    // 固定随机星点
    val starPositions = remember {
        List(80) { i ->
            val rng = kotlin.random.Random(i * 73 + 42L)
            arrayOf(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 2f + 0.5f, rng.nextFloat() * 0.5f + 0.15f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomChange, _ ->
                    viewModel.updateViewport(pan.x / density, pan.y / density)
                    if (zoomChange != 1f) {
                        viewModel.updateZoom(zoomChange, centroid.x / density, centroid.y / density)
                    }
                }
            }
            .pointerInput(nodes.size) {
                detectTapGestures { tap ->
                    val engineX = (tap.x - size.width / 2f) / (zoom * density) - offsetX
                    val engineY = (tap.y - size.height / 2f) / (zoom * density) - offsetY
                    val hit = nodes.minByOrNull {
                        val dx = it.x - engineX; val dy = it.y - engineY
                        dx * dx + dy * dy
                    }
                    if (hit != null) {
                        val dx = hit.x - engineX; val dy = hit.y - engineY
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (dist < 80f / zoom) viewModel.selectNode(hit.id) else viewModel.deselectNode()
                    }
                }
            }
    ) {
        val cw = size.width; val ch = size.height
        val cx = cw / 2f; val cy = ch / 2f
        val d = density; val z = zoom

        // 1. 深空渐变底
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF1A1A3E), Color(0xFF0D0D1A), Color(0xFF0A0A0F)),
                Offset(cx, cy), maxOf(cw, ch) * 1.2f
            ), radius = maxOf(cw, ch) * 1.2f, center = Offset(cx, cy)
        )

        // 2. 随机星点
        starPositions.forEach { s ->
            drawCircle(Color.White.copy(alpha = s[3]), s[2], Offset(s[0] * cw, s[1] * ch))
        }

        // 3. 连线
        drawConnectionLines(nodes, z, d, cw, ch, cx, cy, offsetX, offsetY)

        // 4. 节点
        nodes.forEach { node ->
            val sx = (node.x + offsetX) * z * d + cx
            val sy = (node.y + offsetY) * z * d + cy

            if (sx < -60 || sx > cw + 60 || sy < -60 || sy > ch + 60) return@forEach

            val isSelected = node.id == selectedId
            val isDimmed = highlightedTag != null && node.domainTag != highlightedTag
            val alpha = if (isDimmed) 0.12f else 1f

            val r = when (node.nodeType) {
                NodeType.STORY -> 12f * d * z
                NodeType.FRAGMENT -> 6f * d * z
            }.coerceIn(2f, 36f)

            val clr = when (node.nodeType) {
                NodeType.STORY -> StarColors.NodeStory
                NodeType.FRAGMENT -> StarColors.NodeFragment
            }

            // 光晕
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(clr.copy(alpha = 0.25f * alpha), Color.Transparent),
                    Offset(sx, sy), 16f * d * z
                ), radius = 16f * d * z, center = Offset(sx, sy)
            )

            // 主体
            drawCircle(clr.copy(alpha = alpha), r, Offset(sx, sy))

            // 选中环
            if (isSelected) {
                drawCircle(
                    Color.White.copy(alpha = 0.25f + pulse * 0.45f),
                    r + 5f * d + pulse * 4f * d,
                    Offset(sx, sy),
                    style = Stroke(1.5f * d)
                )
            }
        }
    }
}

/** 绘制连线 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnectionLines(
    nodes: List<GraphNode>, zoom: Float, density: Float,
    cw: Float, ch: Float, cx: Float, cy: Float,
    offsetX: Float, offsetY: Float
) {
    if (zoom < 0.6f || nodes.size < 2) return

    val fragsByStory = nodes.filter { it.storyId != null && it.nodeType == NodeType.FRAGMENT }
        .groupBy { it.storyId!! }
    val nodeMap = nodes.associateBy { it.id }

    fragsByStory.forEach { (storyId, frags) ->
        val story = nodeMap[storyId]
        // 碎片↔故事
        if (story != null && frags.size <= 6) {
            frags.forEach { f ->
                drawBez(f, story, zoom, density, cx, cy, offsetX, offsetY, 2f)
            }
        }
        // 碎片之间
        if (frags.size in 2..4) {
            for (i in frags.indices)
                for (j in i + 1 until frags.size)
                    drawBez(frags[i], frags[j], zoom, density, cx, cy, offsetX, offsetY, 1f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBez(
    a: GraphNode, b: GraphNode, zoom: Float, density: Float,
    cx: Float, cy: Float, offsetX: Float, offsetY: Float, width: Float
) {
    val ax = (a.x + offsetX) * zoom * density + cx
    val ay = (a.y + offsetY) * zoom * density + cy
    val bx = (b.x + offsetX) * zoom * density + cx
    val by = (b.y + offsetY) * zoom * density + cy

    val dx = bx - ax; val dy = by - ay
    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val cp = Offset((ax + bx) / 2 - dy / len * len * 0.12f, (ay + by) / 2 + dx / len * len * 0.12f)

    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(ax, ay); quadraticTo(cp.x, cp.y, bx, by)
    }
    val alpha = ((1f - (len / (1800f * zoom)).coerceIn(0f, 1f)) * 0.4f).coerceAtLeast(0.08f)
    drawPath(path, StarColors.NodeConnection.copy(alpha = alpha), style = Stroke(width * density))
}
