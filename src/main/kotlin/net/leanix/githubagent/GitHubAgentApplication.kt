package net.leanix.githubagent

import jakarta.annotation.PreDestroy
import net.leanix.githubagent.dto.LogLevel
import net.leanix.githubagent.dto.Trigger
import net.leanix.githubagent.services.SyncLogService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(
    value = [
        net.leanix.githubagent.config.GitHubEnterpriseProperties::class,
        net.leanix.githubagent.config.LeanIXProperties::class
    ]
)
class GitHubAgentApplication(
    private val syncLogService: SyncLogService
){

    private val logger = LoggerFactory.getLogger(GitHubAgentApplication::class.java)

    @PreDestroy
    fun onShutdown() {
        sendErrorSyncLog()
    }

    fun sendErrorSyncLog() {
        val message = "Agent shutdown."
        syncLogService.sendSyncLog(
            trigger = Trigger.ABORTED_FULL_SYNC,
            logLevel = LogLevel.ERROR,
            message = message
        )
        logger.error(message)
    }
}

fun main() {
    runApplication<GitHubAgentApplication>()
}
