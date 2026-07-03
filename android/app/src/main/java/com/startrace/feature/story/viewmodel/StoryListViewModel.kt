package com.startrace.feature.story.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.database.dao.StoryDao
import com.startrace.core.database.entity.StoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class StoryListUiState(
    val stories: List<StoryEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StoryListViewModel @Inject constructor(
    private val storyDao: StoryDao
) : ViewModel() {

    val uiState: StateFlow<StoryListUiState> = storyDao.observeAll()
        .map { StoryListUiState(stories = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoryListUiState())
}
