package com.startrace.feature.galaxy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.startrace.design.theme.StarColors
import com.startrace.feature.fragment.ui.FragmentListScreen

/**
 * 星系页 — 碎片列表主入口
 *
 * 展示所有灵感碎片，支持搜索、筛选、滑动删除和批量操作。
 * "记录"按钮导航至底部 Record Tab。
 */
@Composable
fun GalaxyScreen(
    onNavigateToRecord: () -> Unit = {}
) {
    FragmentListScreen(
        onNavigateToRecord = onNavigateToRecord,
        modifier = Modifier
            .fillMaxSize()
            .background(StarColors.Background)
    )
}
