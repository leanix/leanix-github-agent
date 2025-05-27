package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import net.leanix.githubagent.config.WebSocketClientConfig
import net.leanix.githubagent.handler.BrokerStompSessionHandler
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.stomp.StompSession

class WebSocketServiceTests {

    private lateinit var webSocketService: WebSocketService
    private val webSocketClientConfig: WebSocketClientConfig = mockk()
    private val stompSession: StompSession = mockk()
    private val brokerStompSessionHandler: BrokerStompSessionHandler = mockk()
    private val cachingService: CachingService = mockk()

    @BeforeEach
    fun setUp() {
        webSocketService = WebSocketService(webSocketClientConfig, brokerStompSessionHandler, cachingService)
    }

    @Test
    fun `initSession should initialize stompSession successfully`() {
        every { webSocketClientConfig.initSession() } returns stompSession
        every { brokerStompSessionHandler.isConnected() } returns true

        webSocketService.initSession()

        assertNotNull(webSocketService.stompSession)
    }
}
