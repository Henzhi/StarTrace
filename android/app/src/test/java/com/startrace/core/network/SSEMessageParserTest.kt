package com.startrace.core.network

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SSEMessageParser")
class SSEMessageParserTest {

    @Test
    @DisplayName("解析正常 token 行")
    fun `parse token line`() {
        val line = """data: {"choices":[{"delta":{"content":"Hello"}}]}"""
        val event = SSEMessageParser.parseLine(line)
        assertTrue(event is TokenEvent.Token)
        assertEquals("Hello", (event as TokenEvent.Token).text)
    }

    @Test
    @DisplayName("解析 DONE 信号")
    fun `parse done signal`() {
        assertEquals(TokenEvent.Complete, SSEMessageParser.parseLine("data: [DONE]"))
    }

    @Test
    @DisplayName("空行返回 null")
    fun `empty line returns null`() {
        assertNull(SSEMessageParser.parseLine(""))
        assertNull(SSEMessageParser.parseLine("   "))
    }

    @Test
    @DisplayName("非 data 行返回 null")
    fun `non-data line returns null`() {
        assertNull(SSEMessageParser.parseLine("event: message"))
    }

    @Test
    @DisplayName("data 行无 content 返回 null")
    fun `data line without content returns null`() {
        val line = """data: {"id":"123"}"""
        assertNull(SSEMessageParser.parseLine(line))
    }

    @Test
    @DisplayName("多 content 行正确解析")
    fun `multiple content lines parse correctly`() {
        var total = ""
        listOf(
            """data: {"choices":[{"delta":{"content":"你好"}}]}""",
            """data: {"choices":[{"delta":{"content":"世界"}}]}""",
            "data: [DONE]"
        ).forEach { line ->
            when (val ev = SSEMessageParser.parseLine(line)) {
                is TokenEvent.Token -> total += ev.text
                is TokenEvent.Complete -> {}
                else -> fail("Unexpected: $ev")
            }
        }
        assertEquals("你好世界", total)
    }

    @Test
    @DisplayName("转义字符正确解码")
    fun `escape characters are decoded`() {
        // 在 triple-quoted 字符串中，\n 表示反斜杠+n（两个字符），模拟 JSON 中的转义
        val line = """data: {"choices":[{"delta":{"content":"line1\nline2\ttab"}}]}"""
        val event = SSEMessageParser.parseLine(line) as TokenEvent.Token
        assertEquals("line1\nline2\ttab", event.text)
    }

    @Test
    @DisplayName("不含 delta 的 data 行返回 null")
    fun `data line without delta returns null`() {
        val line = """data: {"id":"123","object":"chat.completion"}"""
        assertNull(SSEMessageParser.parseLine(line))
    }
}
