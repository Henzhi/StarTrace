package com.startrace.core.engine

/**
 * 力导向图中的节点。
 *
 * 代表星图中的碎片或故事节点，携带标签信息用于相似度加权。
 *
 * @property id 唯一标识（对应 FragmentEntity.id / StoryEntity.id）
 * @property label 显示名称
 * @property mass 质量（影响受力大小，默认 1.0）
 * @property isFixed 是否为固定节点（故事节点 true，碎片节点 false）
 * @property domainTag 领域标签（世界/人物/情节/对话/设定/哲思）
 * @property formTag 形态标签（场景/角色/台词/概念/冲突/转折）
 * @property storyId 关联的故事 ID（碎片归属时非空）
 * @property nodeType 节点类型（fragment / story）
 * @property x 当前 X 坐标
 * @property y 当前 Y 坐标
 * @property vx 当前 X 方向速度
 * @property vy 当前 Y 方向速度
 */
data class GraphNode(
    val id: String,
    val label: String = "",
    val mass: Float = 1f,
    val isFixed: Boolean = false,
    val domainTag: String = "",
    val formTag: String? = null,
    val storyId: String? = null,
    val nodeType: NodeType = NodeType.FRAGMENT,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f
)

/** 节点类型 */
enum class NodeType { FRAGMENT, STORY }
