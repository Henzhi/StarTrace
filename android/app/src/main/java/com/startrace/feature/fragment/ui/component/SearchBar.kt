package com.startrace.feature.fragment.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startrace.design.theme.StarColors

/**
 * 碎片搜索栏 — 实时过滤
 *
 * 绑定 Room Flow，输入即触发过滤。
 */
@Composable
fun StarSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索灵感碎片..."
) {
    val bgColor by animateColorAsState(
        targetValue = StarColors.Surface,
        label = "searchBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (query.isNotBlank()) StarColors.Primary.copy(alpha = 0.4f)
        else StarColors.SurfaceVariant,
        label = "searchBorder"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor, RoundedCornerShape(12.dp))
                .then(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = if (query.isNotBlank()) StarColors.Primary
                else StarColors.OnSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                singleLine = true,
                textStyle = TextStyle(
                    color = StarColors.OnBackground,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(StarColors.Primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = StarColors.OnSurface.copy(alpha = 0.4f),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotBlank()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除搜索",
                        tint = StarColors.OnSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        // 结果计数（仅在搜索/筛选激活时显示）
        if (query.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "找到 $resultCount 条结果",
                style = MaterialTheme.typography.labelSmall,
                color = StarColors.OnSurface.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun SearchBarPreview() {
    Column(
        modifier = Modifier
            .background(StarColors.Background)
            .padding(16.dp)
    ) {
        StarSearchBar(
            query = "科幻",
            onQueryChange = {},
            resultCount = 3
        )
    }
}
