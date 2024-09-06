package net.leanix.githubagent.runners

import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.GitHubEnterpriseService
import net.leanix.githubagent.services.GitHubScanningService
import net.leanix.githubagent.services.WebSocketService
import net.leanix.githubagent.shared.APP_NAME_TOPIC
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
    private val cachingService: CachingService
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        webSocketService.initSession()
        githubAuthenticationService.generateAndCacheJwtToken()
        val jwt = cachingService.get("jwt") as String
        webSocketService.sendMessage(
            APP_NAME_TOPIC,
            gitHubEnterpriseService.getGitHubApp(jwt).name
        )
        gitHubScanningService.scanGitHubResources()
    }
}
