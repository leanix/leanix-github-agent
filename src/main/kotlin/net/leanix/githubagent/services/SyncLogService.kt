package net.leanix.githubagent.services

import net.leanix.githubagent.dto.LogLevel
import net.leanix.githubagent.dto.SyncLogDto
import net.leanix.githubagent.dto.Trigger
import net.leanix.githubagent.shared.LOGS_TOPIC
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SyncLogService(
    private val webSocketService: WebSocketService,
    private val cachingService: CachingService
) {
    fun sendErrorLog(message: String) {
        sendSyncLog(message, LOGS_TOPIC, null, LogLevel.ERROR)
    }

    fun sendInfoLog(message: String) {
        sendSyncLog(message, LOGS_TOPIC, null, LogLevel.INFO)
    }

    fun sendSyncLog(message: String? = null, topic: String = LOGS_TOPIC, trigger: Trigger?, logLevel: LogLevel) {
        val runId = cachingService.get("runId") as UUID
        val syncLogDto = SyncLogDto(
            runId = runId,
            trigger = trigger ?: Trigger.GENERIC,
            logLevel = logLevel,
            message = message
        )
        webSocketService.sendMessage(constructTopic(topic), syncLogDto)
    }

    private fun constructTopic(topic: String): String {
        return topic
    }
}
