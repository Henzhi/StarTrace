package com.startrace.feature.galaxy.ui.canvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.startrace.design.theme.StarColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 深空背景 — 渐变 + 微尘粒子
 *
 * 使用 Canvas 绘制：
 * - 从深空黑到深蓝紫的径向渐变
 * - ~120 个随机散布的微尘粒子，以极慢速度绕中心旋转
 * - 粒子大小随机 0.5dp–2dp，透明度随机
 *
 * 性能：背景为静态渐变 + 粒子缓存，不触发频繁重组。
 */
@Composable
fun DeepSpaceBackground(modifier: Modifier = Modifier) {
    // 粒子位置种子（固定随机数避免重组时抖动）
    val particles = remember {
        List(120) {
            val angle = Random.nextFloat() * 360f
            val radius = Random.nextFloat() * 0.9f + 0.1f
            Stardust(
                angle = angle,
                radius = radius,
                size = Random.nextFloat() * 2.5f + 0.5f,
                alpha = Random.nextFloat() * 0.6f + 0.2f,
                speed = Random.nextFloat() * 0.02f + 0.005f
            )
        }
    }

    // 缓慢旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "stardust")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = maxOf(centerX, centerY) * 1.2f

        // ── 径向渐变背景 ───────────────────
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A1A3E),
                    Color(0xFF0D0D1A),
                    StarColors.Background
                ),
                center = center.copy(centerX, centerY),
                radius = maxRadius
            ),
            radius = maxRadius,
            center = center.copy(centerX, centerY)
        )

        // ── 微尘粒子 ───────────────────────
        val radRotation = Math.toRadians(rotation.toDouble()).toFloat()
        particles.forEach { particle ->
            val angle = particle.angle + particle.speed * rotation
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val r = particle.radius * maxRadius
            val x = centerX + r * cos(rad)
            val y = centerY + r * sin(rad) * 0.7f  // 轻微椭圆

            drawCircle(
                color = Color.White.copy(alpha = particle.alpha * (0.4f + 0.6f * (sin(radRotation + particle.angle) + 1f) / 2f)),
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

/** 粒子数据 */
private data class Stardust(
    val angle: Float,
    val radius: Float,
    val size: Float,
    val alpha: Float,
    val speed: Float
)

/** DrawScope center helper */
private val DrawScope.center: Offset get() = Offset(size.width / 2f, size.height / 2f)
