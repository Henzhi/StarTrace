package com.startrace.feature.profile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.startrace.feature.story.ui.LLMConfigScreen

/**
 * 我的页 — LLM 配置 + 数据统计（后续扩展）
 *
 * 当前阶段：LLM API 配置管理。
 * 后续：碎片数据统计、故事广场。
 */
@Composable
fun ProfileScreen() {
    LLMConfigScreen(modifier = Modifier.fillMaxSize())
}
