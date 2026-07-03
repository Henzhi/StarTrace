package com.startrace.feature.story.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.FragmentRepository
import com.startrace.core.data.repository.LLMConfigRepository
import com.startrace.core.database.dao.StoryDao
import com.startrace.core.database.entity.FragmentEntity
import com.startrace.core.database.entity.StoryEntity
import com.startrace.core.network.StoryGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StoryGeneratorUiState(
    val fragments: List<FragmentEntity> = emptyList(),
    val selectedFragmentIds: Set<String> = emptySet(),
    val style: String = "scifi",
    val length: String = "medium",
    val isGenerating: Boolean = false,
    val result: StoryEntity? = null,
    val error: String? = null,
    val savedStoryId: String? = null
) {
    val hasConfig: Boolean get() = true // 将在 ViewModel 中动态判断
    val selectedCount: Int get() = selectedFragmentIds.size
    val canGenerate: Boolean get() = selectedFragmentIds.isNotEmpty() && !isGenerating
}

@HiltViewModel
class StoryGeneratorViewModel @Inject constructor(
    private val fragmentRepository: FragmentRepository,
    private val llmConfigRepository: LLMConfigRepository,
    private val storyDao: StoryDao,
    private val storyGenerator: StoryGenerator
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _style = MutableStateFlow("scifi")
    private val _length = MutableStateFlow("medium")
    private val _isGenerating = MutableStateFlow(false)
    private val _result = MutableStateFlow<StoryEntity?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _savedId = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<StoryGeneratorUiState> = combine(
        listOf(
            fragmentRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()),
            _selectedIds, _style, _length, _isGenerating, _result, _error, _savedId
        )
    ) { values ->
        StoryGeneratorUiState(
            fragments = values[0] as List<FragmentEntity>,
            selectedFragmentIds = values[1] as Set<String>,
            style = values[2] as String,
            length = values[3] as String,
            isGenerating = values[4] as Boolean,
            result = values[5] as StoryEntity?,
            error = values[6] as String?,
            savedStoryId = values[7] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoryGeneratorUiState())

    fun toggleFragment(id: String) {
        _selectedIds.update { if (id in it) it - id else it + id }
        _error.value = null
    }

    fun selectAll() {
        viewModelScope.launch {
            val allIds = fragmentRepository.observeAll().first().map { it.id }
            _selectedIds.value = allIds.toSet()
        }
    }

    fun clearSelection() { _selectedIds.value = emptySet() }

    fun setStyle(s: String) { _style.value = s }
    fun setLength(l: String) { _length.value = l }

    fun generate() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _result.value = null
            try {
                // 获取默认 LLM 配置
                val config = llmConfigRepository.getDefault()
                    ?: throw IllegalStateException("请先在「我的」页面配置 LLM")

                // 获取选中的碎片
                val selectedFragments = withContext(Dispatchers.IO) {
                    ids.mapNotNull { fragmentRepository.getById(it) }
                }

                if (selectedFragments.isEmpty()) throw IllegalStateException("未找到选中的碎片")

                // 调用 AI 生成
                val story = storyGenerator.generate(
                    config = config,
                    fragments = selectedFragments,
                    style = _style.value,
                    length = _length.value
                )
                _result.value = story
            } catch (e: Exception) {
                _error.value = e.message ?: "生成失败"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun saveStory() {
        val story = _result.value ?: return
        viewModelScope.launch {
            storyDao.upsert(story)
            _savedId.value = story.id
        }
    }

    fun reset() {
        _result.value = null
        _error.value = null
        _savedId.value = null
    }
}
