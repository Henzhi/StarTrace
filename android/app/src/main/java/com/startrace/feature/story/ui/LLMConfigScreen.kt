package com.startrace.feature.story.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.core.database.entity.LLMConfigEntity
import com.startrace.design.theme.StarColors
import com.startrace.feature.story.viewmodel.LLMConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLMConfigScreen(
    viewModel: LLMConfigViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = StarColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("LLM 配置", color = StarColors.OnBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StarColors.Surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateForm() },
                containerColor = StarColors.Primary
            ) { Icon(Icons.Default.Add, "添加配置", tint = StarColors.OnPrimary) }
        }
    ) { padding ->
        if (uiState.configs.isEmpty()) {
            // 空状态
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤖", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("还没有 LLM 配置", style = MaterialTheme.typography.titleMedium, color = StarColors.OnBackground)
                    Text("添加 OpenAI 兼容的 API 地址", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.openCreateForm() }) {
                        Text("添加配置")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.configs, key = { it.id }) { config ->
                    ConfigCard(
                        config = config,
                        onEdit = { viewModel.openEditForm(config) },
                        onDelete = { viewModel.delete(config) }
                    )
                }
            }
        }
    }

    // 新增/编辑弹窗
    if (uiState.showForm) {
        ConfigFormDialog(
            form = uiState.form,
            isEditing = uiState.editingConfig != null,
            isSaving = uiState.isSaving,
            isTesting = uiState.isTesting,
            testResult = uiState.testResult,
            onDismiss = { viewModel.dismissForm() },
            onNameChange = { viewModel.updateName(it) },
            onUrlChange = { viewModel.updateUrl(it) },
            onModelChange = { viewModel.updateModel(it) },
            onKeyChange = { viewModel.updateApiKey(it) },
            onDefaultChange = { viewModel.updateDefault(it) },
            onSave = { viewModel.save() },
            onTest = { viewModel.testConnection() }
        )
    }
}

// ═══════════════════════════════════════════════════════
// 配置卡片
// ═══════════════════════════════════════════════════════

@Composable
private fun ConfigCard(
    config: LLMConfigEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StarColors.Surface),
        shape = RoundedCornerShape(12.dp),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (config.isDefault) {
                    Surface(
                        color = StarColors.Primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "默认", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = StarColors.Primary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(config.name, style = MaterialTheme.typography.titleSmall, color = StarColors.OnBackground, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = StarColors.Error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(config.modelName, style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(config.apiUrl, style = MaterialTheme.typography.labelSmall, color = StarColors.OnSurface.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除配置") },
            text = { Text("确定删除「${config.name}」吗？关联的 API Key 也将被移除。") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }, colors = ButtonDefaults.textButtonColors(contentColor = StarColors.Error)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } },
            containerColor = StarColors.Surface
        )
    }
}

// ═══════════════════════════════════════════════════════
// 表单弹窗
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigFormDialog(
    form: com.startrace.feature.story.viewmodel.LLMConfigForm,
    isEditing: Boolean,
    isSaving: Boolean,
    isTesting: Boolean,
    testResult: String?,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onDefaultChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit
) {
    var showKey by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "编辑配置" else "新增配置", color = StarColors.OnBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.name, onValueChange = onNameChange, label = { Text("配置名称 *") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = form.apiUrl, onValueChange = onUrlChange, label = { Text("API 地址 *") },
                    placeholder = { Text("https://api.openai.com") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Uri),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = form.modelName, onValueChange = onModelChange, label = { Text("模型名称 *") },
                    placeholder = { Text("gpt-4o") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = form.apiKey, onValueChange = onKeyChange, label = { Text("API Key") },
                    placeholder = { Text(if (isEditing && form.apiKey.isBlank()) "留空则保留原 Key" else "sk-...") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "toggle")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    colors = fieldColors()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = form.isDefault,
                        onCheckedChange = onDefaultChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = StarColors.Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("设为默认", style = MaterialTheme.typography.bodyMedium, color = StarColors.OnSurface)
                }

                // 测试结果
                if (testResult != null) {
                    Surface(
                        color = if (testResult.startsWith("✅")) StarColors.MoodCalm.copy(alpha = 0.15f) else StarColors.Error.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(testResult, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onTest, enabled = form.isValid && !isTesting) {
                    if (isTesting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = StarColors.Primary)
                    else Text("测试连接")
                }
                Button(onClick = onSave, enabled = form.isValid && !isSaving) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = StarColors.OnPrimary)
                    else Text("保存")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = StarColors.Surface,
        titleContentColor = StarColors.OnBackground
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StarColors.Primary,
    unfocusedBorderColor = StarColors.SurfaceVariant,
    focusedLabelColor = StarColors.Primary,
    cursorColor = StarColors.Primary,
    focusedTextColor = StarColors.OnBackground,
    unfocusedTextColor = StarColors.OnBackground
)
