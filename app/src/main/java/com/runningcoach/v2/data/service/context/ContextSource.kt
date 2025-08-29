package com.runningcoach.v2.data.service.context

/**
 * A source of contextual text for the AI chat agent.
 * Implementations can pull from local DB, assets, or remote providers.
 */
interface ContextSource {
    suspend fun build(message: String?): String?
}

