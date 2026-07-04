package com.startrace.feature.fragment.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.core.database.entity.FragmentEntity
import com.startrace.design.theme.StarColors
import com.startrace.feature.fragment.ui.component.*
import com.startrace.feature.fragment.viewmodel.FragmentListViewModel
import kotlinx.coroutines.launch

/**
 * 碎片列表页 — 搜索、筛选、滑动删除、批量操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentListScreen(
    viewModel: FragmentListViewModel = hiltViewModel(),
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // ── 确认弹窗状态 ────────────────────────────────────
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var swipeTarget by remember { mutableStateOf<FragmentEntity?>(null) }

    // ── 监听 Snackbar 事件 ──────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (message == "已删除") "撤销" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StarColors.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ══ 标题（与 记录碎片 / 故事库 同款风格） ══
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "星辰编织",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = StarColors.OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "浏览与管理你的灵感碎片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StarColors.OnSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ══ 搜索栏 ═══════════════════════════════
            StarSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                resultCount = uiState.resultCount,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ══ 领域筛选 Chips ═══════════════════════
            DomainFilterChips(
                selectedDomains = uiState.selectedDomains,
                onToggleDomain = { viewModel.toggleDomainFilter(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ══ 列表 / 空状态 ═══════════════════════
            if (uiState.isLoading && uiState.fragments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StarColors.Primary)
                }
            } else if (uiState.fragments.isEmpty()) {
                FragmentEmptyState(
                    isSearchActive = uiState.isFilterActive,
                    hasFragments = uiState.totalCount > 0,
                    onRecordClick = onNavigateToRecord
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 4.dp, bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.fragments,
                        key = { it.id }
                    ) { fragment ->
                        val (emoji, label) = getDomainDisplay(fragment.domainTag)
                        val isSelected = fragment.id in uiState.selectedIds

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    swipeTarget = fragment
                                    true
                                } else false
                            }
                        )

                        // 滑动删除仅在非选择模式下启用
                        if (!uiState.isSelectionMode) {
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val bgColor by animateColorAsState(
                                        targetValue = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> StarColors.Error
                                            else -> StarColors.Surface
                                        },
                                        label = "swipeBg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bgColor),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = StarColors.OnPrimary,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                            ) {
                                FragmentListItem(
                                    fragment = fragment,
                                    domainEmoji = emoji,
                                    domainLabel = label,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        focusManager.clearFocus()
                                        // TODO: navigate to detail
                                    },
                                    onLongClick = {
                                        viewModel.enterSelectionMode(fragment.id)
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        } else {
                            // 选择模式：不使用滑动删除，点击切换选中
                            FragmentListItem(
                                fragment = fragment,
                                domainEmoji = emoji,
                                domainLabel = label,
                                isSelectionMode = true,
                                isSelected = isSelected,
                                onClick = { viewModel.toggleSelection(fragment.id) },
                                onLongClick = {},
                                modifier = Modifier.animateItem()
                            )
                        }

                        // ── 滑动删除执行 ────────────
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                swipeTarget?.let { viewModel.swipeDelete(it) }
                                swipeTarget = null
                            }
                        }
                    }
                }
            }
        }

        // ══ Snackbar ══════════════════════════════
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ══ 批量操作栏（选择模式下显示） ════════════
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BatchActionBar(
                selectedCount = uiState.selectedIds.size,
                onDismiss = { viewModel.exitSelectionMode() },
                onDelete = { showDeleteDialog = true },
                onArchive = { showArchiveDialog = true }
            )
        }
    }

    // ══ 批量删除确认弹窗 ═══════════════════════════
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("将删除选中的 ${uiState.selectedIds.size} 条碎片，此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.batchDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StarColors.Error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
            containerColor = StarColors.Surface,
            titleContentColor = StarColors.OnBackground,
            textContentColor = StarColors.OnSurface
        )
    }

    // ══ 批量归档确认弹窗 ═══════════════════════════
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("确认归档") },
            text = { Text("将归档选中的 ${uiState.selectedIds.size} 条碎片，稍后可在归档中查看。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveDialog = false
                        viewModel.batchArchive()
                    }
                ) { Text("归档") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("取消") }
            },
            containerColor = StarColors.Surface,
            titleContentColor = StarColors.OnBackground,
            textContentColor = StarColors.OnSurface
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun FragmentListScreenPreview() {
    Box(modifier = Modifier.background(StarColors.Background)) {
        Text(
            text = "FragmentListScreen — 预览请运行完整 app",
            color = StarColors.OnSurface
        )
    }
}
