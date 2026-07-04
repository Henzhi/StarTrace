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
import com.startrace.core.network.TokenManager
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
    val useCustomLength: Boolean = false,
    val customLengthText: String = "",
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
    private val storyGenerator: StoryGenerator,
    private val userRepository: com.startrace.core.data.repository.LocalUserRepository
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _style = MutableStateFlow("scifi")
    private val _length = MutableStateFlow("medium")
    private val _useCustomLength = MutableStateFlow(false)
    private val _customLengthText = MutableStateFlow("")
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
            _selectedIds, _style, _length, _useCustomLength, _customLengthText,
            _isGenerating, _streamingTokens, _result, _error, _savedId
        )
    ) { values ->
        StoryGeneratorUiState(
            fragments = values[0] as List<FragmentEntity>,
            selectedFragmentIds = values[1] as Set<String>,
            style = values[2] as String,
            length = values[3] as String,
            useCustomLength = values[4] as Boolean,
            customLengthText = values[5] as String,
            isGenerating = values[6] as Boolean,
            streamingTokens = values[7] as String,
            result = values[8] as StoryEntity?,
            error = values[9] as String?,
            savedStoryId = values[10] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoryGeneratorUiState())

    fun toggleFragment(id: String) { _selectedIds.update { if (id in it) it - id else it + id }; _error.value = null }
    fun selectAll() { viewModelScope.launch { _selectedIds.value = fragmentRepository.observeAll().first().map { it.id }.toSet() } }
    fun clearSelection() { _selectedIds.value = emptySet() }
    fun setStyle(s: String) { _style.value = s }
    fun setLength(l: String) { _length.value = l; if (l != "custom") _useCustomLength.value = false }
    fun setCustomLength(text: String) { _customLengthText.value = text; _useCustomLength.value = true; _length.value = "custom" }

    /** 获取实际传递给 LLM 的长度值：预设的字数范围或用户自定义字数 */
    private fun effectiveLength(): String {
        return if (_useCustomLength.value) _customLengthText.value.trim().ifEmpty { "1000 字" }
        else _length.value
    }

    /** 获取当前用户 ID */
    private suspend fun currentUserId(): String = userRepository.getUserId()

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
                    style = _style.value, length = effectiveLength()
                ).collect { event ->
                    when (event) {
                        is TokenEvent.Token -> {
                            fullContent.append(event.text)
                            _streamingTokens.value = fullContent.toString()
                        }
                        is TokenEvent.Complete -> {
                            // 去掉末尾 LLM 在 stop 信号前残留的逗号和引号 artifact
                            val raw = fullContent.toString()
                                .trim()
                                .trimEnd('，', ',', '"', '\'')
                            if (raw.isNotBlank()) {
                                // 提取 # 标题行（第一行），正文中剔除标题行
                                val lines = raw.lines()
                                val firstNonBlank = lines.firstOrNull { it.isNotBlank() }
                                val (title, body) = if (firstNonBlank != null && firstNonBlank.startsWith("# ")) {
                                    firstNonBlank.removePrefix("# ").trim().take(50) to
                                        lines.drop(1).joinToString("\n").trim()
                                } else {
                                    (firstNonBlank?.trim()?.take(50) ?: "未命名故事") to raw
                                }
                                _result.value = StoryEntity(
                                    id = UUID.randomUUID().toString(),
                                    userId = currentUserId(),
                                    title = title,
                                    content = body,
                                    fragmentIdsJson = JSONArray(selectedFragmentIds).toString(),
                                    length = _length.value,
                                    style = _style.value,
                                    positionX = (Math.random() * 400 - 200).toFloat(),
                                    positionY = (Math.random() * 400 - 200).toFloat(),
                                    llmConfigId = config.id,
                                    createdAt = System.currentTimeMillis()
                                )
                                _selectedFragmentIdsForSave = selectedFragmentIds.toSet()
                            }
                            _isGenerating.value = false
                        }
                        is TokenEvent.Truncated -> {
                            // 后端因 token 限制强制截断，仍保存已有内容但给出警告
                            val raw = fullContent.toString()
                                .trim()
                                .trimEnd('，', ',', '"', '\'')
                            if (raw.isNotBlank()) {
                                val lines = raw.lines()
                                val firstNonBlank = lines.firstOrNull { it.isNotBlank() }
                                val (title, body) = if (firstNonBlank != null && firstNonBlank.startsWith("# ")) {
                                    firstNonBlank.removePrefix("# ").trim().take(50) to
                                        lines.drop(1).joinToString("\n").trim()
                                } else {
                                    ((firstNonBlank?.trim()?.take(50) ?: "未命名故事") + "（截断）") to raw
                                }
                                _result.value = StoryEntity(
                                    id = UUID.randomUUID().toString(),
                                    userId = currentUserId(),
                                    title = title,
                                    content = body,
                                    fragmentIdsJson = JSONArray(selectedFragmentIds).toString(),
                                    length = _length.value,
                                    style = _style.value,
                                    positionX = (Math.random() * 400 - 200).toFloat(),
                                    positionY = (Math.random() * 400 - 200).toFloat(),
                                    llmConfigId = config.id,
                                    createdAt = System.currentTimeMillis()
                                )
                                _selectedFragmentIdsForSave = selectedFragmentIds.toSet()
                            }
                            _error.value = "故事被后端 token 限制截断，请尝试更换 LLM 或减少碎片数量"
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

    fun reset() { _result.value = null; _error.value = null; _savedId.value = null; _streamingTokens.value = ""; _useCustomLength.value = false; _customLengthText.value = ""; _length.value = "medium" }
}
