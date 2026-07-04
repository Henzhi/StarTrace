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
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)   // 长篇故事可能需要更长时间
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
            // 不设 max_tokens：部分 LLM 后端（Ollama/vLLM/本地模型）数值设太高反而
            // 会被忽略回落到很低的服务端默认值。不传则走各后端自身的"无限制/填满上下文"逻辑
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

        // 提取标题并剔除正文中的 # 标题行
        val title = extractTitle(content)
        val storyBody = stripTitleLine(content).trim().trimEnd('，', ',', '"', '\'')

        // 构建 StoryEntity
        StoryEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            content = storyBody,
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
        // 篇幅提示：预设映射为字数；自定义直接透传用户输入
        val lengthHint = when (length) {
            "short" -> "800 字左右"
            "medium" -> "1500 字左右"
            "long" -> "3000 字左右"
            else -> length  // 自定义字数（如 "5000 字"、"8000字中等篇幅" 等）
        }
        return "你是一个创意写作助手。请根据用户提供的灵感碎片，创作一段连贯的${styleDesc}故事。" +
                "篇幅参考：${lengthHint}，但故事完整自然收尾的优先级远高于字数限制。" +
                "你必须在故事达到一个自然的结束点时才停止，绝不能在半句或逗号后停止。" +
                "如果故事需要超出参考字数才能完整，就超出。" +
                "格式要求：第一行必须以\"# 标题内容\"的格式输出标题（例如\"# 星辰之外\"），" +
                "从第二行开始输出故事正文。正文中不要再次出现\"#\"符号。" +
                "请直接按此格式输出，不要有其他额外说明。"
    }

    private fun buildUserPrompt(fragments: List<FragmentEntity>): String {
        if (fragments.isEmpty()) return "请根据你的创意写一个完整的故事，确保有自然的结尾。"
        val fragmentsText = fragments.joinToString("\n\n") { f ->
            val emoji = domainEmoji(f.domainTag)
            "【$emoji ${f.domainTag}】${f.content}"
        }
        return "以下是我的灵感碎片，请将它们编织成一个连贯的故事：\n\n$fragmentsText" +
                "\n\n请务必将故事写到完整结束，有一个自然、令人满意的结尾。不要在句子中间停下来。"
    }

    /**
     * 从内容中提取标题。第一行若以 \"# \" 开头则取其后内容；
     * 否则取第一句（最多 20 字）。
     */
    private fun extractTitle(content: String): String {
        val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: return "未命名故事"
        return if (firstLine.startsWith("# ")) {
            firstLine.removePrefix("# ").trim().take(50)
        } else {
            firstLine.trim().take(20) + if (firstLine.length > 20) "..." else ""
        }
    }

    /** 剔除内容中的 # 标题行（若第一非空行以 "# " 开头），返回正文内容 */
    private fun stripTitleLine(content: String): String {
        val lines = content.lines()
        val firstNonBlankIdx = lines.indexOfFirst { it.isNotBlank() }
        if (firstNonBlankIdx < 0) return content
        return if (lines[firstNonBlankIdx].startsWith("# ")) {
            lines.filterIndexed { i, _ -> i != firstNonBlankIdx }.joinToString("\n")
        } else content
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
            // 不设 max_tokens：部分 LLM 后端数值设太高反而会被忽略回落到很低的服务端默认值
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

        var wasTruncated = false
        reader.use { r ->
            var line: String?
            while (r.readLine().also { line = it } != null) {
                val rawLine = line!!

                // 服务端强制截断标记：finish_reason=length 说明 AI 还有内容但被 token 限制卡断了
                if (rawLine.contains("\"finish_reason\":\"length\"")) {
                    wasTruncated = true
                }

                val event = SSEMessageParser.parseLine(rawLine)
                when (event) {
                    is TokenEvent.Token -> emit(event)
                    is TokenEvent.Complete -> {
                        if (wasTruncated) emit(TokenEvent.Truncated)
                        else emit(event)
                        return@flow
                    }
                    is TokenEvent.Truncated -> { emit(event); return@flow }
                    is TokenEvent.Error -> { emit(event); return@flow }
                    null -> { /* skip empty/non-content lines */ }
                }
            }
        }
        if (wasTruncated) emit(TokenEvent.Truncated)
        else emit(TokenEvent.Complete)
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
