package com.startrace.feature.story.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.design.theme.StarColors
import com.startrace.feature.fragment.ui.component.DOMAIN_TAGS
import com.startrace.feature.story.viewmodel.StoryGeneratorViewModel

/**
 * 故事生成器 — 碎片选择 + 风格/长度 + SSE 流式生成
 */
@Composable
fun StoryGeneratorView(
    onClose: () -> Unit,
    viewModel: StoryGeneratorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.reset() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StarColors.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ══ 标题（与 记录碎片 / 故事库 同款风格） ══
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = StarColors.OnSurface)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "星辰编织",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = StarColors.OnBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "将灵感碎片编织成星辰故事",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StarColors.OnSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (uiState.savedStoryId != null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("故事已保存！", style = MaterialTheme.typography.titleMedium, color = StarColors.OnBackground)
                        Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = StarColors.Primary)) { Text("回到故事库") }
                    }
                }
                return@Box
            }

            if (uiState.isStreaming) {
                StreamingView(text = uiState.streamingTokens, modifier = Modifier.weight(1f))
                return@Box
            }

            if (uiState.result != null) {
                ResultView(story = uiState.result!!, onSave = { viewModel.saveStory() }, onRetry = { viewModel.generate() }, onBack = { viewModel.reset() }, modifier = Modifier.weight(1f))
                return@Box
            }

            // 碎片选择 + 风格/长度
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("故事风格", style = MaterialTheme.typography.titleSmall, color = StarColors.OnBackground); Spacer(Modifier.height(6.dp))
                    val styles = listOf("scifi" to "🚀科幻", "fantasy" to "🧙奇幻", "realistic" to "📷现实", "prose" to "🌸散文", "poetry" to "🎵诗歌", "mystery" to "🔍悬疑")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(styles) { (v, l) ->
                            FilterChip(selected = uiState.style == v, onClick = { viewModel.setStyle(v) }, label = { Text(l, style = MaterialTheme.typography.labelSmall) }, colors = chipColors(uiState.style == v))
                        }
                    }
                }
                item { Text("故事长度", style = MaterialTheme.typography.titleSmall, color = StarColors.OnBackground); Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("short" to "短篇", "medium" to "中篇", "long" to "长篇", "custom" to "自定义").forEach { (v, l) ->
                            FilterChip(selected = uiState.length == v, onClick = { viewModel.setLength(v) }, label = { Text(l, style = MaterialTheme.typography.labelSmall) }, colors = chipColors(uiState.length == v))
                        }
                    }
                    if (uiState.length == "custom") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.customLengthText,
                            onValueChange = { viewModel.setCustomLength(it) },
                            label = { Text("期望字数", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("例如：1500 字", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.4f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StarColors.Primary,
                                unfocusedBorderColor = StarColors.SurfaceVariant,
                                focusedLabelColor = StarColors.Primary,
                                cursorColor = StarColors.Primary
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = StarColors.OnSurface)
                        )
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("选择碎片 (${uiState.selectedCount})", style = MaterialTheme.typography.titleSmall, color = StarColors.OnBackground)
                        Row { TextButton(onClick = { viewModel.selectAll() }) { Text("全选", style = MaterialTheme.typography.labelSmall) }; TextButton(onClick = { viewModel.clearSelection() }) { Text("清除", style = MaterialTheme.typography.labelSmall) } }
                    }
                }
                if (uiState.fragments.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { Text("还没有碎片，先去星系记录灵感吧 ✨", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.5f)) } }
                } else {
                    items(uiState.fragments, key = { it.id }) { frag ->
                        val sel = frag.id in uiState.selectedFragmentIds
                        val domain = DOMAIN_TAGS.find { it.key == frag.domainTag }
                        val (emoji, label) = if (domain != null) Pair(domain.emoji, domain.label) else Pair("📌", frag.domainTag)
                        Card(onClick = { viewModel.toggleFragment(frag.id) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (sel) StarColors.Primary.copy(alpha = 0.15f) else StarColors.Surface), shape = RoundedCornerShape(10.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Checkbox(checked = sel, onCheckedChange = { viewModel.toggleFragment(frag.id) }, colors = CheckboxDefaults.colors(checkedColor = StarColors.Primary)); Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) { Text("$emoji $label", style = MaterialTheme.typography.labelSmall, color = StarColors.Primary); Spacer(Modifier.height(2.dp)); Text(frag.content, style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.generate() }, enabled = uiState.canGenerate, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = StarColors.Primary), shape = RoundedCornerShape(14.dp)) {
                        if (uiState.isGenerating) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = StarColors.OnPrimary); Spacer(Modifier.width(8.dp)); Text("准备中...", color = StarColors.OnPrimary) }
                        else { Icon(Icons.Default.AutoAwesome, null, tint = StarColors.OnPrimary); Spacer(Modifier.width(8.dp)); Text("生成故事 (${uiState.selectedCount} 碎片)", color = StarColors.OnPrimary) }
                    }
                    if (uiState.error != null) { Spacer(Modifier.height(8.dp)); Text(uiState.error!!, style = MaterialTheme.typography.bodySmall, color = StarColors.Error) }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun StreamingView(text: String, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState(); val lines = text.lines()
    LaunchedEffect(text) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1) }
    Column(modifier.padding(horizontal = 20.dp)) {
        Text("✨ AI 正在创作...", style = MaterialTheme.typography.titleSmall, color = StarColors.Primary); Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(Modifier.fillMaxWidth(), color = StarColors.Primary, trackColor = StarColors.SurfaceVariant); Spacer(Modifier.height(12.dp))
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) { items(lines) { line -> Text(line.ifBlank { "\u00A0" }, style = MaterialTheme.typography.bodyMedium, color = StarColors.OnSurface, modifier = Modifier.padding(vertical = 2.dp)) } }
    }
}

@Composable
private fun ResultView(story: com.startrace.core.database.entity.StoryEntity, onSave: () -> Unit, onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val styleLabel = when (story.style) { "scifi"->"🚀科幻" "fantasy"->"🧙奇幻" "realistic"->"📷现实" "prose"->"🌸散文" "poetry"->"🎵诗歌" "mystery"->"🔍悬疑" else->story.style }
            Surface(color = StarColors.Primary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) { Text(styleLabel, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = StarColors.Primary) }
        }; Spacer(Modifier.height(12.dp))
        Text(story.title, style = MaterialTheme.typography.headlineSmall, color = StarColors.OnBackground, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f)) { item { Text(story.content, style = MaterialTheme.typography.bodyMedium, color = StarColors.OnSurface) } }; Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, Modifier.weight(0.8f)) { Text("重选", maxLines = 1) }; OutlinedButton(onClick = onRetry, Modifier.weight(1.2f)) { Text("重新生成", maxLines = 1) }
            Button(onClick = onSave, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StarColors.Primary)) { Icon(Icons.Default.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("保存", maxLines = 1) }
        }
    }
}

@Composable
private fun chipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(selectedContainerColor = StarColors.Primary.copy(alpha = 0.25f), selectedLabelColor = StarColors.Primary, containerColor = StarColors.Surface, labelColor = StarColors.OnSurface.copy(alpha = 0.7f))
