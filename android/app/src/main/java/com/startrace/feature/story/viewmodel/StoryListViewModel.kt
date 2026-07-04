package com.startrace.feature.story.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.LocalUserRepository
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
    private val storyDao: StoryDao,
    private val userRepository: LocalUserRepository
) : ViewModel() {

    private val _selectedStyles = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)

    private val _filteredStories: StateFlow<List<StoryEntity>> = combine(
        userRepository.userIdFlow,
        _selectedStyles
    ) { uid, styles ->
        Pair(uid, styles)
    }.flatMapLatest { (uid, styles) ->
        storyDao.observeAll(uid).map { stories ->
            _isLoading.value = false
            if (styles.isEmpty()) stories
            else stories.filter { it.style in styles }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<StoryListUiState> = combine(
        _filteredStories,
        _selectedStyles,
        _isLoading
    ) { stories, styles, loading ->
        StoryListUiState(stories = stories, selectedStyles = styles, isLoading = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoryListUiState())

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    fun toggleStyleFilter(tag: String) {
        _selectedStyles.update { current ->
            if (tag in current) current - tag else current + tag
        }
    }

    fun clearStyleFilters() {
        _selectedStyles.value = emptySet()
    }

    fun deleteStory(story: StoryEntity) {
        viewModelScope.launch {
            storyDao.delete(story)
            _snackbarEvent.emit("故事已删除")
        }
    }
}