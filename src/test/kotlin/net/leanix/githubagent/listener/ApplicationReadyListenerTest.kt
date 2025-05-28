package net.leanix.githubagent.listener

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.services.WebSocketService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplicationReadyListenerTest {

    private lateinit var applicationListener: ApplicationReadyListener
    private var webSocketService: WebSocketService = mockk()

    @BeforeEach
    fun setUp() {
        applicationListener = ApplicationReadyListener(
            webSocketService
        )

        every { webSocketService.initSession() } returns Unit
    }

    @Test
    fun `should start the agent process`() {
        applicationListener.onApplicationEvent(mockk())

        verify { webSocketService.initSession() }
    }
}
