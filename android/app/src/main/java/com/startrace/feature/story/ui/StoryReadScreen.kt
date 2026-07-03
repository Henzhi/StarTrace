package com.startrace.feature.story.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.startrace.core.database.entity.StoryEntity
import com.startrace.design.theme.StarColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * 故事阅读页 — Markdown 渲染 + 深空背景
 *
 * 支持基础 Markdown 语法：
 * - # 标题 / ## 副标题 / ### 三级标题
 * - **粗体** / *斜体*
 * - 普通段落
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryReadScreen(
    story: StoryEntity,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("阅读", color = StarColors.OnBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = StarColors.OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StarColors.Surface.copy(alpha = 0.8f))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A0A0F), Color(0xFF0D0D1A), Color(0xFF1A1A3E))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // 风格/长度标签
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styleLabel = when (story.style) {
                        "scifi" -> "🚀 科幻"; "fantasy" -> "🧙 奇幻"; "realistic" -> "📷 现实"
                        "prose" -> "🌸 散文"; "poetry" -> "🎵 诗歌"; "mystery" -> "🔍 悬疑"; else -> story.style
                    }
                    Surface(color = StarColors.Primary.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                        Text(styleLabel, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = StarColors.Primary)
                    }
                    val lengthLabel = when (story.length) { "short" -> "短篇" "medium" -> "中篇" "long" -> "长篇" else -> story.length }
                    Surface(color = StarColors.Secondary.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                        Text(lengthLabel, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = StarColors.Secondary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 标题
                Text(story.title, style = MaterialTheme.typography.headlineMedium, color = StarColors.OnBackground, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                // 时间
                Text(dateFormat.format(Date(story.createdAt)), style = MaterialTheme.typography.labelSmall, color = StarColors.OnSurface.copy(alpha = 0.5f))

                Spacer(Modifier.height(20.dp))

                // 正文 — Markdown 渲染
                val titleLargeSize = MaterialTheme.typography.titleLarge.fontSize
                val titleMediumSize = MaterialTheme.typography.titleMedium.fontSize
                val titleSmallSize = MaterialTheme.typography.titleSmall.fontSize
                Text(
                    text = renderMarkdown(story.content, titleLargeSize, titleMediumSize, titleSmallSize),
                    style = MaterialTheme.typography.bodyLarge,
                    color = StarColors.OnSurface,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 简易 Markdown → AnnotatedString
 */
private fun renderMarkdown(text: String, titleLarge: androidx.compose.ui.unit.TextUnit, titleMedium: androidx.compose.ui.unit.TextUnit, titleSmall: androidx.compose.ui.unit.TextUnit) = buildAnnotatedString {
    text.lines().forEach { line ->
        when {
            line.startsWith("### ") -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = titleSmall, color = StarColors.OnBackground)) {
                append(line.removePrefix("### ")); append('\n')
            }
            line.startsWith("## ") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = titleMedium, color = StarColors.OnBackground)) {
                append(line.removePrefix("## ")); append('\n')
            }
            line.startsWith("# ") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = titleLarge, color = StarColors.OnBackground)) {
                append(line.removePrefix("# ")); append('\n')
            }
            else -> { appendInlineMarkdown(line); append('\n') }
        }
    }
}

/**
 * 行内 Markdown：**粗体** / *斜体*
 */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineMarkdown(line: String) {
    var i = 0
    while (i < line.length) {
        when {
            line.startsWith("**", i) -> {
                val end = line.indexOf("**", i + 2)
                if (end > i) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line.substring(i + 2, end)) }
                    i = end + 2
                } else { append(line[i]); i++ }
            }
            line.startsWith("*", i) && i + 1 < line.length && line[i + 1] != ' ' && line[i + 1] != '*' -> {
                val end = line.indexOf("*", i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(line.substring(i + 1, end)) }
                    i = end + 1
                } else { append(line[i]); i++ }
            }
            else -> { append(line[i]); i++ }
        }
    }
}
