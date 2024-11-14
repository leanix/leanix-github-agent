package net.leanix.githubagent.runners

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.dto.GitHubAppResponse
import net.leanix.githubagent.dto.LogLevel
import net.leanix.githubagent.dto.SynchronizationProgress
import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.GitHubEnterpriseService
import net.leanix.githubagent.services.GitHubScanningService
import net.leanix.githubagent.services.SyncLogService
import net.leanix.githubagent.services.WebSocketService
import net.leanix.githubagent.shared.APP_NAME_TOPIC
import net.leanix.githubagent.shared.LOGS_TOPIC
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PostStartupRunnerTest {

    private lateinit var githubAuthenticationService: GitHubAuthenticationService
    private lateinit var webSocketService: WebSocketService
    private lateinit var gitHubScanningService: GitHubScanningService
    private lateinit var gitHubEnterpriseService: GitHubEnterpriseService
    private lateinit var cachingService: CachingService
    private lateinit var postStartupRunner: PostStartupRunner
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

        postStartupRunner = PostStartupRunner(
            githubAuthenticationService,
            webSocketService,
            gitHubScanningService,
            gitHubEnterpriseService,
            cachingService,
            brokerStompSessionHandler,
            syncLogService
        )

        every { webSocketService.initSession() } returns Unit
        every { webSocketService.sendMessage(any(), any()) } returns Unit
        every { githubAuthenticationService.generateAndCacheJwtToken() } returns Unit
        every { cachingService.get("jwtToken") } returns "jwt"
        every { cachingService.set("runId", any(), any()) } returns Unit
        every { gitHubScanningService.scanGitHubResources() } returns Unit
        every { brokerStompSessionHandler.isConnected() } returns true
        every { syncLogService.sendSyncLog(any(), any(), any(), any()) } returns Unit
    }

    @Test
    fun `should start syncLog and send GitHub App name`() {
        val gitHubAppName = "appName"
        every { gitHubEnterpriseService.getGitHubApp("jwt") } returns
            GitHubAppResponse(
                gitHubAppName, mapOf(), listOf()
            )

        postStartupRunner.run(mockk())

        verify { webSocketService.sendMessage(APP_NAME_TOPIC, gitHubAppName) }
        verify { syncLogService.sendSyncLog(null, LOGS_TOPIC, LogLevel.INFO, SynchronizationProgress.PENDING) }
    }
}
