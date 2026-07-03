package com.startrace.feature.story.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.LLMConfigRepository
import com.startrace.core.database.entity.LLMConfigEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/** LLM 配置表单数据 */
data class LLMConfigForm(
    val name: String = "",
    val apiUrl: String = "",
    val modelName: String = "",
    val apiKey: String = "",
    val isDefault: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank() && apiUrl.isNotBlank() && modelName.isNotBlank()
}

/** LLM 配置 UI 状态 */
data class LLMConfigUiState(
    val configs: List<LLMConfigEntity> = emptyList(),
    val showForm: Boolean = false,
    val editingConfig: LLMConfigEntity? = null,
    val form: LLMConfigForm = LLMConfigForm(),
    val testResult: String? = null,
    val isTesting: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class LLMConfigViewModel @Inject constructor(
    private val repository: LLMConfigRepository
) : ViewModel() {

    private val _showForm = MutableStateFlow(false)
    private val _editing = MutableStateFlow<LLMConfigEntity?>(null)
    private val _form = MutableStateFlow(LLMConfigForm())
    private val _testResult = MutableStateFlow<String?>(null)
    private val _isTesting = MutableStateFlow(false)
    private val _isSaving = MutableStateFlow(false)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<LLMConfigUiState> = combine(
        listOf(
            repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()),
            _showForm, _editing, _form, _testResult, _isTesting, _isSaving
        )
    ) { values ->
        LLMConfigUiState(
            configs = values[0] as List<LLMConfigEntity>,
            showForm = values[1] as Boolean,
            editingConfig = values[2] as LLMConfigEntity?,
            form = values[3] as LLMConfigForm,
            testResult = values[4] as String?,
            isTesting = values[5] as Boolean,
            isSaving = values[6] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LLMConfigUiState())

    // ── 弹窗操作 ─────────────────────────────────

    fun openCreateForm() {
        _form.value = LLMConfigForm()
        _editing.value = null
        _showForm.value = true
        _testResult.value = null
    }

    fun openEditForm(config: LLMConfigEntity) {
        // 加载已保存的 API Key
        val key = repository.getApiKey(config.id) ?: ""
        _form.value = LLMConfigForm(
            name = config.name,
            apiUrl = config.apiUrl,
            modelName = config.modelName,
            apiKey = key,
            isDefault = config.isDefault
        )
        _editing.value = config
        _showForm.value = true
        _testResult.value = null
    }

    fun dismissForm() {
        _showForm.value = false
        _editing.value = null
        _testResult.value = null
    }

    // ── 表单字段更新 ───────────────────────────

    fun updateName(v: String) { _form.update { it.copy(name = v) } }
    fun updateUrl(v: String) { _form.update { it.copy(apiUrl = v) } }
    fun updateModel(v: String) { _form.update { it.copy(modelName = v) } }
    fun updateApiKey(v: String) { _form.update { it.copy(apiKey = v) } }
    fun updateDefault(v: Boolean) { _form.update { it.copy(isDefault = v) } }

    // ── 保存 ────────────────────────────────────

    fun save() {
        val f = _form.value
        if (!f.isValid) return
        viewModelScope.launch {
            _isSaving.value = true
            repository.save(
                id = _editing.value?.id,
                name = f.name,
                apiUrl = f.apiUrl,
                modelName = f.modelName,
                apiKey = f.apiKey,
                isDefault = f.isDefault
            )
            _isSaving.value = false
            dismissForm()
        }
    }

    // ── 删除 ────────────────────────────────────

    fun delete(config: LLMConfigEntity) {
        viewModelScope.launch { repository.delete(config) }
    }

    // ── 连接测试 ────────────────────────────────

    fun testConnection() {
        val f = _form.value
        if (!f.isValid) return
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL(f.apiUrl.trimEnd('/') + "/v1/models")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("Authorization", "Bearer ${f.apiKey}")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.connect()
                    val code = conn.responseCode
                    conn.disconnect()
                    if (code in 200..299) "✅ 连接成功 ($code)" else "❌ 失败: HTTP $code"
                }
                _testResult.value = result
            } catch (e: Exception) {
                _testResult.value = "❌ 无法连接: ${e.message}"
            } finally {
                _isTesting.value = false
            }
        }
    }
}
