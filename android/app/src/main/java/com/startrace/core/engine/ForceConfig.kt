package com.startrace.core.engine

/**
 * 力导向引擎配置参数。
 *
 * 所有数值均为物理模拟调参，通过调整这些值可控制布局的疏密、聚合程度和收敛速度。
 *
 * @property repulsionStrength 全局库伦斥力强度（越大节点越分散）
 * @property attractionStrength 同标签引力基础强度
 * @property domainTagBonus 同 domainTag 的额外引力加成（叠加在 attractionStrength 上）
 * @property formTagBonus 同 formTag 的弱引力加成
 * @property storyBondStrength 同 storyId 的碎片-故事锚定引力强度
 * @property centeringForce 向画布中心的回拉力
 * @property canvasRadius 画布半径（节点位置会被限制在此范围内）
 * @property minDistance 最小距离（防止除零导致力无穷大）
 * @property maxVelocity 单步最大速度（防止节点飞出画布）
 * @property initialDamping 初始阻尼（0-1，越小移动越快）
 * @property finalDamping 最终阻尼（0-1，越小移动越快）
 * @property convergenceThreshold 收敛阈值：当所有节点平均速度低于此值时提前终止
 */
data class ForceConfig(
    val repulsionStrength: Float = 5000f,
    val attractionStrength: Float = 0.01f,
    val domainTagBonus: Float = 0.015f,
    val formTagBonus: Float = 0.01f,
    val storyBondStrength: Float = 0.05f,
    val centeringForce: Float = 0.001f,
    val canvasRadius: Float = 1000f,
    val minDistance: Float = 1f,
    val maxVelocity: Float = 50f,
    val initialDamping: Float = 0.60f,
    val finalDamping: Float = 0.92f,
    val convergenceThreshold: Float = 0.5f
)
