package net.leanix.githubagent.handler

import io.mockk.mockkObject
import io.mockk.verify
import net.leanix.githubagent.services.GitHubWebhookService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.stomp.StompHeaders

class ConfigurationSetHandlerTest {

    private val configurationSetHandler = ConfigurationSetHandler()

    @BeforeEach
    fun setUp() {
        mockkObject(GitHubWebhookService.Companion)
    }

    @Test
    fun `should update supported webhook events when payload is valid`() {
        val headers = StompHeaders()
        val supportedEvents = listOf(
            SupportedWebhookEvent("PUSH", listOf("created", "updated")),
            SupportedWebhookEvent("PULL_REQUEST", listOf("opened", "closed"))
        )
        val payload = AgentConfigurationRequest(supportedWebhookEvents = supportedEvents)

        configurationSetHandler.handleFrame(headers, payload)

        verify { GitHubWebhookService.updateSupportedWebhookEvents(supportedEvents) }
        assertEquals(GitHubWebhookService.getSupportedWebhookEvents().size, supportedEvents.size)
    }

    @Test
    fun `should not update supported webhook events when payload is null`() {
        val headers = StompHeaders()

        configurationSetHandler.handleFrame(headers, null)

        verify(exactly = 0) { GitHubWebhookService.updateSupportedWebhookEvents(any()) }
    }
}
