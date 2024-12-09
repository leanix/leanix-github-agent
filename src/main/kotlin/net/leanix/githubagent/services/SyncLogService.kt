package net.leanix.githubagent.services

import net.leanix.githubagent.dto.LogLevel
import net.leanix.githubagent.dto.SyncLogDto
import net.leanix.githubagent.dto.SynchronizationProgress
import net.leanix.githubagent.dto.Trigger
import net.leanix.githubagent.shared.LOGS_TOPIC
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SyncLogService(
    private val webSocketService: WebSocketService,
    private val cachingService: CachingService
) {
    private val logger = LoggerFactory.getLogger(SyncLogService::class.java)

    fun sendFullScanStart() {
        cachingService.set("runId", UUID.randomUUID(), null)
        logger.info("Starting full sync")
        sendSyncLog(
            logLevel = LogLevel.INFO,
            synchronizationProgress = SynchronizationProgress.PENDING,
            message = "Starting synchronization"
        )
    }

    fun sendFullScanSuccess() {
        sendSyncLog(
            logLevel = LogLevel.INFO,
            synchronizationProgress = SynchronizationProgress.FINISHED,
            message = "Synchronization finished."
        )
        cachingService.remove("runId")
        logger.info("Full sync finished")
    }

    fun sendFullScanFailure(errorMessage: String?) {
        val message = "Synchronization aborted. " +
            "An error occurred while scanning GitHub resources. Error: $errorMessage"
        sendSyncLog(
            logLevel = LogLevel.ERROR,
            synchronizationProgress = SynchronizationProgress.ABORTED,
            message = message
        )
        cachingService.remove("runId")
        logger.error(message)
    }

    fun sendErrorLog(message: String) {
        sendSyncLog(message, LOGS_TOPIC, LogLevel.ERROR, SynchronizationProgress.RUNNING)
    }

    fun sendInfoLog(message: String) {
        sendSyncLog(message, LOGS_TOPIC, LogLevel.INFO, SynchronizationProgress.RUNNING)
    }

    fun sendSyncLog(
        message: String,
        topic: String = LOGS_TOPIC,
        logLevel: LogLevel,
        synchronizationProgress: SynchronizationProgress
    ) {
        val runId = cachingService.get("runId")?.let { it as UUID }
        val syncLogDto = SyncLogDto(
            runId = runId,
            trigger = if (runId != null) Trigger.FULL_SCAN else Trigger.WEB_HOOK,
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
