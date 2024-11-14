package net.leanix.githubagent.services

import net.leanix.githubagent.dto.LogLevel
import net.leanix.githubagent.dto.SyncLogDto
import net.leanix.githubagent.dto.SynchronizationProgress
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
        sendSyncLog(message, LOGS_TOPIC, LogLevel.ERROR, SynchronizationProgress.RUNNING)
    }

    fun sendSystemErrorLog(message: String) {
        sendSyncLog(message, LOGS_TOPIC, LogLevel.ERROR, SynchronizationProgress.RUNNING)
    }

    fun sendInfoLog(message: String) {
        sendSyncLog(message, LOGS_TOPIC, LogLevel.INFO, SynchronizationProgress.RUNNING)
    }

    fun sendSyncLog(
        message: String? = null,
        topic: String = LOGS_TOPIC,
        logLevel: LogLevel,
        synchronizationProgress: SynchronizationProgress
    ) {
        val runId = cachingService.get("runId")?.let { it as UUID }
        val syncLogDto = SyncLogDto(
            runId = runId,
            trigger = if (runId != null) Trigger.FULL_SYNC else Trigger.WEB_HOOK,
            logLevel = logLevel,
            synchronizationProgress = synchronizationProgress,
            message = message
        )
        webSocketService.sendMessage(constructTopic(topic), syncLogDto)
    }

    private fun constructTopic(topic: String): String {
        return topic
    }
}
