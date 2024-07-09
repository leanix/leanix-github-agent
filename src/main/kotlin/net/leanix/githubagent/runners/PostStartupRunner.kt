package net.leanix.githubagent.runners

import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.WebSocketService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class PostStartupRunner(
    private val githubAuthenticationService: GitHubAuthenticationService,
    private val webSocketService: WebSocketService
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        githubAuthenticationService.generateJwtToken()
        webSocketService.initSession()
    }
}
