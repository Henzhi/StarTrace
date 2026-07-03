package com.startrace.feature.fragment.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.startrace.design.theme.StarColors

/**
 * 批量操作栏 — 多选模式下显示在底部
 *
 * 显示选中数量 badge，提供批量删除/归档操作。
 */
@Composable
fun BatchActionBar(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StarColors.SurfaceVariant,
        shadowElevation = 8.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消选择",
                    tint = StarColors.OnSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 选中数量 badge
            Surface(
                color = StarColors.Primary.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "已选 $selectedCount 项",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = StarColors.Primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 归档按钮
            TextButton(
                onClick = onArchive,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = StarColors.Secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("归档")
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 删除按钮
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = StarColors.Error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("删除")
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun BatchActionBarPreview() {
    Box(modifier = Modifier.background(StarColors.Background)) {
        BatchActionBar(
            selectedCount = 3,
            onDismiss = {},
            onDelete = {},
            onArchive = {}
        )
    }
}
