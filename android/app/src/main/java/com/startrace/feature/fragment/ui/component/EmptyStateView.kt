package com.startrace.feature.fragment.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startrace.design.component.StarButton
import com.startrace.design.theme.StarColors

/**
 * 碎片列表空状态
 *
 * 两种状态：
 * - noFragments: 还没有记录任何碎片
 * - noSearchResults: 搜索/筛选无结果
 */
@Composable
fun FragmentEmptyState(
    isSearchActive: Boolean,
    hasFragments: Boolean,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (isSearchActive) {
                // ── 无搜索结果 ─────────────────────
                Text(
                    text = "🔍",
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 48.sp
                )
                Text(
                    text = "没有找到匹配的碎片",
                    style = MaterialTheme.typography.titleMedium,
                    color = StarColors.OnBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "换个关键词或调整筛选条件试试",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StarColors.OnSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            } else {
                // ── 无碎片 ─────────────────────────
                Text(
                    text = "🌌",
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 56.sp
                )
                Text(
                    text = "你的灵感宇宙还是空的",
                    style = MaterialTheme.typography.titleMedium,
                    color = StarColors.OnBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "记录下第一个灵感碎片\n它们将化作漫天星辰",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StarColors.OnSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                StarButton(
                    onClick = onRecordClick,
                    text = "记录第一条灵感"
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun EmptyStateNoFragmentsPreview() {
    Box(modifier = Modifier.background(StarColors.Background)) {
        FragmentEmptyState(
            isSearchActive = false,
            hasFragments = false,
            onRecordClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun EmptyStateNoResultsPreview() {
    Box(modifier = Modifier.background(StarColors.Background)) {
        FragmentEmptyState(
            isSearchActive = true,
            hasFragments = true,
            onRecordClick = {}
        )
    }
}
