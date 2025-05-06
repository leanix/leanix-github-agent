package net.leanix.githubagent.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.services.LeanIXAuthService
import net.leanix.githubagent.shared.GitHubAgentProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.messaging.WebSocketStompClient

class WebSocketClientConfigTests {
    private lateinit var webSocketClientConfig: WebSocketClientConfig
    private lateinit var stompClient: WebSocketStompClient
    private lateinit var stompSession: StompSession
    private lateinit var authService: LeanIXAuthService
    private lateinit var leanIXProperties: LeanIXProperties
    private lateinit var gitHubEnterpriseProperties: GitHubEnterpriseProperties
    private lateinit var leanIXAuthService: LeanIXAuthService

    @BeforeEach
    fun setUp() {
        val brokerStompSessionHandler = mockk<BrokerStompSessionHandler>()
        val objectMapper = mockk<ObjectMapper>()
        leanIXProperties = mockk()
        gitHubEnterpriseProperties = mockk()
        stompClient = mockk()
        stompSession = mockk()
        authService = mockk()
        leanIXAuthService = mockk()
        webSocketClientConfig = WebSocketClientConfig(
            brokerStompSessionHandler,
            objectMapper,
            leanIXAuthService,
            leanIXProperties,
            gitHubEnterpriseProperties
        )

        GitHubAgentProperties.GITHUB_AGENT_VERSION = "test-version"
    }

    @Test
    fun `initSession should fail after max retry attempts`() = runBlocking {
        coEvery { authService.getBearerToken() } returns "validToken"
        coEvery { leanIXProperties.wsBaseUrl } returns "ws://test.url"
        coEvery { gitHubEnterpriseProperties.baseUrl } returns "http://github.enterprise.url"
        coEvery { gitHubEnterpriseProperties.gitHubAppId } returns "appId"
        coEvery { leanIXAuthService.getBearerToken() } returns "bearer token"
        coEvery {
            stompClient.connectAsync(
                any<String>(), any<WebSocketHttpHeaders>(),
                any<StompHeaders>(), any<BrokerStompSessionHandler>()
            )
        } throws RuntimeException("Connection failed")

        val session = runCatching { webSocketClientConfig.initSession() }.getOrNull()

        assertEquals(null, session)
    }
}
