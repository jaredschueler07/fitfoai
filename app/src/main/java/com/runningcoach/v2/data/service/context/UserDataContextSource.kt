package com.runningcoach.v2.data.service.context

import com.runningcoach.v2.data.service.ChatContextProvider

/**
 * Provides compact, structured context from in-app data (profile + fitness summaries).
 */
class UserDataContextSource(
    private val provider: ChatContextProvider
) : ContextSource {
    override suspend fun build(message: String?): String? {
        return provider.buildFullContextBlock()
    }
}

