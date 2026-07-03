package com.startrace.feature.fragment.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.design.theme.StarColors
import com.startrace.feature.fragment.ui.component.*
import com.startrace.feature.fragment.viewmodel.RecordViewModel

@Composable
fun RecordScreen(
    viewModel: RecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 保存成功 → 震动反馈
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            triggerHaptic(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StarColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .imePadding()
    ) {
        // 标题
        Text(
            text = "记录碎片",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = StarColors.OnBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "捕捉瞬间灵感，化作星辰碎片",
            style = MaterialTheme.typography.bodyMedium,
            color = StarColors.OnSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. 文本输入
        FragmentInputCard(
            content = uiState.content,
            onContentChange = viewModel::updateContent
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 领域标签（必选）
        DomainTagSelector(
            selectedTag = uiState.domainTag,
            onTagSelected = viewModel::selectDomainTag
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 内容形态标签（可选）
        FormTagSelector(
            selectedTag = uiState.formTag,
            onTagSelected = viewModel::selectFormTag
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. 情绪选择（可选）
        Text(
            text = "当前心情",
            style = MaterialTheme.typography.labelLarge,
            color = StarColors.OnSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        MoodPicker(
            selectedMood = uiState.mood,
            onMoodSelected = viewModel::selectMood
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 5. 错误提示
        val errorMsg = uiState.error
        AnimatedVisibility(
            visible = errorMsg != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            if (errorMsg != null) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = StarColors.Error,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // 6. 保存按钮
        SaveButton(
            enabled = uiState.canSave,
            isSaving = uiState.isSaving,
            onClick = viewModel::save
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 保存成功弹窗
    if (uiState.saveSuccess) {
        SaveSuccessDialog(onDismiss = viewModel::clearSuccess)
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        !enabled -> StarColors.SurfaceVariant
        else -> StarColors.Primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSaving) {
            Text(
                text = "收集中...",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = StarColors.OnPrimary
            )
        } else {
            Text(
                text = "化作星辰 ✨",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (enabled) StarColors.OnPrimary else StarColors.OnSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun SaveSuccessDialog(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StarColors.Background.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "✨", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "星辰已记录",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = StarColors.Primary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "点击任意处继续",
                style = MaterialTheme.typography.bodySmall,
                color = StarColors.OnSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private fun triggerHaptic(context: android.content.Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
