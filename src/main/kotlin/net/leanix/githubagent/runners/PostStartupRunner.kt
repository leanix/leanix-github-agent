package net.leanix.githubagent.runners

import net.leanix.githubagent.services.GithubAuthenticationService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class PostStartupRunner(private val githubAuthenticationService: GithubAuthenticationService) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        githubAuthenticationService.generateJwtToken()
    }
}
