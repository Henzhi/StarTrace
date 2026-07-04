package com.startrace.feature.story.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.database.dao.StoryDao
import com.startrace.core.database.entity.StoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryListUiState(
    val stories: List<StoryEntity> = emptyList(),
    val selectedStyles: Set<String> = emptySet(),
    val isLoading: Boolean = true
) {
    val isFilterActive: Boolean
        get() = selectedStyles.isNotEmpty()
}

/** 故事风格标签定义 */
data class StoryStyleTag(val key: String, val emoji: String, val label: String)

val STORY_STYLE_TAGS = listOf(
    StoryStyleTag("scifi", "🚀", "科幻"),
    StoryStyleTag("fantasy", "🧙", "奇幻"),
    StoryStyleTag("realistic", "📷", "写实"),
    StoryStyleTag("prose", "🌸", "散文"),
    StoryStyleTag("poetry", "🎵", "诗歌"),
    StoryStyleTag("mystery", "🔍", "悬疑")
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StoryListViewModel @Inject constructor(
    private val storyDao: StoryDao
) : ViewModel() {

    private val _selectedStyles = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)

    private val _filteredStories: StateFlow<List<StoryEntity>> = combine(
        storyDao.observeAll(),
        _selectedStyles
    ) { allStories, styles ->
        _isLoading.value = false
        if (styles.isEmpty()) allStories
        else allStories.filter { it.style in styles }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<StoryListUiState> = combine(
        _filteredStories,
        _selectedStyles,
        _isLoading
    ) { stories, styles, loading ->
        StoryListUiState(
            stories = stories,
            selectedStyles = styles,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoryListUiState())

    // ═══════════════════════════════════════════════════════
    // 风格筛选
    // ═══════════════════════════════════════════════════════

    fun toggleStyleFilter(style: String) {
        _selectedStyles.update { current ->
            if (style in current) current - style else current + style
        }
    }

    fun clearStyleFilters() {
        _selectedStyles.value = emptySet()
    }

    // ═══════════════════════════════════════════════════════
    // 单条操作
    // ═══════════════════════════════════════════════════════

    private val _snackbarEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    /** 删除故事 */
    fun deleteStory(story: StoryEntity) {
        viewModelScope.launch {
            storyDao.delete(story)
            _snackbarEvent.emit("已删除故事「${story.title}」")
        }
    }
}
