package com.startrace.feature.story.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.core.database.entity.StoryEntity
import com.startrace.design.theme.StarColors
import com.startrace.feature.story.viewmodel.StoryListViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 故事页 — 故事库列表 + 生成入口
 */
@Composable
fun StoryScreen(
    onNavigateToGenerate: () -> Unit = {}
) {
    val listViewModel: StoryListViewModel = hiltViewModel()
    val uiState by listViewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    var selectedStory by remember { mutableStateOf<StoryEntity?>(null) }
    var showGenerator by remember { mutableStateOf(false) }

    // 生成器页面
    if (showGenerator) {
        StoryGeneratorView(onClose = { showGenerator = false })
        return
    }

    if (selectedStory != null) {
        StoryReadScreen(story = selectedStory!!, onBack = { selectedStory = null })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StarColors.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ══ 标题（与 记录碎片 同款风格） ══
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "故事库",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = StarColors.OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI 将灵感碎片编织成的星辰故事",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StarColors.OnSurface.copy(alpha = 0.5f)
                )
            }

            if (uiState.stories.isEmpty() && !uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📖", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("星辰书库", style = MaterialTheme.typography.titleMedium, color = StarColors.OnBackground)
                        Text("用 AI 将灵感碎片编织成故事", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onNavigateToGenerate, colors = ButtonDefaults.buttonColors(containerColor = StarColors.Primary)) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("生成第一个故事")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.stories, key = { it.id }) { story ->
                        StoryCard(story = story, dateFormat = dateFormat, onClick = { selectedStory = story })
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showGenerator = true },
            containerColor = StarColors.Primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "生成故事", tint = StarColors.OnPrimary)
        }
    }
}

@Composable
private fun StoryCard(story: StoryEntity, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    val styleLabel = when (story.style) { "scifi"->"🚀" "fantasy"->"🧙" "realistic"->"📷" "prose"->"🌸" "poetry"->"🎵" "mystery"->"🔍" else->"📖" }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = StarColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(styleLabel, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(story.title, style = MaterialTheme.typography.titleSmall, color = StarColors.OnBackground, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(story.content.take(80) + if (story.content.length > 80) "..." else "", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateFormat.format(Date(story.createdAt)), style = MaterialTheme.typography.labelSmall, color = StarColors.OnSurface.copy(alpha = 0.4f))
                val lenLabel = when (story.length) { "short"->"短" "medium"->"中" "long"->"长" else->"" }
                Surface(color = StarColors.Secondary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text(lenLabel, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = StarColors.Secondary)
                }
            }
        }
    }
}
