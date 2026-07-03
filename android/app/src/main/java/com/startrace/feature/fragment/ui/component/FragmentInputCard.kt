package com.startrace.feature.fragment.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startrace.design.theme.StarColors

private const val MAX_LENGTH = 500

/**
 * 碎片文本输入卡片
 *
 * 四种视觉状态：
 * - Empty: 占位提示 "现在想到了什么？"
 * - Focused: 边框高亮 + 占位提示消失
 * - Typing: 实时字数统计
 * - Max: 字数统计变红警告
 */
@Composable
fun FragmentInputCard(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFocused = content.isNotEmpty()
    val isAtMax = content.length >= MAX_LENGTH

    val borderColor by animateColorAsState(
        targetValue = when {
            isAtMax -> StarColors.Error
            isFocused -> StarColors.Primary
            else -> StarColors.SurfaceVariant
        },
        label = "borderColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .background(StarColors.Surface)
            .padding(20.dp)
    ) {
        Column {
            // 文本输入区
            BasicTextField(
                value = content,
                onValueChange = { newText ->
                    if (newText.length <= MAX_LENGTH) {
                        onContentChange(newText)
                    }
                },
                textStyle = TextStyle(
                    color = StarColors.OnBackground,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(StarColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                text = "现在想到了什么？\n一个画面、一句台词、一个设定…",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = StarColors.OnSurface.copy(alpha = 0.35f),
                                    lineHeight = 24.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 底部字数统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${content.length}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAtMax) StarColors.Error else StarColors.OnSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Text(
                    text = " / $MAX_LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = StarColors.OnSurface.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun EmptyInputCardPreview() {
    FragmentInputCard(content = "", onContentChange = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun TypingInputCardPreview() {
    FragmentInputCard(
        content = "在遥远的星系边缘，一艘孤独的飞船缓缓驶过恒星残骸…",
        onContentChange = {}
    )
}
