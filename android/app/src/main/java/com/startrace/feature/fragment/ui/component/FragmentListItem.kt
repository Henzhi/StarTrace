package com.startrace.feature.fragment.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startrace.core.database.entity.FragmentEntity
import com.startrace.design.theme.StarColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * 碎片列表项组件
 *
 * 支持两种模式：
 * - 普通模式：点击触发操作（跳转详情）
 * - 选择模式：左侧显示复选框，点击切换选中状态
 */
@Composable
fun FragmentListItem(
    fragment: FragmentEntity,
    domainEmoji: String,
    domainLabel: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = rememberDateFormat()

    val itemBg by animateColorAsState(
        targetValue = when {
            isSelected -> StarColors.Primary.copy(alpha = 0.12f)
            else -> StarColors.Surface
        },
        label = "itemBg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = {
                    if (isSelectionMode) onClick() // toggle selection
                    else onClick()
                },
                onClickLabel = if (isSelectionMode) "选择碎片" else "查看碎片"
            ),
        shape = RoundedCornerShape(12.dp),
        color = itemBg
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 多选复选框
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle
                    else Icons.Outlined.Circle,
                    contentDescription = if (isSelected) "已选中" else "未选中",
                    tint = if (isSelected) StarColors.Primary
                    else StarColors.OnSurface.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            // 领域标签色块
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(CircleShape)
                    .background(StarColors.Primary.copy(alpha = 0.15f))
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = domainEmoji, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 内容
                Text(
                    text = fragment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StarColors.OnBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 底部：领域标签 + 时间
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = domainLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = StarColors.Primary.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (fragment.mood != null) {
                        Text(
                            text = moodEmoji(fragment.mood),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "·",
                        color = StarColors.OnSurface.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = dateFormat.format(Date(fragment.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = StarColors.OnSurface.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/** 情绪 → emoji */
private fun moodEmoji(mood: String): String = when (mood) {
    "excited" -> "🔥"
    "calm" -> "🌿"
    "confused" -> "❓"
    "amazed" -> "✨"
    else -> ""
}

/** 避免每次重组创建 SimpleDateFormat */
@Composable
private fun rememberDateFormat(): SimpleDateFormat {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
}

/**
 * 根据 domainTag key 获取对应 emoji 和中文标签
 */
fun getDomainDisplay(tag: String): Pair<String, String> {
    return DOMAIN_TAGS.find { it.key == tag }
        ?.let { it.emoji to it.label }
        ?: ("📌" to tag)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun FragmentListItemPreview() {
    Column(
        modifier = Modifier
            .background(StarColors.Background)
            .padding(16.dp)
    ) {
        FragmentListItem(
            fragment = FragmentEntity(
                id = "1",
                content = "在量子海的最深处，所有可能性同时存在，就像薛定谔的猫既是死的也是活的，直到观察者出现的那一刻。",
                tagsJson = "[]",
                domainTag = "thought",
                mood = "amazed",
                createdAt = System.currentTimeMillis()
            ),
            domainEmoji = "💡",
            domainLabel = "哲思",
            isSelectionMode = false,
            isSelected = false,
            onClick = {},
            onLongClick = {}
        )
    }
}
