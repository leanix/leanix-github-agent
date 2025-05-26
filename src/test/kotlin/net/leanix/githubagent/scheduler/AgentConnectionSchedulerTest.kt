package net.leanix.githubagent.scheduler

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.services.WebSocketService
import org.junit.jupiter.api.Test

class AgentConnectionSchedulerTest {

    private var brokerStompSessionHandler: BrokerStompSessionHandler = mockk()
    private var webSocketService: WebSocketService = mockk()
    private var scheduler: AgentConnectionScheduler =
        AgentConnectionScheduler(brokerStompSessionHandler, webSocketService)

    @Test
    fun `should not connect when already connected`() {
        every { brokerStompSessionHandler.isConnected() } returns true

        scheduler.checkConnection()

        verify(exactly = 0) { webSocketService.initSession() }
    }

    @Test
    fun `should connect when not connected`() {
        every { brokerStompSessionHandler.isConnected() } returns false
        every { webSocketService.initSession() } just Runs

        scheduler.checkConnection()

        verify(exactly = 1) { webSocketService.initSession() }
    }
}
