package com.startrace.feature.fragment.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startrace.design.theme.StarColors

/**
 * 系统预设领域标签
 *
 * 标签数组：世界/人物/情节/对话/设定/哲思
 * 每个标签带 emoji 图标和中文名
 */
data class DomainTag(
    val key: String,
    val emoji: String,
    val label: String
)

val DOMAIN_TAGS = listOf(
    DomainTag("world",    "🌍", "世界"),
    DomainTag("character","👤", "人物"),
    DomainTag("plot",     "📖", "情节"),
    DomainTag("dialogue", "💬", "对话"),
    DomainTag("setting",  "⚙️", "设定"),
    DomainTag("thought",  "💡", "哲思")
)

val FORM_TAGS = listOf(
    DomainTag("scene",     "🎬", "场景"),
    DomainTag("role",      "🧑", "角色"),
    DomainTag("line",      "🗣", "台词"),
    DomainTag("concept",   "🧩", "概念"),
    DomainTag("conflict",  "⚡", "冲突"),
    DomainTag("twist",     "🔄", "转折")
)

/**
 * 领域标签横滚选择器 — 必选
 */
@Composable
fun DomainTagSelector(
    selectedTag: String,
    onTagSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "领域",
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            color = StarColors.OnSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DOMAIN_TAGS) { tag ->
                TagChip(
                    emoji = tag.emoji,
                    label = tag.label,
                    isSelected = selectedTag == tag.key,
                    selectedColor = StarColors.Primary,
                    onClick = { onTagSelected(tag.key) }
                )
            }
        }
    }
}

/**
 * 内容形态标签选择器 — 可选，点击可取消
 */
@Composable
fun FormTagSelector(
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "形态（可选）",
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            color = StarColors.OnSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FORM_TAGS) { tag ->
                TagChip(
                    emoji = tag.emoji,
                    label = tag.label,
                    isSelected = selectedTag == tag.key,
                    selectedColor = StarColors.Secondary,
                    onClick = {
                        onTagSelected(if (selectedTag == tag.key) null else tag.key)
                    }
                )
            }
        }
    }
}

@Composable
private fun TagChip(
    emoji: String,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor.copy(alpha = 0.2f) else StarColors.Surface,
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else StarColors.SurfaceVariant,
        label = "chipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else StarColors.OnSurface.copy(alpha = 0.6f),
        label = "chipText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor, RoundedCornerShape(20.dp))
            .then(
                if (isSelected) Modifier else Modifier.border(1.dp, borderColor, RoundedCornerShape(20.dp))
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun DomainTagSelectorPreview() {
    Column(
        modifier = Modifier
            .background(StarColors.Background)
            .padding(16.dp)
    ) {
        DomainTagSelector(selectedTag = "world", onTagSelected = {})
        Spacer(modifier = Modifier.height(16.dp))
        FormTagSelector(selectedTag = null, onTagSelected = {})
    }
}
