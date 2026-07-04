package com.startrace.feature.galaxy.ui.canvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import com.startrace.core.engine.GraphNode
import com.startrace.core.engine.NodeType
import com.startrace.design.theme.StarColors
import com.startrace.feature.galaxy.viewmodel.GalaxyViewModel
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 星系画布 — 背景 + 连线 + 节点 LOD + 手势交互
 *
 * 性能优化：
 * - 视口裁剪：跳过屏幕外节点
 * - 三级 LOD：MICRO (zoom<0.5) / NORMAL / DETAIL (>2, 显示标题)
 * - 背景星点：固定 seed，remember 避免重组闪烁
 */
@Composable
fun GalaxyCanvas(
    viewModel: GalaxyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current.density

    val pulse = rememberInfiniteTransition(label = "pulse").let { t ->
        val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), "p")
        v
    }

    val nodes = uiState.nodes
    val offsetX = uiState.offsetX
    val offsetY = uiState.offsetY
    val zoom = uiState.zoom
    val selectedId = uiState.selectedNodeId
    val highlightedTag = uiState.highlightedTag
    val draggingNodeId = uiState.draggingNodeId
    val viewConfig = LocalViewConfiguration.current
    val doubleTapTimeout = viewConfig.doubleTapTimeoutMillis

    // rememberUpdatedState 确保 pointerInput 始终读取最新值
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val currentZoom by rememberUpdatedState(zoom)
    val currentNodes by rememberUpdatedState(nodes)
    val currentSelectedId by rememberUpdatedState(selectedId)

    // 固定 seed 星点
    val starPositions = remember {
        List(80) { i ->
            val rng = kotlin.random.Random(i * 73 + 42L)
            arrayOf(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 2f + 0.5f, rng.nextFloat() * 0.5f + 0.15f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // 视口平移 + 缩放（仅在未拖拽节点时生效）
            // 使用 draggingNodeId 作为 key，拖拽开始/结束时自动切换
            .pointerInput(draggingNodeId) {
                if (draggingNodeId == null) {
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        viewModel.updateViewport(pan.x / density, pan.y / density)
                        if (zoomChange != 1f) viewModel.updateZoom(zoomChange, centroid.x / density, centroid.y / density)
                    }
                }
            }
            // 单击选点/取消 + 长按选中 + 仅已选中节点可长按拖拽
            .pointerInput(draggingNodeId, selectedId) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position

                    // 等待抬起或超时（短按 / 长按分流）
                    val up = withTimeoutOrNull(doubleTapTimeout) {
                        waitForUpOrCancellation()
                    }

                    if (up != null) {
                        // ── 单击 ──
                        val node = hitTestNode(
                            downPos.x, downPos.y,
                            size.width.toFloat(), size.height.toFloat(),
                            density, currentZoom, currentOffsetX, currentOffsetY, currentNodes
                        )
                        if (node != null) {
                            // 点击已选中节点 → 取消选中；否则选中
                            if (node.id == currentSelectedId) viewModel.deselectNode()
                            else viewModel.selectNode(node.id)
                        } else {
                            viewModel.deselectNode()
                        }
                        return@awaitEachGesture
                    }

                    // ── 长按 ──
                    val node = hitTestNode(
                        downPos.x, downPos.y,
                        size.width.toFloat(), size.height.toFloat(),
                        density, currentZoom, currentOffsetX, currentOffsetY, currentNodes
                    )
                    if (node != null) {
                        if (node.id == currentSelectedId) {
                            // ✅ 长按已选中节点 → 开始拖拽
                            viewModel.startDragging(node.id)
                            down.consume()
                            var prevX = down.position.x; var prevY = down.position.y
                            var dragging = true
                            while (dragging) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id }
                                if (ch == null || !ch.pressed) dragging = false
                                else {
                                    ch.consume()
                                    val pos = ch.position
                                    viewModel.dragNode((pos.x - prevX) / density / currentZoom, (pos.y - prevY) / density / currentZoom)
                                    prevX = pos.x; prevY = pos.y
                                }
                            }
                            viewModel.endDragging()
                        } else {
                            // 长按未选中节点 → 仅选中，不拖拽
                            viewModel.selectNode(node.id)
                            down.consume()
                            // 清空该手指的后续事件，避免穿透到平移
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id }
                                if (ch == null || !ch.pressed) break
                                ch.consume()
                            }
                        }
                    }
                    // 长按空白 → 不消费，平移缩放正常工作
                }
            }
    ) {
        val cw = size.width; val ch = size.height
        val cx = cw / 2f; val cy = ch / 2f
        val d = density; val z = zoom
        val maxR = maxOf(cw, ch) * 1.2f

        // 1. 深空渐变背景
        drawCircle(Brush.radialGradient(listOf(Color(0xFF1A1A3E), Color(0xFF0D0D1A), Color(0xFF0A0A0F)), Offset(cx, cy), maxR), maxR, Offset(cx, cy))

        // 2. 星点
        starPositions.forEach { s -> drawCircle(Color.White.copy(alpha = s[3]), s[2], Offset(s[0] * cw, s[1] * ch)) }

        // 3. 连线
        drawConnectionLines(nodes, z, d, cw, ch, cx, cy, offsetX, offsetY)

        // 4. 节点 + LOD + 视口裁剪
        nodes.forEach { node ->
            val sx = (node.x + offsetX) * z * d + cx
            val sy = (node.y + offsetY) * z * d + cy
            if (sx < -60 || sx > cw + 60 || sy < -60 || sy > ch + 60) return@forEach

            val isSelected = node.id == selectedId
            val isDimmed = highlightedTag != null && node.domainTag != highlightedTag
            val alpha = if (isDimmed) 0.12f else 1f

            val baseR = when (node.nodeType) {
                NodeType.STORY -> 12f * d * z; NodeType.FRAGMENT -> 6f * d * z
            }.coerceIn(2f, 36f)

            val clr = when (node.nodeType) {
                NodeType.STORY -> StarColors.NodeStory; NodeType.FRAGMENT -> StarColors.NodeFragment
            }

            when {
                z < 0.5f -> drawCircle(clr.copy(alpha = alpha), baseR.coerceAtMost(3f), Offset(sx, sy))
                z > 2f -> {
                    drawCircle(Brush.radialGradient(listOf(clr.copy(alpha = 0.25f * alpha), Color.Transparent), Offset(sx, sy), 16f * d * z), 16f * d * z, Offset(sx, sy))
                    drawCircle(clr.copy(alpha = alpha), baseR, Offset(sx, sy))
                    drawContext.canvas.nativeCanvas.drawText(node.label.take(12), sx, sy - baseR - 6f * d,
                        android.graphics.Paint().apply { color = android.graphics.Color.argb((alpha * 255).toInt(), 224, 224, 224); textSize = 10f * d; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true })
                }
                else -> {
                    drawCircle(Brush.radialGradient(listOf(clr.copy(alpha = 0.25f * alpha), Color.Transparent), Offset(sx, sy), 16f * d * z), 16f * d * z, Offset(sx, sy))
                    drawCircle(clr.copy(alpha = alpha), baseR, Offset(sx, sy))
                    if (isSelected) drawCircle(Color.White.copy(alpha = 0.25f + pulse * 0.45f), baseR + 5f * d + pulse * 4f * d, Offset(sx, sy), style = Stroke(1.5f * d))
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnectionLines(
    nodes: List<GraphNode>, zoom: Float, density: Float, cw: Float, ch: Float, cx: Float, cy: Float, offsetX: Float, offsetY: Float
) {
    if (zoom < 0.6f || nodes.size < 2) return
    // 多对多：遍历每个碎片的 storyIds，画出到所有关联故事节点的连线
    val storyNodes = nodes.filter { it.nodeType == NodeType.STORY }.associateBy { it.id }
    val fragmentNodes = nodes.filter { it.nodeType == NodeType.FRAGMENT && it.storyIds.isNotEmpty() }
    val drawn = mutableSetOf<Pair<String, String>>()
    fragmentNodes.filter { it.storyIds.size <= 8 }.forEach { frag ->
        frag.storyIds.forEach { storyId ->
            val story = storyNodes[storyId] ?: return@forEach
            val key = if (frag.id < story.id) Pair(frag.id, story.id) else Pair(story.id, frag.id)
            if (key !in drawn) {
                drawn.add(key)
                drawBez(frag, story, zoom, density, cx, cy, offsetX, offsetY, 2f)
            }
        }
    }
    // 同一故事的碎片间微妙连线
    storyNodes.keys.forEach { sid ->
        val sameStoryFrags = fragmentNodes.filter { sid in it.storyIds }
        if (sameStoryFrags.size in 2..4) {
            for (i in sameStoryFrags.indices) for (j in i + 1 until sameStoryFrags.size) {
                drawBez(sameStoryFrags[i], sameStoryFrags[j], zoom, density, cx, cy, offsetX, offsetY, 1f)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBez(
    a: GraphNode, b: GraphNode, zoom: Float, density: Float, cx: Float, cy: Float, offsetX: Float, offsetY: Float, width: Float
) {
    val ax = (a.x + offsetX) * zoom * density + cx; val ay = (a.y + offsetY) * zoom * density + cy
    val bx = (b.x + offsetX) * zoom * density + cx; val by = (b.y + offsetY) * zoom * density + cy
    val dx = bx - ax; val dy = by - ay
    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val cp = Offset((ax + bx) / 2 - dy / len * len * 0.12f, (ay + by) / 2 + dx / len * len * 0.12f)
    val path = androidx.compose.ui.graphics.Path().apply { moveTo(ax, ay); quadraticTo(cp.x, cp.y, bx, by) }
    val alpha = ((1f - (len / (1800f * zoom)).coerceIn(0f, 1f)) * 0.4f).coerceAtLeast(0.08f)
    drawPath(path, StarColors.NodeConnection.copy(alpha = alpha), style = Stroke(width * density))
}

/** 命中测试：将屏幕坐标转换为引擎坐标，找到最近的节点 */
private fun hitTestNode(
    tapX: Float, tapY: Float, canvasW: Float, canvasH: Float, density: Float,
    zoom: Float, offsetX: Float, offsetY: Float, nodes: List<GraphNode>
): GraphNode? {
    val ex = (tapX - canvasW / 2f) / (zoom * density) - offsetX
    val ey = (tapY - canvasH / 2f) / (zoom * density) - offsetY
    val hit = nodes.minByOrNull { val dx = it.x - ex; val dy = it.y - ey; dx * dx + dy * dy }
    // 命中半径为固定 48 屏幕像素，不随缩放放大。缩小时需要点得更精准
    val hitRadius = 48f / (zoom * density)
    return if (hit != null && kotlin.math.sqrt((hit.x - ex) * (hit.x - ex) + (hit.y - ey) * (hit.y - ey)) < hitRadius) hit else null
}
