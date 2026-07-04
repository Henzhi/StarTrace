package com.startrace.feature.galaxy.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.core.database.entity.FragmentEntity
import com.startrace.core.database.entity.StoryEntity
import com.startrace.core.engine.NodeType
import com.startrace.design.theme.StarColors
import com.startrace.feature.fragment.ui.FragmentListScreen
import com.startrace.feature.fragment.ui.component.getDomainDisplay
import com.startrace.feature.galaxy.ui.canvas.GalaxyCanvas
import com.startrace.feature.galaxy.viewmodel.GalaxyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 星系页 — 画布/列表双模式 + 节点详情 Sheet + 悬浮气泡
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxyScreen(
    onNavigateToRecord: () -> Unit = {}
) {
    var viewMode by remember { mutableStateOf(ViewMode.CANVAS) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val galaxyViewModel: GalaxyViewModel? =
        if (viewMode == ViewMode.CANVAS) hiltViewModel() else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StarColors.Background)
    ) {
        // ═══ 画布模式 ═══════════════════════════
        if (viewMode == ViewMode.CANVAS && galaxyViewModel != null) {
            val uiState by galaxyViewModel.uiState.collectAsState()

            LaunchedEffect(uiState.selectedNodeId, uiState.draggingNodeId) {
                showSheet = uiState.selectedNodeId != null && uiState.draggingNodeId == null
            }

            GalaxyCanvas(viewModel = galaxyViewModel, modifier = Modifier.fillMaxSize())

            // 浮动按钮
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                color = StarColors.Surface.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.large
            ) {
                IconButton(onClick = { viewMode = ViewMode.LIST }) {
                    Icon(Icons.AutoMirrored.Outlined.ViewList, "列表", tint = StarColors.OnSurface)
                }
            }

            // 节点详情 Sheet
            if (showSheet && uiState.selectedNode != null) {
                NodeDetailSheet(
                    node = uiState.selectedNode!!,
                    fragment = uiState.selectedFragment,
                    linkedStories = uiState.linkedStories,
                    sheetState = sheetState,
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false; galaxyViewModel.deselectNode()
                        }
                    }
                )
            }
        }

        // ═══ 列表模式 ═══════════════════════════
        if (viewMode == ViewMode.LIST) {
            FragmentListScreen(onNavigateToRecord = onNavigateToRecord, modifier = Modifier.fillMaxSize())

            // 拖拽悬浮气泡
            CanvasBubble(onClick = { viewMode = ViewMode.CANVAS })
        }
    }
}

// ═══════════════════════════════════════════════════════
// 悬浮气泡 — 列表模式右下角半隐藏，可拖拽
// ═══════════════════════════════════════════════════════

@Composable
private fun BoxScope.CanvasBubble(onClick: () -> Unit) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val hideOffset = with(density) { 24.dp.toPx() }
    val edgeMargin = with(density) { 32.dp.toPx() }

    var bubbleW by remember { mutableIntStateOf(0) }
    var bubbleH by remember { mutableIntStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var initDone by remember { mutableStateOf(false) }

    if (!initDone && bubbleW > 0) {
        offsetX = screenWidthPx - hideOffset
        offsetY = screenHeightPx * 0.4f
        initDone = true
    }

    var dragging by remember { mutableStateOf(false) }
    val slideTarget = if (dragging) screenWidthPx - bubbleW - 8f else offsetX
    val animatedX by animateFloatAsState(slideTarget, spring(dampingRatio = 0.55f, stiffness = 350f))

    Surface(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(animatedX.toInt(), offsetY.toInt()) }
            .size(52.dp)
            .onSizeChanged { bubbleW = it.width; bubbleH = it.height }
            .pointerInput(screenWidthPx, screenHeightPx, bubbleW, bubbleH) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        offsetX = if (offsetX + bubbleW / 2f > screenWidthPx / 2f) {
                            screenWidthPx - hideOffset
                        } else {
                            -edgeMargin
                        }
                    },
                    onDragCancel = { dragging = false }
                ) { change, drag ->
                    change.consume()
                    offsetX = (offsetX + drag.x).coerceIn(-edgeMargin, screenWidthPx + edgeMargin - bubbleW)
                    offsetY = (offsetY + drag.y).coerceIn(0f, screenHeightPx - bubbleH)
                }
            }
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                onClick()
            },
        color = StarColors.Primary.copy(alpha = 0.82f),
        shape = RoundedCornerShape(50),
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("🌌", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// ═══════════════════════════════════════════════════════
// 节点详情 Sheet
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeDetailSheet(
    node: com.startrace.core.engine.GraphNode,
    fragment: FragmentEntity?,
    linkedStories: List<StoryEntity>,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val (emoji, domainLabel) = getDomainDisplay(node.domainTag)
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val timeText = fragment?.let { dateFormat.format(Date(it.createdAt)) } ?: ""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StarColors.Surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = StarColors.OnSurface.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = (if (node.nodeType == NodeType.FRAGMENT) StarColors.NodeFragment else StarColors.NodeStory).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (node.nodeType == NodeType.FRAGMENT) "$emoji $domainLabel" else "📖 故事节点",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (node.nodeType == NodeType.FRAGMENT) StarColors.NodeFragment else StarColors.NodeStory
                    )
                }
                if (!node.formTag.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = StarColors.Secondary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text(node.formTag, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = StarColors.Secondary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(node.label.ifBlank { "（无内容）" }, style = MaterialTheme.typography.bodyLarge, color = StarColors.OnBackground, maxLines = 8, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (node.nodeType == NodeType.FRAGMENT) "灵感碎片" else "故事", style = MaterialTheme.typography.labelSmall, color = StarColors.OnSurface.copy(alpha = 0.5f))
                Text(timeText, style = MaterialTheme.typography.labelSmall, color = StarColors.OnSurface.copy(alpha = 0.5f))
            }

            // 关联故事
            if (linkedStories.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = StarColors.OnSurface.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))
                Text("关联故事 (${linkedStories.size})", style = MaterialTheme.typography.labelLarge, color = StarColors.Primary)
                Spacer(Modifier.height(8.dp))
                linkedStories.forEach { story ->
                    Surface(
                        color = StarColors.Primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Text(
                            text = story.title,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = StarColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private enum class ViewMode { CANVAS, LIST }
