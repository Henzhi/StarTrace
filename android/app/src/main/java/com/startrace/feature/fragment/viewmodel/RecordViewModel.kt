package com.startrace.feature.fragment.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.FragmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 碎片记录页 UI 状态
 */
data class RecordUiState(
    val content: String = "",
    val domainTag: String = "",
    val formTag: String? = null,
    val mood: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    /** content 非空 + domainTag 已选 → 允许保存 */
    val canSave: Boolean
        get() = content.isNotBlank() && domainTag.isNotEmpty() && !isSaving
}

/**
 * 碎片记录页 ViewModel
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val fragmentRepository: FragmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    /** 更新文本内容 */
    fun updateContent(text: String) {
        _uiState.update { it.copy(content = text, saveSuccess = false, error = null) }
    }

    /** 选择领域标签（必选） */
    fun selectDomainTag(tag: String) {
        _uiState.update { it.copy(domainTag = tag) }
    }

    /** 选择内容形态标签（可选） */
    fun selectFormTag(tag: String?) {
        _uiState.update { it.copy(formTag = tag) }
    }

    /** 选择情绪 */
    fun selectMood(mood: String?) {
        _uiState.update { it.copy(mood = mood) }
    }

    /** 保存碎片 */
    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            runCatching {
                fragmentRepository.create(
                    content = state.content.trim(),
                    domainTag = state.domainTag,
                    formTag = state.formTag,
                    mood = state.mood
                )
            }.onSuccess {
                _uiState.update {
                    RecordUiState(saveSuccess = true)  // 重置表单
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "保存失败")
                }
            }
        }
    }

    /** 清除成功状态（动画回调后调用） */
    fun clearSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
