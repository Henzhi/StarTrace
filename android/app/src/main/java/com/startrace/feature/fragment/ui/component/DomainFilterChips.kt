package com.startrace.feature.fragment.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * 领域标签多选筛选器
 *
 * 复用 DomainTag 数据模型；支持多选 toggle，选中高亮。
 */
@Composable
fun DomainFilterChips(
    selectedDomains: Set<String>,
    onToggleDomain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 筛选图标（点击清除全部筛选）
        if (selectedDomains.isNotEmpty()) {
            IconButton(
                onClick = { DOMAIN_TAGS.forEach { onToggleDomain(it.key) } },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "筛选",
                    tint = StarColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(DOMAIN_TAGS) { tag ->
                val isSelected = tag.key in selectedDomains

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) StarColors.Primary.copy(alpha = 0.25f)
                    else StarColors.Surface,
                    label = "chipBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) StarColors.Primary.copy(alpha = 0.6f)
                    else Color.Transparent,
                    label = "chipBorder"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) StarColors.Primary
                    else StarColors.OnSurface.copy(alpha = 0.6f),
                    label = "chipText"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor, RoundedCornerShape(16.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { onToggleDomain(tag.key) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${tag.emoji} ${tag.label}",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun DomainFilterChipsPreview() {
    Column(
        modifier = Modifier
            .background(StarColors.Background)
            .padding(16.dp)
    ) {
        DomainFilterChips(
            selectedDomains = setOf("character", "thought"),
            onToggleDomain = {}
        )
    }
}
