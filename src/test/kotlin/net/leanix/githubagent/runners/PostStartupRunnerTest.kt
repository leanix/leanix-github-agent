package net.leanix.githubagent.runners

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.dto.GitHubAppResponse
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.GitHubEnterpriseService
import net.leanix.githubagent.services.GitHubScanningService
import net.leanix.githubagent.services.WebSocketService
import net.leanix.githubagent.shared.AGENT_METADATA_TOPIC
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PostStartupRunnerTest {

    private lateinit var githubAuthenticationService: GitHubAuthenticationService
    private lateinit var webSocketService: WebSocketService
    private lateinit var gitHubScanningService: GitHubScanningService
    private lateinit var gitHubEnterpriseService: GitHubEnterpriseService
    private lateinit var cachingService: CachingService
    private lateinit var postStartupRunner: PostStartupRunner

    @BeforeEach
    fun setUp() {
        githubAuthenticationService = mockk()
        webSocketService = mockk()
        gitHubScanningService = mockk()
        gitHubEnterpriseService = mockk()
        cachingService = mockk()

        postStartupRunner = PostStartupRunner(
            githubAuthenticationService,
            webSocketService,
            gitHubScanningService,
            gitHubEnterpriseService,
            cachingService
        )

        every { webSocketService.initSession() } returns Unit
        every { webSocketService.sendMessage(any(), any()) } returns Unit
        every { githubAuthenticationService.generateAndCacheJwtToken() } returns Unit
        every { cachingService.get("jwt") } returns "jwt"
        every { gitHubScanningService.scanGitHubResources() } returns Unit
    }

    @Test
    fun `should send GitHub App name`() {
        val gitHubAppName = "appName"
        every { gitHubEnterpriseService.getGitHubAppData("jwt") } returns
            GitHubAppResponse(
                gitHubAppName, mapOf(), listOf()
            )

        postStartupRunner.run(mockk())

        verify { webSocketService.sendMessage(AGENT_METADATA_TOPIC, gitHubAppName) }
    }
}
