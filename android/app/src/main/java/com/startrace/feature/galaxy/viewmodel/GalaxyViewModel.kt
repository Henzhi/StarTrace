package com.startrace.feature.galaxy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.FragmentRepository
import com.startrace.core.data.repository.LocalUserRepository
import com.startrace.core.database.dao.StoryDao
import com.startrace.core.database.dao.StoryFragmentRefDao
import com.startrace.core.database.entity.FragmentEntity
import com.startrace.core.database.entity.StoryEntity
import com.startrace.core.database.entity.StoryFragmentRef
import com.startrace.core.engine.ForceConfig
import com.startrace.core.engine.ForceDirectedEngine
import com.startrace.core.engine.GraphNode
import com.startrace.core.engine.NodeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 星系 UI 状态 */
data class GalaxyUiState(
    val nodes: List<GraphNode> = emptyList(),
    val selectedNodeId: String? = null,
    val draggingNodeId: String? = null,
    val highlightedTag: String? = null,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val zoom: Float = 1.2f,
    val isSimulating: Boolean = true,
    val fragmentMap: Map<String, FragmentEntity> = emptyMap(),  // id → 碎片完整数据
    val storyMap: Map<String, StoryEntity> = emptyMap()         // id → 故事完整数据
) {
    val selectedNode: GraphNode? get() = nodes.find { it.id == selectedNodeId }
    val selectedFragment: FragmentEntity? get() = selectedNodeId?.let { fragmentMap[it] }
    /** 选中节点关联的故事列表 */
    val linkedStories: List<StoryEntity> get() {
        val node = selectedNode ?: return emptyList()
        return node.storyIds.mapNotNull { storyMap[it] }
    }
}

