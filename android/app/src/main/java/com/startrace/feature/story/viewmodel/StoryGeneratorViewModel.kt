package com.startrace.feature.story.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.FragmentRepository
import com.startrace.core.data.repository.LLMConfigRepository
import com.startrace.core.database.dao.StoryDao
import com.startrace.core.database.dao.StoryFragmentRefDao
import com.startrace.core.database.entity.FragmentEntity
import com.startrace.core.database.entity.StoryEntity
import com.startrace.core.database.entity.StoryFragmentRef
import com.startrace.core.network.StoryGenerator
import com.startrace.core.network.TokenEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject

data class StoryGeneratorUiState(
    val fragments: List<FragmentEntity> = emptyList(),
    val selectedFragmentIds: Set<String> = emptySet(),
    val style: String = "scifi",
    val length: String = "medium",
    val isGenerating: Boolean = false,
    val streamingTokens: String = "",       // 流式输出累积
    val result: StoryEntity? = null,
    val error: String? = null,
    val savedStoryId: String? = null
) {
    val selectedCount: Int get() = selectedFragmentIds.size
    val canGenerate: Boolean get() = selectedFragmentIds.isNotEmpty() && !isGenerating
    val isStreaming: Boolean get() = isGenerating && result == null
}

@HiltViewModel
class StoryGeneratorViewModel @Inject constructor(
    private val fragmentRepository: FragmentRepository,
    private val llmConfigRepository: LLMConfigRepository,
    private val storyDao: StoryDao,
    private val storyFragmentRefDao: StoryFragmentRefDao,
    private val storyGenerator: StoryGenerator
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _style = MutableStateFlow("scifi")
    private val _length = MutableStateFlow("medium")
    private val _isGenerating = MutableStateFlow(false)
    private val _streamingTokens = MutableStateFlow("")
    private val _result = MutableStateFlow<StoryEntity?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _savedId = MutableStateFlow<String?>(null)
    private var _selectedFragmentIdsForSave: Set<String> = emptySet()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<StoryGeneratorUiState> = combine(
        listOf(
            fragmentRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()),
            _selectedIds, _style, _length, _isGenerating, _streamingTokens, _result, _error, _savedId
        )
    ) { values ->
        StoryGeneratorUiState(
            fragments = values[0] as List<FragmentEntity>,
            selectedFragmentIds = values[1] as Set<String>,
            style = values[2] as String,
            length = values[3] as String,
            isGenerating = values[4] as Boolean,
            streamingTokens = values[5] as String,
            result = values[6] as StoryEntity?,
            error = values[7] as String?,
            savedStoryId = values[8] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoryGeneratorUiState())

    fun toggleFragment(id: String) { _selectedIds.update { if (id in it) it - id else it + id }; _error.value = null }
    fun selectAll() { viewModelScope.launch { _selectedIds.value = fragmentRepository.observeAll().first().map { it.id }.toSet() } }
    fun clearSelection() { _selectedIds.value = emptySet() }
    fun setStyle(s: String) { _style.value = s }
    fun setLength(l: String) { _length.value = l }

    /** 流式生成故事 */
    fun generate() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _result.value = null
            _streamingTokens.value = ""

            try {
                val config = llmConfigRepository.getDefault()
                    ?: throw IllegalStateException("请先在「我的」页面配置 LLM")

                val selectedFragments = withContext(Dispatchers.IO) {
                    ids.mapNotNull { fragmentRepository.getById(it) }
                }
                if (selectedFragments.isEmpty()) throw IllegalStateException("未找到选中的碎片")

                val fullContent = StringBuilder()
                val selectedFragmentIds = selectedFragments.map { it.id }

                storyGenerator.generateStream(
                    config = config, fragments = selectedFragments,
                    style = _style.value, length = _length.value
                ).collect { event ->
                    when (event) {
                        is TokenEvent.Token -> {
                            fullContent.append(event.text)
                            _streamingTokens.value = fullContent.toString()
                        }
                        is TokenEvent.Complete -> {
                            val content = fullContent.toString().trim()
                            if (content.isNotBlank()) {
                                _result.value = StoryEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = content.lines().firstOrNull { it.isNotBlank() }?.trim()?.take(50) ?: "未命名故事",
                                    content = content,
                                    fragmentIdsJson = JSONArray(selectedFragmentIds).toString(),
                                    length = _length.value,
                                    style = _style.value,
                                    positionX = (Math.random() * 400 - 200).toFloat(),
                                    positionY = (Math.random() * 400 - 200).toFloat(),
                                    llmConfigId = config.id,
                                    createdAt = System.currentTimeMillis()
                                )
                                // 暂存选中的碎片 ID，保存时写入 junction table
                                _selectedFragmentIdsForSave = selectedFragmentIds.toSet()
                            }
                            _isGenerating.value = false
                        }
                        is TokenEvent.Error -> {
                            _error.value = event.message
                            _isGenerating.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "生成失败"
                _isGenerating.value = false
            }
        }
    }

    fun saveStory() {
        val story = _result.value ?: return
        viewModelScope.launch {
            storyDao.upsert(story)
            // 写入多对多关联表
            val refs = _selectedFragmentIdsForSave.map { fid -> StoryFragmentRef(storyId = story.id, fragmentId = fid) }
            storyFragmentRefDao.insertAll(refs)
            _savedId.value = story.id
        }
    }

    fun reset() { _result.value = null; _error.value = null; _savedId.value = null; _streamingTokens.value = "" }
}
