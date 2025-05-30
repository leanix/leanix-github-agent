package net.leanix.githubagent

import jakarta.annotation.PreDestroy
import net.leanix.githubagent.dto.LogLevel
import net.leanix.githubagent.dto.SynchronizationProgress
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.SyncLogService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(
    value = [
        net.leanix.githubagent.config.GitHubEnterpriseProperties::class,
        net.leanix.githubagent.config.LeanIXProperties::class
    ]
)
class GitHubAgentApplication(
    private val syncLogService: SyncLogService,
    private val cachingService: CachingService
) {

    private val logger = LoggerFactory.getLogger(GitHubAgentApplication::class.java)

    @PreDestroy
    fun onShutdown() {
        sendShutdownLog()
    }

    private fun sendShutdownLog() {
        val message = "Agent shutdown."
        val synchronizationProgress = if (cachingService.get("runId") != null) {
            SynchronizationProgress.ABORTED
        } else {
            SynchronizationProgress.FINISHED
        }
        syncLogService.sendSyncLog(
            message = message,
            logLevel = LogLevel.INFO,
            synchronizationProgress = synchronizationProgress
        )
        logger.info(message)
    }
}

fun main() {
    runApplication<GitHubAgentApplication>()
}
