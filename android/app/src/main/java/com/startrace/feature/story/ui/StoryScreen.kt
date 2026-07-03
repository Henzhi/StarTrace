package com.startrace.feature.story.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.design.theme.StarColors
import com.startrace.feature.fragment.ui.component.getDomainDisplay
import com.startrace.feature.story.viewmodel.StoryGeneratorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(
    viewModel: StoryGeneratorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = StarColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("AI 故事", color = StarColors.OnBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StarColors.Surface)
            )
        }
    ) { padding ->
        // 如果已保存，显示成功状态
        if (uiState.savedStoryId != null) {
            SavedState(onReset = { viewModel.reset() }, Modifier.padding(padding))
            return@Scaffold
        }

        // 如果已有生成结果，显示结果页
        if (uiState.result != null) {
            ResultView(
                story = uiState.result!!,
                onSave = { viewModel.saveStory() },
                onRetry = { viewModel.generate() },
                onBack = { viewModel.reset() },
                isSaved = false,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        // 主页面：碎片选择 + 风格/长度
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 风格选择 ──────────────
            item {
                Text("故事风格", style = MaterialTheme.typography.titleSmall, color = StarColors.OnBackground)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("scifi" to "🚀 科幻", "fantasy" to "🧙 奇幻", "realistic" to "📷 现实", "prose" to "🌸 散文", "poetry" to "🎵 诗歌", "mystery" to "🔍 悬疑").forEach { (v, label) ->
                        FilterChip(
                            selected = uiState.style == v,
                            onClick = { viewModel.setStyle(v) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = chipColors(uiState.style == v)
                        )
                    }
                }
            }

            // ── 长度选择 ──────────────
            item {
                Text("故事长度", style = MaterialTheme.typography.titleSmall, color = StarColors.OnBackground)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("short" to "短篇 (~800字)", "medium" to "中篇 (~2000字)", "long" to "长篇 (~4000字)").forEach { (v, label) ->
                        FilterChip(
                            selected = uiState.length == v,
                            onClick = { viewModel.setLength(v) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = chipColors(uiState.length == v)
                        )
                    }
                }
            }

            // ── 碎片列表标题 ──────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("选择碎片 (${uiState.selectedCount})", style = MaterialTheme.typography.titleSmall, color = StarColors.OnBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.selectAll() }) { Text("全选", style = MaterialTheme.typography.labelSmall) }
                        TextButton(onClick = { viewModel.clearSelection() }) { Text("清除", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            // ── 碎片列表 ──────────────
            if (uiState.fragments.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("还没有碎片，先去星系记录灵感吧 ✨", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                items(uiState.fragments, key = { it.id }) { frag ->
                    val isSelected = frag.id in uiState.selectedFragmentIds
                    val (emoji, label) = getDomainDisplay(frag.domainTag)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) StarColors.Primary.copy(alpha = 0.15f) else StarColors.Surface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        onClick = { viewModel.toggleFragment(frag.id) }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleFragment(frag.id) },
                                colors = CheckboxDefaults.colors(checkedColor = StarColors.Primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$emoji $label", style = MaterialTheme.typography.labelSmall, color = StarColors.Primary)
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(frag.content, style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // ── 生成按钮 ──────────────
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.generate() },
                    enabled = uiState.canGenerate,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StarColors.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = StarColors.OnPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("AI 正在创作...", color = StarColors.OnPrimary)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, tint = StarColors.OnPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("生成故事 (${uiState.selectedCount} 碎片)", color = StarColors.OnPrimary)
                    }
                }

                if (uiState.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("${uiState.error}", style = MaterialTheme.typography.bodySmall, color = StarColors.Error)
                }
            }
        }
    }
}

// ═════════════════════════════════════════
// 结果展示页
// ═════════════════════════════════════════

@Composable
private fun ResultView(
    story: com.startrace.core.database.entity.StoryEntity,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    isSaved: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // 风格/长度标签
        Row(verticalAlignment = Alignment.CenterVertically) {
            val styleLabel = when (story.style) { "scifi"->"🚀科幻" "fantasy"->"🧙奇幻" "realistic"->"📷现实" "prose"->"🌸散文" "poetry"->"🎵诗歌" "mystery"->"🔍悬疑" else->story.style }
            val lengthLabel = when (story.length) { "short"->"短篇" "medium"->"中篇" "long"->"长篇" else->story.length }
            Surface(color = StarColors.Primary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) { Text(styleLabel, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = StarColors.Primary) }
            Spacer(Modifier.width(8.dp))
            Surface(color = StarColors.Secondary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) { Text(lengthLabel, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = StarColors.Secondary) }
        }

        Spacer(Modifier.height(12.dp))

        // 标题
        Text(story.title, style = MaterialTheme.typography.headlineSmall, color = StarColors.OnBackground, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        // 故事内容（可滚动）
        LazyColumn(Modifier.weight(1f)) {
            item {
                Text(story.content, style = MaterialTheme.typography.bodyMedium, color = StarColors.OnSurface, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 按钮组
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, Modifier.weight(1f)) { Text("重选") }
            OutlinedButton(onClick = onRetry, Modifier.weight(1f)) { Text("重新生成") }
            Button(onClick = onSave, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StarColors.Primary)) {
                Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("保存")
            }
        }
    }
}

// ═════════════════════════════════════════
// 保存成功状态
// ═════════════════════════════════════════

@Composable
private fun SavedState(onReset: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))
            Text("故事已保存到星系！", style = MaterialTheme.typography.titleMedium, color = StarColors.OnBackground)
            Text("返回星系可以看到新的故事节点", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onReset) { Text("继续创作") }
        }
    }
}

@Composable
private fun chipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = StarColors.Primary.copy(alpha = 0.25f),
    selectedLabelColor = StarColors.Primary,
    containerColor = StarColors.Surface,
    labelColor = StarColors.OnSurface.copy(alpha = 0.7f)
)
