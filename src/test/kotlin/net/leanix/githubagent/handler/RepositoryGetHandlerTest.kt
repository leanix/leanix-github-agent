package net.leanix.githubagent.handler

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.dto.Account
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.RepositoryRequestDTO
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.GitHubRepositoryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.stomp.StompHeaders

class RepositoryGetHandlerTest {

    private val gitHubAuthenticationService = mockk<GitHubAuthenticationService>()

    private val gitHubRepositoryService = mockk<GitHubRepositoryService>()

    private val repositoryGetHandler = RepositoryGetHandler(
        gitHubAuthenticationService,
        gitHubRepositoryService
    )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(repositoryGetHandler)
    }

    @Test
    fun `it should receive message from server and send repository data and manifest files`() {
        // given
        every { gitHubAuthenticationService.getInstallationToken(any()) } returns "token"

        // when
        repositoryGetHandler.handleFrame(
            StompHeaders(),
            RepositoryRequestDTO(
                installation = Installation(1, Account("account"), mapOf(), listOf()),
                repositoryName = "repoName",
                repositoryFullName = "repoFullName"
            )
        )

        // then
        verify {
            gitHubRepositoryService.fetchAndSendRepositoryAndManifest(
                installation = Installation(1, Account("account"), mapOf(), listOf()),
                repositoryName = "repoName",
                repositoryFullName = "repoFullName",
                installationToken = "token"
            )
        }
    }
}
