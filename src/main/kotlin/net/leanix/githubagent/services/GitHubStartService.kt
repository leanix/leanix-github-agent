package net.leanix.githubagent.services

import net.leanix.githubagent.dto.ConnectionEstablishedEvent
import net.leanix.githubagent.shared.APP_NAME_TOPIC
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class GitHubStartService(
    private val githubAuthenticationService: GitHubAuthenticationService,
    private val webSocketService: WebSocketService,
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    private val cachingService: CachingService,
) {

    @SuppressWarnings("UnusedParameter")
    @Async
    @EventListener
    fun startAgent(connectionEstablishedEvent: ConnectionEstablishedEvent) {
        githubAuthenticationService.generateAndCacheJwtToken()
        sendAppName()
    }

    private fun sendAppName() {
        val jwt = cachingService.get("jwtToken") as String
        webSocketService.sendMessage(
            APP_NAME_TOPIC,
            gitHubEnterpriseService.getGitHubApp(jwt).slug
        )
    }
}
