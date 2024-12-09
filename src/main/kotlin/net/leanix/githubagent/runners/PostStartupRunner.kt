package net.leanix.githubagent.runners

import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.GitHubEnterpriseService
import net.leanix.githubagent.services.GitHubScanningService
import net.leanix.githubagent.services.SyncLogService
import net.leanix.githubagent.services.WebSocketService
import net.leanix.githubagent.shared.APP_NAME_TOPIC
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class PostStartupRunner(
    private val githubAuthenticationService: GitHubAuthenticationService,
    private val webSocketService: WebSocketService,
    private val gitHubScanningService: GitHubScanningService,
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    private val cachingService: CachingService,
    private val brokerStompSessionHandler: BrokerStompSessionHandler,
    private val syncLogService: SyncLogService
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(PostStartupRunner::class.java)

    override fun run(args: ApplicationArguments?) {
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
