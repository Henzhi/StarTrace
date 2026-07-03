package com.startrace.feature.fragment.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.FragmentRepository
import com.startrace.core.database.entity.FragmentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 碎片列表 UI 状态
 */
data class FragmentListUiState(
    val fragments: List<FragmentEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedDomains: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val resultCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true
) {
    /** 当前搜索/筛选是否激活 */
    val isFilterActive: Boolean
        get() = searchQuery.isNotBlank() || selectedDomains.isNotEmpty()
}

/**
 * 碎片列表 ViewModel — 管理碎片集合的展示、搜索、筛选、批量操作
 */
@HiltViewModel
class FragmentListViewModel @Inject constructor(
    private val repository: FragmentRepository
) : ViewModel() {

    // ── 内部状态 ──────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    private val _selectedDomains = MutableStateFlow<Set<String>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)

    // ── 待撤销删除 (Snackbar undo) ────────────────────────

    private var pendingDeleteFragment: FragmentEntity? = null

    // ── 派生：过滤后的碎片列表 ────────────────────────────

    private val _filteredFragments: StateFlow<List<FragmentEntity>> = combine(
        repository.observeAll(),
        _searchQuery.debounce(300),
        _selectedDomains
    ) { allFragments, query, domains ->
        _isLoading.value = false
        var result = allFragments

        if (query.isNotBlank()) {
            result = result.filter { it.content.contains(query, ignoreCase = true) }
        }
        if (domains.isNotEmpty()) {
            result = result.filter { it.domainTag in domains }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── 派生：总碎片数 ────────────────────────────────────

    private val _totalCount: StateFlow<Int> = repository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── 对外暴露的 UI 状态 ─────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<FragmentListUiState> = combine(
        listOf(
            _filteredFragments,
            _searchQuery,
            _selectedDomains,
            _isSelectionMode,
            _selectedIds,
            _totalCount,
            _isLoading
        )
    ) { values: Array<Any?> ->
        val fragments = values[0] as List<FragmentEntity>
        FragmentListUiState(
            fragments = fragments,
            searchQuery = values[1] as String,
            selectedDomains = values[2] as Set<String>,
            isSelectionMode = values[3] as Boolean,
            selectedIds = values[4] as Set<String>,
            resultCount = fragments.size,
            totalCount = values[5] as Int,
            isLoading = values[6] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FragmentListUiState())

    // ── 一次性事件（Snackbar） ─────────────────────────────

    private val _snackbarEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // ═══════════════════════════════════════════════════════
    // 搜索 & 筛选
    // ═══════════════════════════════════════════════════════

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun toggleDomainFilter(domain: String) {
        _selectedDomains.update { current ->
            if (domain in current) current - domain else current + domain
        }
    }

    fun clearDomainFilters() {
        _selectedDomains.value = emptySet()
    }

    // ═══════════════════════════════════════════════════════
    // 单条操作
    // ═══════════════════════════════════════════════════════

    /** 滑动删除 — 先删，Snackbar 可撤销 */
    fun swipeDelete(fragment: FragmentEntity) {
        viewModelScope.launch {
            pendingDeleteFragment = fragment
            repository.delete(fragment)
            _snackbarEvent.emit("已删除")
        }
    }

    /** 撤销上一次删除 */
    fun undoDelete() {
        viewModelScope.launch {
            pendingDeleteFragment?.let { repository.update(it) }
            pendingDeleteFragment = null
            _snackbarEvent.emit("已撤销删除")
        }
    }

    /** 归档单条碎片 */
    fun archiveFragment(id: String) {
        viewModelScope.launch {
            repository.archive(id)
            _snackbarEvent.emit("已归档")
        }
    }

    // ═══════════════════════════════════════════════════════
    // 批量选择
    // ═══════════════════════════════════════════════════════

    /** 长按进入多选模式 */
    fun enterSelectionMode(firstId: String) {
        _isSelectionMode.value = true
        _selectedIds.update { it + firstId }
    }

    /** 切换某条碎片的选中状态 */
    fun toggleSelection(id: String) {
        _selectedIds.update { current ->
            val updated = if (id in current) current - id else current + id
            if (updated.isEmpty()) _isSelectionMode.value = false
            updated
        }
    }

    /** 退出多选模式 */
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    /** 批量删除 */
    fun batchDelete() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            if (ids.isEmpty()) return@launch
            repository.deleteByIds(ids)
            _snackbarEvent.emit("已删除 ${ids.size} 条碎片")
            exitSelectionMode()
        }
    }

    /** 批量归档 */
    fun batchArchive() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            if (ids.isEmpty()) return@launch
            repository.archiveByIds(ids)
            _snackbarEvent.emit("已归档 ${ids.size} 条碎片")
            exitSelectionMode()
        }
    }
}