/**
 * 星系页 ViewModel — Room 碎片 → ForceDirectedEngine → 节点位置
 *
 * 观察 Room 碎片变化，自动转换为 GraphNode 并运行力导向模拟，
 * 输出节点位置供 Canvas 渲染。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class GalaxyViewModel @Inject constructor(
    private val fragmentRepository: FragmentRepository,
    private val storyDao: StoryDao,
    private val storyFragmentRefDao: StoryFragmentRefDao,
    private val userRepository: LocalUserRepository
) : ViewModel() {

    private val engine = ForceDirectedEngine(
        ForceConfig(canvasRadius = 1000f, convergenceThreshold = 0.3f)
    )

    // ── 内部状态 ─────────────────────────────────────
    private val _nodes = MutableStateFlow<List<GraphNode>>(emptyList())
    private val _selectedNodeId = MutableStateFlow<String?>(null)
    private val _highlightedTag = MutableStateFlow<String?>(null)
    private val _offsetX = MutableStateFlow(0f)
    private val _offsetY = MutableStateFlow(0f)
    private val _zoom = MutableStateFlow(1.2f)
    private val _isSimulating = MutableStateFlow(true)
    private val _fragmentMap = MutableStateFlow<Map<String, FragmentEntity>>(emptyMap())
    private val _storyMap = MutableStateFlow<Map<String, StoryEntity>>(emptyMap())
    private val _draggingNodeId = MutableStateFlow<String?>(null)

    // ── 单向数据流 ───────────────────────────────────
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<GalaxyUiState> = combine(
        listOf(_nodes, _selectedNodeId, _draggingNodeId, _highlightedTag, _offsetX, _offsetY, _zoom, _isSimulating, _fragmentMap, _storyMap)
    ) { values ->
        GalaxyUiState(
            nodes = values[0] as List<GraphNode>,
            selectedNodeId = values[1] as String?,
            draggingNodeId = values[2] as String?,
            highlightedTag = values[3] as String?,
            offsetX = values[4] as Float,
            offsetY = values[5] as Float,
            zoom = values[6] as Float,
            isSimulating = values[7] as Boolean,
            fragmentMap = values[8] as Map<String, FragmentEntity>,
            storyMap = values[9] as Map<String, StoryEntity>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalaxyUiState())

    // ── 初始化 ───────────────────────────────────────
    init {
        viewModelScope.launch {
            combine(
                fragmentRepository.observeAll(),
                userRepository.userIdFlow
            ) { fragments, userId -> fragments to userId }
                .flatMapLatest { (fragments, uid) ->
                    storyDao.observeAll(uid).map { stories -> fragments to stories }
                }
                .collect { (fragments, stories) ->
                    runSimulation(fragments, stories)
                }
        }
    }

    // ═══════════════════════════════════════════════════
    private suspend fun runSimulation(fragments: List<FragmentEntity>, stories: List<StoryEntity>) {
        _isSimulating.value = true
        _fragmentMap.value = fragments.associateBy { it.id }
        _storyMap.value = stories.associateBy { it.id }
        withContext(Dispatchers.Default) {
            engine.clear()

            // 加载多对多关联
            val allRefs = storyFragmentRefDao.getByFragmentIds(fragments.map { it.id })
            // fragmentId → set of storyIds
            val fragToStoryIds = fragments.associate { f ->
                f.id to allRefs.filter { it.fragmentId == f.id }.map { it.storyId }.toSet()
            }

            // FragmentEntity → GraphNode（带 storyIds）
            val graphNodes = mutableListOf<GraphNode>()
            fragments.forEach { f ->
                graphNodes.add(GraphNode(
                    id = f.id, label = f.content, mass = 1f, isFixed = false,
                    domainTag = f.domainTag, formTag = f.formTag,
                    storyIds = fragToStoryIds[f.id] ?: emptySet(),
                    nodeType = NodeType.FRAGMENT, x = f.positionX, y = f.positionY
                ))
            }

            // StoryEntity → 固定锚点（storyIds 只包含自身 id，用于使碎片锚定到它）
            stories.forEach { s ->
                graphNodes.add(GraphNode(
                    id = s.id, label = s.title, mass = 2f, isFixed = true,
                    domainTag = "", formTag = null,
                    storyIds = setOf(s.id),
                    nodeType = NodeType.STORY, x = s.positionX, y = s.positionY
                ))
            }

            if (graphNodes.isNotEmpty()) {
                engine.addNodes(graphNodes)
                engine.simulate(maxIterations = minOf(100, graphNodes.size * 5))
                _nodes.value = engine.snapshot()
            } else {
                _nodes.value = emptyList()
            }
        }
        _isSimulating.value = false
    }

    // ═══════════════════════════════════════════════════
    // 交互
    // ═══════════════════════════════════════════════════

    fun selectNode(id: String?) { _selectedNodeId.value = id }
    fun deselectNode() { _selectedNodeId.value = null }

    fun highlightTag(tag: String?) { _highlightedTag.value = tag }

    fun updateViewport(dx: Float, dy: Float) {
        _offsetX.update { it + dx }
        _offsetY.update { it + dy }
    }

    fun updateZoom(factor: Float, centroidX: Float, centroidY: Float) {
        val newZoom = (_zoom.value * factor).coerceIn(0.3f, 5f)
        // 缩放以点击点为中心
        _offsetX.update { centroidX - (centroidX - it) * newZoom / _zoom.value }
        _offsetY.update { centroidY - (centroidY - it) * newZoom / _zoom.value }
        _zoom.value = newZoom
    }

    // ── 节点拖拽 ───────────────────────────────────

    /** 开始拖拽节点 */
    fun startDragging(nodeId: String) {
        _draggingNodeId.value = nodeId
        _selectedNodeId.value = nodeId
    }

    /** 拖拽移动节点（delta 为引擎坐标系偏移量） */
    fun dragNode(dx: Float, dy: Float) {
        val id = _draggingNodeId.value ?: return
        _nodes.update { nodes ->
            nodes.map { node ->
                if (node.id == id) node.copy(x = node.x + dx, y = node.y + dy) else node
            }
        }
        // 同步更新引擎内部位置
        engine.moveNode(id, dx, dy)
    }

    /** 结束拖拽 */
    fun endDragging() {
        _draggingNodeId.value = null
    }
}
