package com.startrace.feature.galaxy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrace.core.data.repository.FragmentRepository
import com.startrace.core.database.entity.FragmentEntity
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
    val highlightedTag: String? = null,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val zoom: Float = 1.2f,
    val isSimulating: Boolean = true,
    val fragmentMap: Map<String, FragmentEntity> = emptyMap()  // id → 碎片完整数据
) {
    val selectedNode: GraphNode? get() = nodes.find { it.id == selectedNodeId }
    val selectedFragment: FragmentEntity? get() = selectedNodeId?.let { fragmentMap[it] }
}

/**
 * 星系页 ViewModel — Room 碎片 → ForceDirectedEngine → 节点位置
 *
 * 观察 Room 碎片变化，自动转换为 GraphNode 并运行力导向模拟，
 * 输出节点位置供 Canvas 渲染。
 */
@HiltViewModel
class GalaxyViewModel @Inject constructor(
    private val fragmentRepository: FragmentRepository
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

    // ── 单向数据流 ───────────────────────────────────
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<GalaxyUiState> = combine(
        listOf(_nodes, _selectedNodeId, _highlightedTag, _offsetX, _offsetY, _zoom, _isSimulating, _fragmentMap)
    ) { values ->
        GalaxyUiState(
            nodes = values[0] as List<GraphNode>,
            selectedNodeId = values[1] as String?,
            highlightedTag = values[2] as String?,
            offsetX = values[3] as Float,
            offsetY = values[4] as Float,
            zoom = values[5] as Float,
            isSimulating = values[6] as Boolean,
            fragmentMap = values[7] as Map<String, FragmentEntity>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalaxyUiState())

    // ── 初始化 ───────────────────────────────────────
    init {
        viewModelScope.launch {
            fragmentRepository.observeAll().collect { fragments ->
                runSimulation(fragments)
            }
        }
    }

    // ═══════════════════════════════════════════════════
    private suspend fun runSimulation(fragments: List<FragmentEntity>) {
        _isSimulating.value = true
        _fragmentMap.value = fragments.associateBy { it.id }
        withContext(Dispatchers.Default) {
            engine.clear()

            // FragmentEntity → GraphNode
            val graphNodes = fragments.map { f ->
                GraphNode(
                    id = f.id,
                    label = f.content,
                    mass = 1f,
                    isFixed = false,
                    domainTag = f.domainTag,
                    formTag = f.formTag,
                    storyId = null,  // 暂无故事关联
                    nodeType = NodeType.FRAGMENT,
                    x = f.positionX,
                    y = f.positionY
                )
            }

            if (graphNodes.isNotEmpty()) {
                engine.addNodes(graphNodes)
                engine.simulate(maxIterations = minOf(80, fragments.size * 10))
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
}
