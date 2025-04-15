package net.leanix.githubagent.handler

import net.leanix.githubagent.dto.RepositoryRequestDTO
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.GitHubRepositoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type

@Component
class RepositoryGetHandler(
    @Lazy @Autowired
    private val gitHubAuthenticationService: GitHubAuthenticationService,
    @Lazy @Autowired
    private val repositoryGetService: GitHubRepositoryService
) : StompFrameHandler {

    private val logger = LoggerFactory.getLogger(RepositoryGetHandler::class.java)

    override fun getPayloadType(headers: StompHeaders): Type {
        return RepositoryRequestDTO::class.java
    }

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        payload?.let {
            val dto = payload as RepositoryRequestDTO
            logger.info("Received repository get message from server for repo: ${dto.repositoryName}")
            runCatching {
                val installationToken = gitHubAuthenticationService.getInstallationToken(dto.installation.id.toInt())
                repositoryGetService.fetchAndSendRepositoryAndManifest(
                    dto.installation,
                    dto.repositoryName,
                    dto.repositoryFullName,
                    installationToken
                )
            }
        }
    }
}
