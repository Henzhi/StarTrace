package com.startrace.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.FragmentRepository
import com.startrace.core.data.repository.LocalUserRepository
import com.startrace.core.data.repository.LLMConfigRepository
import com.startrace.core.database.dao.StoryDao
import com.startrace.core.database.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserEntity? = null,
    val username: String = "",
    val fragmentCount: Int = 0,
    val storyCount: Int = 0,
    val configCount: Int = 0,
    val isEditingName: Boolean = false,
    val editNameValue: String = "",
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: LocalUserRepository,
    private val fragmentRepository: FragmentRepository,
    private val storyDao: StoryDao,
    private val llmConfigRepository: LLMConfigRepository
) : ViewModel() {

    private val _isEditingName = MutableStateFlow(false)
    private val _editNameValue = MutableStateFlow("")
    private val _error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val storyCountFlow = userRepository.userIdFlow.flatMapLatest { userId ->
        storyDao.observeCount(userId)
    }

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.userFlow,
        fragmentRepository.observeCount(),
        storyCountFlow,
        llmConfigRepository.observeAll(),
        _isEditingName,
        _editNameValue,
        _error
    ) { values ->
        val user = values[0] as UserEntity?
        ProfileUiState(
            user = user,
            username = user?.username ?: "",
            fragmentCount = values[1] as Int,
            storyCount = values[2] as Int,
            configCount = (values[3] as List<*>).size,
            isEditingName = values[4] as Boolean,
            editNameValue = values[5] as String,
            error = values[6] as String?
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), ProfileUiState())

    init {
        viewModelScope.launch {
            userRepository.ensureLocalUser()
        }
    }

    fun startEditName() {
        viewModelScope.launch {
            _editNameValue.value = userRepository.getUsername()
            _isEditingName.value = true
        }
    }

    fun cancelEditName() {
        _isEditingName.value = false
        _editNameValue.value = ""
        _error.value = null
    }

    fun updateEditName(value: String) {
        _editNameValue.value = value
    }

    fun saveUsername() {
        val newName = _editNameValue.value.trim()
        if (newName.isBlank()) {
            _error.value = "用户名不能为空"
            return
        }
        if (newName.length > 20) {
            _error.value = "用户名不能超过20个字符"
            return
        }

        viewModelScope.launch {
            userRepository.updateUsername(newName)
            _isEditingName.value = false
            _editNameValue.value = ""
            _error.value = null
        }
    }

    fun clearError() {
        _error.value = null
    }
}