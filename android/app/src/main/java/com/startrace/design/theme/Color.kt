package com.startrace.design.theme

import androidx.compose.ui.graphics.Color

/**
 * 深空主题色彩体系 — 只用 Dark Theme（v1 不支持亮色主题）
 *
 * 设计理念：
 * - Primary:   星蓝 — 交互主色，不刺眼的冷色调
 * - Secondary: 星紫 — 强调色，用于故事/星云节点
 * - Background: 深空黑 — 沉浸式背景
 * - Surface:    半透明灰 — 卡片/面板
 */
object StarColors {
    val Primary       = Color(0xFF5B9BD5)  // 星蓝
    val PrimaryVariant = Color(0xFF3A7CC3)
    val Secondary      = Color(0xFF9B7ED8)  // 星紫
    val SecondaryVariant = Color(0xFF7B5EC8)

    val Background     = Color(0xFF0A0A0F)   // 深空黑
    val Surface        = Color(0xFF1A1A2E)   // 卡片表面
    val SurfaceVariant = Color(0xFF252540)   // 较亮的表面

    val OnPrimary   = Color.White
    val OnSecondary = Color.White
    val OnBackground = Color(0xFFE0E0E0)    // 主文字
    val OnSurface   = Color(0xFFCCCCCC)     // 辅助文字

    val Error    = Color(0xFFCF6679)
    val OnError  = Color.Black

    // 星系节点色（按类型区分）
    val NodeFragment = Color(0xFF5B9BD5)     // 碎片节点 — 星蓝
    val NodeStory    = Color(0xFFFFB74D)     // 故事节点 — 暖金
    val NodeConnection = Color(0x44FFFFFF)   // 连线 — 半透明白

    // 功能色
    val MoodExcited = Color(0xFFFFB74D)      // 兴奋 — 暖黄
    val MoodCalm    = Color(0xFF81C784)      // 平静 — 柔和绿
    val MoodConfused = Color(0xFFBA68C8)     // 困惑 — 淡紫
    val MoodAmazed  = Color(0xFF4FC3F7)      // 惊叹 — 亮蓝
}
