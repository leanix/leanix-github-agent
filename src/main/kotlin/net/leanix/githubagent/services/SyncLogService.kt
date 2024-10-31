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
        sendSyncLog(message, LOGS_TOPIC, Trigger.PROGRESS_FULL_SYNC, LogLevel.ERROR)
    }

    fun sendSystemErrorLog(message: String) {
        sendSyncLog(message, LOGS_TOPIC, Trigger.SYSTEM, LogLevel.ERROR)
    }

    fun sendInfoLog(message: String) {
        sendSyncLog(message, LOGS_TOPIC, Trigger.PROGRESS_FULL_SYNC, LogLevel.INFO)
    }

    fun sendSyncLog(message: String? = null, topic: String = LOGS_TOPIC, trigger: Trigger, logLevel: LogLevel) {
        val runId = cachingService.get("runId")?.let { it as UUID }
        val syncLogDto = SyncLogDto(
            runId = runId,
            trigger = trigger,
            logLevel = logLevel,
            message = message
        )
        webSocketService.sendMessage(constructTopic(topic), syncLogDto)
    }

    private fun constructTopic(topic: String): String {
        return topic
    }
}
