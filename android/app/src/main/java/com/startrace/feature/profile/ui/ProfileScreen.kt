package com.startrace.feature.profile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.design.theme.StarColors
import com.startrace.feature.profile.viewmodel.ProfileViewModel
import android.content.Intent
import android.net.Uri

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToLLMConfig: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0A0A0F), Color(0xFF0D0D1A), Color(0xFF1A1A3E)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Spacer(Modifier.height(32.dp))
            Text("我的", style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 0.5.sp), color = StarColors.OnBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("管理你的创作空间", style = MaterialTheme.typography.bodyMedium, color = StarColors.OnSurface.copy(alpha = 0.5f))

            Spacer(Modifier.height(24.dp))

            Surface(color = StarColors.Surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = StarColors.Primary.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(uiState.username.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall, color = StarColors.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (uiState.isEditingName) {
                            OutlinedTextField(
                                value = uiState.editNameValue,
                                onValueChange = viewModel::updateEditName,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { viewModel.saveUsername() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StarColors.Primary,
                                    unfocusedBorderColor = StarColors.SurfaceVariant,
                                    focusedLabelColor = StarColors.Primary,
                                    cursorColor = StarColors.Primary
                                ),
                                textStyle = MaterialTheme.typography.titleMedium.copy(color = StarColors.OnBackground)
                            )
                        } else {
                            Text(uiState.username, style = MaterialTheme.typography.titleMedium, color = StarColors.OnBackground, fontWeight = FontWeight.SemiBold)
                            Text("星迹创作者", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.5f))
                        }
                    }
                    if (uiState.isEditingName) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = viewModel::cancelEditName) {
                                Icon(Icons.Default.Close, "取消", tint = StarColors.OnSurface.copy(alpha = 0.5f))
                            }
                            IconButton(onClick = viewModel::saveUsername) {
                                Icon(Icons.Default.Check, "保存", tint = StarColors.Primary)
                            }
                        }
                    } else {
                        IconButton(onClick = viewModel::startEditName) {
                            Icon(Icons.Default.Edit, "编辑", tint = StarColors.OnSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (uiState.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.error!!, style = MaterialTheme.typography.bodySmall, color = StarColors.Error)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(icon = Icons.Default.Lightbulb, label = "灵感碎片", count = uiState.fragmentCount, modifier = Modifier.weight(1f))
                StatCard(icon = Icons.Default.Book, label = "生成故事", count = uiState.storyCount, modifier = Modifier.weight(1f))
                StatCard(icon = Icons.Default.Settings, label = "LLM配置", count = uiState.configCount, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            Surface(color = StarColors.Surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    ProfileMenuItem(icon = Icons.Default.Settings, label = "LLM 配置", onClick = onNavigateToLLMConfig)
                    ProfileMenuItem(icon = Icons.Default.Info, label = "关于星迹", onClick = { showAboutDialog = true })
                    ProfileMenuItem(icon = Icons.Default.Mail, label = "意见反馈", onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:startrace2026@163.com")
                            putExtra(Intent.EXTRA_SUBJECT, "星迹 StarTrace 意见反馈")
                        }
                        context.startActivity(intent)
                    })
                    ProfileMenuItem(icon = Icons.Default.Share, label = "分享应用", onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "星迹 StarTrace - 捕捉瞬间灵感，化作星辰碎片\nhttps://github.com/Henzhi/StarTrace")
                    }
                    context.startActivity(Intent.createChooser(intent, "分享星迹"))
                })
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("v0.1.0 · 星迹 StarTrace", style = MaterialTheme.typography.labelSmall, color = StarColors.OnSurface.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    AboutDialog(show = showAboutDialog, onDismiss = { showAboutDialog = false })
}

@Composable
private fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(color = StarColors.Surface, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = StarColors.Primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, color = StarColors.OnBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = StarColors.OnSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = StarColors.OnSurface.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = StarColors.OnSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = StarColors.OnSurface.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = StarColors.OnSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun AboutDialog(show: Boolean, onDismiss: () -> Unit) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("关于星迹", color = StarColors.OnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✨ 捕捉瞬间灵感，化作星辰碎片", style = MaterialTheme.typography.bodyMedium, color = StarColors.OnSurface)
                    Spacer(Modifier.height(8.dp))
                    Text("版本 v0.1.0", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.5f))
                    Text("Android 应用", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("星迹 StarTrace 是一款帮助创作者收集灵感、生成故事的工具应用。", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.7f))
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("知道了") }
            },
            containerColor = StarColors.Surface,
            titleContentColor = StarColors.OnBackground,
            textContentColor = StarColors.OnSurface
        )
    }
}