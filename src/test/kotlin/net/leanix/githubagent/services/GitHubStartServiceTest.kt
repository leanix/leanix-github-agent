package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.dto.ConnectionEstablishedEvent
import net.leanix.githubagent.dto.GitHubAppResponse
import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.shared.APP_NAME_TOPIC
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GitHubStartServiceTest {

    private lateinit var githubAuthenticationService: GitHubAuthenticationService
    private lateinit var webSocketService: WebSocketService
    private lateinit var gitHubScanningService: GitHubScanningService
    private lateinit var gitHubEnterpriseService: GitHubEnterpriseService
    private lateinit var cachingService: CachingService
    private lateinit var gitHubStartService: GitHubStartService
    private lateinit var brokerStompSessionHandler: BrokerStompSessionHandler
    private lateinit var syncLogService: SyncLogService

    @BeforeEach
    fun setUp() {
        githubAuthenticationService = mockk()
        webSocketService = mockk()
        gitHubScanningService = mockk()
        gitHubEnterpriseService = mockk()
        cachingService = mockk()
        brokerStompSessionHandler = mockk()
        syncLogService = mockk()

        gitHubStartService = GitHubStartService(
            githubAuthenticationService,
            webSocketService,
            gitHubEnterpriseService,
            cachingService,
        )

        every { webSocketService.initSession() } returns Unit
        every { webSocketService.sendMessage(any(), any()) } returns Unit
        every { githubAuthenticationService.generateAndCacheJwtToken() } returns Unit
        every { cachingService.get("jwtToken") } returns "jwt"
        every { cachingService.set("runId", any(), any()) } returns Unit
        every { cachingService.remove("runId") } returns Unit
        every { gitHubScanningService.scanGitHubResources() } returns Unit
        every { brokerStompSessionHandler.isConnected() } returns true
        every { syncLogService.sendSyncLog(any(), any(), any(), any()) } returns Unit
        every { syncLogService.sendFullScanStart(any()) } returns Unit
        every { syncLogService.sendFullScanSuccess() } returns Unit
    }

    @Test
    fun `should start syncLog and send GitHub App name`() {
        val gitHubAppName = "appName"
        every { gitHubEnterpriseService.getGitHubApp("jwt") } returns
            GitHubAppResponse(
                gitHubAppName, mapOf(), listOf()
            )

        gitHubStartService.startAgent(ConnectionEstablishedEvent())

        verify { webSocketService.sendMessage(APP_NAME_TOPIC, gitHubAppName) }
    }
}
