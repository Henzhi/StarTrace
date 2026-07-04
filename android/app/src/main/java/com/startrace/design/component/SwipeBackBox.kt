package com.startrace.design.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import com.startrace.design.theme.StarColors

/**
 * 滑动返回容器 — 从屏幕左/右边缘向右/左滑动触发 onBack
 *
 * 检测逻辑：
 * - 触摸起点在屏幕左侧 40dp 以内，向右滑动 → 左边缘出发
 * - 触摸起点在屏幕右侧 40dp 以内，向左滑动 → 右边缘出发
 * - 滑动距离超过屏幕宽度的 30% 或速度 > 800f/s 时触发返回
 * - 松手后若未达阈值则弹性回弹到原位
 *
 * 视觉效果：
 * - 内容随手指平移
 * - 边缘出现渐隐的返回箭头指示器
 */
@Composable
fun SwipeBackBox(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    edgeWidthDp: Float = 40f,
    dismissThresholdRatio: Float = 0.3f,
    velocityThreshold: Float = 800f,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { edgeWidthDp.dp.toPx() }
    val thresholdRatio = dismissThresholdRatio

    var containerSize by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var swipeFromLeft by remember { mutableFloatStateOf(0f) } // 左边缘出发的进度 0..1
    var swipeFromRight by remember { mutableFloatStateOf(0f) } // 右边缘出发的进度 0..1
    var isDragging by remember { mutableFloatStateOf(0f) } // 0=不拖拽, 1=左边缘, 2=右边缘

    // 回弹动画
    val animatedOffsetX by animateDpAsState(
        targetValue = if (isDragging > 0) with(density) { offsetX.toDp() } else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "swipeBack"
    )
    val offsetDp = if (isDragging > 0) with(density) { offsetX.toDp() } else animatedOffsetX

    Box(modifier = modifier.fillMaxSize()) {
        // ── 内容区域（随手指平移） ───────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it.width.toFloat() }
                .offset { IntOffset(offsetDp.roundToPx(), 0) }
                .pointerInput(edgeWidthPx, thresholdRatio, velocityThreshold) {
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            if (startOffset.x <= edgeWidthPx) {
                                // 左边缘开始，允许向右滑回
                                isDragging = 1f
                            } else if (startOffset.x >= containerSize - edgeWidthPx) {
                                // 右边缘开始，允许向左滑回
                                isDragging = 2f
                            }
                        },
                        onDragEnd = {
                            val progress = abs(offsetX) / (containerSize * thresholdRatio).coerceAtLeast(1f)
                            if (progress >= 1f) {
                                onBack()
                            }
                            offsetX = 0f
                            swipeFromLeft = 0f
                            swipeFromRight = 0f
                            isDragging = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                            swipeFromLeft = 0f
                            swipeFromRight = 0f
                            isDragging = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (isDragging == 1f) {
                                // 从左侧出发，只允许向右滑动
                                val newOffset = (offsetX + dragAmount).coerceIn(0f, containerSize)
                                offsetX = newOffset
                                swipeFromLeft = (newOffset / (containerSize * thresholdRatio)).coerceIn(0f, 1f)
                                swipeFromRight = 0f
                            } else if (isDragging == 2f) {
                                // 从右侧出发，只允许向左滑动
                                val newOffset = (offsetX + dragAmount).coerceIn(-containerSize, 0f)
                                offsetX = newOffset
                                swipeFromRight = (abs(newOffset) / (containerSize * thresholdRatio)).coerceIn(0f, 1f)
                                swipeFromLeft = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxSize()) { content() }
        }

        // ── 左边缘箭头指示器 ────────────────────
        if (swipeFromLeft > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (offsetDp * 0.5f).coerceAtMost(with(density) { 60.dp }))
                    .alpha(swipeFromLeft)
                    .size(44.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                StarColors.Primary.copy(alpha = 0.3f),
                                StarColors.Primary.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = StarColors.Primary.copy(alpha = swipeFromLeft * 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ── 右边缘箭头指示器 ────────────────────
        if (swipeFromRight > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (offsetDp * 0.5f).coerceAtLeast(with(density) { -60.dp }))
                    .alpha(swipeFromRight)
                    .size(44.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                StarColors.Primary.copy(alpha = 0f),
                                StarColors.Primary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = StarColors.Primary.copy(alpha = swipeFromRight * 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
