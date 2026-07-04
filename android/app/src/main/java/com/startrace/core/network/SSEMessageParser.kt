package com.startrace.core.network

/**
 * SSE 流式解析事件
 */
sealed class TokenEvent {
    /** 新 token 到达 */
    data class Token(val text: String) : TokenEvent()

    /** 流式输出正常完成 */
    data object Complete : TokenEvent()

    /** 流式输出完成，但被后端因 token 限制截断 (finish_reason=length) */
    data object Truncated : TokenEvent()

    /** 解析或网络错误 */
    data class Error(val message: String) : TokenEvent()
}

/**
 * SSE 消息解析器 — 解析 OpenAI 兼容的 Server-Sent Events 流
 *
 * 格式：
 * ```
 * data: {"choices":[{"delta":{"content":"Hello"}}]}
 * data: {"choices":[{"delta":{"content":" world"}}]}
 * data: [DONE]
 * ```
 *
 * 纯 Kotlin，无 Android 依赖，可独立单元测试。
 */
object SSEMessageParser {

    /**
     * 解析单行 SSE 数据，提取 content delta。
     *
     * @param line 原始行（含 "data: " 前缀）
     * @return Token/Complete/null（空行或非内容行返回 null）
     */
    fun parseLine(line: String): TokenEvent? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        // [DONE] 信号
        if (trimmed == "data: [DONE]") return TokenEvent.Complete

        // 必须以 "data: " 开头
        if (!trimmed.startsWith("data: ")) return null

        val json = trimmed.removePrefix("data: ")

        // 跳过非数据消息
        if (json == "[DONE]") return TokenEvent.Complete

        return try {
            val delta = extractContent(json)
            if (delta != null) TokenEvent.Token(delta) else null
        } catch (e: Exception) {
            TokenEvent.Error("SSE parse error: ${e.message}")
        }
    }

    /**
     * 从 JSON 中提取 choices[0].delta.content
     */
    private fun extractContent(json: String): String? {
        val deltaIdx = json.indexOf("\"delta\":")
        if (deltaIdx < 0) return null

        val contentIdx = json.indexOf("\"content\":\"", deltaIdx)
        if (contentIdx < 0) return null

        val start = contentIdx + "\"content\":\"".length
        var end = start
        while (end < json.length) {
            val ch = json[end]
            if (ch == '"') {
                // 检查不是转义引号
                if (end > start && json[end - 1] != '\\') break
            }
            end++
        }

        if (end <= start) return null

        return json.substring(start, end)
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
