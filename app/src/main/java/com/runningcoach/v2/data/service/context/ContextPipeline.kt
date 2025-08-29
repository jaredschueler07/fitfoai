package com.runningcoach.v2.data.service.context

/**
 * Aggregates multiple ContextSource providers into a single block.
 */
class ContextPipeline(
    private val sources: List<ContextSource>,
    private val joiner: String = "\n"
) {
    suspend fun build(message: String?): String? {
        val parts = sources.mapNotNull { it.build(message)?.trim() }.filter { it.isNotEmpty() }
        return if (parts.isEmpty()) null else parts.joinToString(joiner)
    }
}

