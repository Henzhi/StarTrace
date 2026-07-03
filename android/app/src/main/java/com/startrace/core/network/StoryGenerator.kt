package com.startrace.core.network

import com.startrace.core.database.entity.FragmentEntity
import com.startrace.core.database.entity.LLMConfigEntity
import com.startrace.core.database.entity.StoryEntity
import com.startrace.core.security.KeyStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LLM 故事生成服务。
 *
 * 使用 OkHttp 直接调用 OpenAI 兼容 API（/v1/chat/completions），
 * 将选中的碎片拼装为 Prompt，AI 生成故事后存入 StoryEntity。
 */
@Singleton
class StoryGenerator @Inject constructor(
    private val keyStore: KeyStoreManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 异步生成故事。
     *
     * @param config LLM 配置（API 地址 + 模型名）
     * @param fragments 选中的碎片列表
     * @param style 故事风格 (scifi/fantasy/realistic/prose/poetry/mystery)
     * @param length 故事长度 (short/medium/long)
     * @return StoryEntity（已填充 id、坐标等），失败则抛异常
     */
    suspend fun generate(
        config: LLMConfigEntity,
        fragments: List<FragmentEntity>,
        style: String,
        length: String
    ): StoryEntity = withContext(Dispatchers.IO) {
        val apiKey = keyStore.decrypt(config.id)
            ?: throw IllegalStateException("API Key 未配置")

        // 构建 Prompt
        val systemPrompt = buildSystemPrompt(style, length)
        val userPrompt = buildUserPrompt(fragments)

        // 构建请求体
        val body = JSONObject().apply {
            put("model", config.modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("temperature", 0.8)
            put("max_tokens", when (length) {
                "short" -> 3000; "medium" -> 8000; else -> 16000
            })
        }

        val request = Request.Builder()
            .url(config.apiUrl.trimEnd('/') + "/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("空响应")

        if (!response.isSuccessful) {
            throw Exception("API 错误 ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val choices = json.getJSONArray("choices")
        val message = choices.getJSONObject(0).getJSONObject("message")
        val content = message.getString("content")

        // 提取标题（取第一行或 AI 返回的标题）
        val title = extractTitle(content)

        // 构建 StoryEntity
        StoryEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content.trim(),
            fragmentIdsJson = JSONArray(fragments.map { it.id }).toString(),
            length = length,
            style = style,
            positionX = (Math.random() * 400 - 200).toFloat(),
            positionY = (Math.random() * 400 - 200).toFloat(),
            llmConfigId = config.id,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun buildSystemPrompt(style: String, length: String): String {
        val styleDesc = when (style) {
            "scifi" -> "科幻风格，包含未来科技、太空探索、人工智能等元素"
            "fantasy" -> "奇幻风格，包含魔法、神话生物、异世界等元素"
            "realistic" -> "现实主义风格，贴近现实生活"
            "prose" -> "散文风格，语言优美富有诗意"
            "poetry" -> "诗歌风格，韵律感强，意象丰富"
            "mystery" -> "悬疑风格，充满悬念和推理"
            else -> "创意写作"
        }
        val lengthDesc = when (length) {
            "short" -> "800-1500 字"
            "medium" -> "3000-5000 字"
            "long" -> "6000-10000 字"
            else -> "适中的篇幅"
        }
        return "你是一个创意写作助手。请根据用户提供的灵感碎片，创作一段连贯的${styleDesc}故事。" +
                "故事长度约$lengthDesc。需要有一个吸引人的标题。请直接输出故事，不要有额外说明。"
    }

    private fun buildUserPrompt(fragments: List<FragmentEntity>): String {
        if (fragments.isEmpty()) return "请根据你的创意写一个故事。"
        val fragmentsText = fragments.joinToString("\n\n") { f ->
            val emoji = domainEmoji(f.domainTag)
            "【$emoji ${f.domainTag}】${f.content}"
        }
        return "以下是我的灵感碎片，请将它们编织成一个连贯的故事：\n\n$fragmentsText"
    }

    private fun extractTitle(content: String): String {
        // 尝试从内容中提取 ## 或 ** 标记的标题
        val lines = content.lines()
        for (line in lines) {
            val cleaned = line.trimStart('#', ' ', '*').trim()
            if (cleaned.isNotBlank() && cleaned.length <= 50) {
                return cleaned
            }
        }
        // 取第一句（最多 20 字）
        return content.take(20).trim() + if (content.length > 20) "..." else ""
    }

    /**
     * 流式生成故事 — 通过 Server-Sent Events 逐 token 返回。
     *
     * @return Flow<TokenEvent> — Token/Complete/Error 事件序列
     */
    fun generateStream(
        config: LLMConfigEntity,
        fragments: List<FragmentEntity>,
        style: String,
        length: String
    ): Flow<TokenEvent> = flow {
        val apiKey = keyStore.decrypt(config.id)
            ?: throw IllegalStateException("API Key 未配置")

        val body = JSONObject().apply {
            put("model", config.modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", buildSystemPrompt(style, length))
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", buildUserPrompt(fragments))
                })
            })
            put("temperature", 0.8)
            put("max_tokens", when (length) {
                "short" -> 3000; "medium" -> 8000; else -> 16000
            })
            put("stream", true)
        }

        val request = Request.Builder()
            .url(config.apiUrl.trimEnd('/') + "/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: "未知错误"
            emit(TokenEvent.Error("HTTP ${response.code}: $errBody"))
            return@flow
        }

        val reader = response.body?.byteStream()?.bufferedReader() ?: run {
            emit(TokenEvent.Error("空响应流"))
            return@flow
        }

        reader.use { r ->
            var line: String?
            while (r.readLine().also { line = it } != null) {
                val event = SSEMessageParser.parseLine(line!!)
                when (event) {
                    is TokenEvent.Token -> emit(event)
                    is TokenEvent.Complete -> { emit(event); return@flow }
                    is TokenEvent.Error -> { emit(event); return@flow }
                    null -> { /* skip empty/non-content lines */ }
                }
            }
        }
        emit(TokenEvent.Complete)
    }.flowOn(Dispatchers.IO).buffer(Channel.BUFFERED)

    private fun domainEmoji(tag: String): String = when (tag) {
        "world" -> "🌍"
        "character" -> "👤"
        "plot" -> "📖"
        "dialogue" -> "💬"
        "setting" -> "⚙️"
        "thought" -> "💡"
        else -> "📌"
    }
}
