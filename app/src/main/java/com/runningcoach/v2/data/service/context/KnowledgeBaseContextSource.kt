package com.runningcoach.v2.data.service.context

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Loads concise, general health/fitness guidance snippets from assets/knowledge-base
 * based on topic keywords present in the user message. Keeps things modular and easy to extend.
 */
class KnowledgeBaseContextSource(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ContextSource {

    @Serializable
    data class Topic(
        val id: String,
        val name: String,
        val file: String,
        val keywords: List<String> = emptyList()
    )
    @Serializable
    data class Index(val topics: List<Topic> = emptyList())

    private val kbRoot = "knowledge-base"
    private val indexPath = "$kbRoot/index.json"

    override suspend fun build(message: String?): String? {
        return try {
            val index = loadIndex() ?: return null
            val lower = (message ?: "").lowercase()
            val matched = index.topics.filter { t ->
                t.keywords.any { k -> lower.contains(k.lowercase()) }
            }.take(2) // Limit to keep prompts concise

            if (matched.isEmpty()) return null
            val snippets = matched.mapNotNull { t ->
                val text = loadAsset("${kbRoot}/${t.file}") ?: return@mapNotNull null
                val trimmed = text.trim().take(1200) // Soft cap to avoid ballooning tokens
                "KB-${t.name}:\n$trimmed"
            }
            if (snippets.isEmpty()) null else snippets.joinToString("\n\n")
        } catch (_: Exception) {
            null
        }
    }

    private fun loadIndex(): Index? {
        val raw = loadAsset(indexPath) ?: return null
        return json.decodeFromString(Index.serializer(), raw)
    }

    private fun loadAsset(path: String): String? = try {
        context.assets.open(path).bufferedReader().use { it.readText() }
    } catch (_: Exception) { null }
}

