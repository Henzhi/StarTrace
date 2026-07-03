package com.startrace.feature.story.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.startrace.design.theme.StarColors

/**
 * 故事页 — AI 故事生成入口（Phase 3.3）
 *
 * 后续：星系中选中碎片 → 进入此页 → 选择风格/长度 → AI 生成故事。
 * LLM 配置已移至「我的」页面。
 */
@Composable
fun StoryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📖", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                "AI 为你续写灵感",
                style = MaterialTheme.typography.headlineMedium,
                color = StarColors.OnBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "在星系中选中碎片后\n来这里生成故事",
                style = MaterialTheme.typography.bodyMedium,
                color = StarColors.OnSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "前往「我的」→ LLM 配置",
                style = MaterialTheme.typography.labelMedium,
                color = StarColors.Primary
            )
        }
    }
}
