package net.leanix.githubagent.services

import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.shared.APP_NAME_TOPIC
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class GitHubStartService(
    private val githubAuthenticationService: GitHubAuthenticationService,
    private val webSocketService: WebSocketService,
    private val gitHubScanningService: GitHubScanningService,
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    private val cachingService: CachingService,
    private val brokerStompSessionHandler: BrokerStompSessionHandler,
    private val syncLogService: SyncLogService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Async
    fun startAgent() {
        webSocketService.initSession()
        if (!brokerStompSessionHandler.isConnected()) {
            logger.error("Stopping the application as the WebSocket connection could not be established.")
            return
        }
        kotlin.runCatching {
            syncLogService.sendFullScanStart(null)
            scanResources()
        }.onSuccess {
            syncLogService.sendFullScanSuccess()
        }.onFailure {
            syncLogService.sendFullScanFailure(it.message)
        }
    }

    private fun scanResources() {
        githubAuthenticationService.generateAndCacheJwtToken()
        val jwt = cachingService.get("jwtToken") as String
        webSocketService.sendMessage(
            APP_NAME_TOPIC,
            gitHubEnterpriseService.getGitHubApp(jwt).slug
        )
        gitHubScanningService.scanGitHubResources()
    }
}
