package net.leanix.githubagent.services

import net.leanix.githubagent.dto.ConnectionEstablishedEvent
import net.leanix.githubagent.exceptions.UnableToSendMessageException
import net.leanix.githubagent.shared.APP_NAME_TOPIC
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class GitHubStartService(
    private val githubAuthenticationService: GitHubAuthenticationService,
    private val webSocketService: WebSocketService,
    private val gitHubScanningService: GitHubScanningService,
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    private val cachingService: CachingService,
    private val syncLogService: SyncLogService
) {
    var requireScan = true

    @SuppressWarnings("UnusedParameter")
    @Async
    @EventListener
    fun startAgent(connectionEstablishedEvent: ConnectionEstablishedEvent) {
        if (requireScan) {
            runCatching {
                requireScan = false
                syncLogService.sendFullScanStart(null)
                scanResources()
            }.onSuccess {
                syncLogService.sendFullScanSuccess()
            }.onFailure {
                if (it is UnableToSendMessageException) requireScan = true
                syncLogService.sendFullScanFailure(it.message)
            }
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
