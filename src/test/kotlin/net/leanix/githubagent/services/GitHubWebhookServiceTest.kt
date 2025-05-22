package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.exceptions.InvalidEventSignatureException
import net.leanix.githubagent.exceptions.WebhookSecretNotSetException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GitHubWebhookServiceTest {

    private val webhookEventService = mockk<WebhookEventService>()
    private val gitHubEnterpriseProperties = mockk<GitHubEnterpriseProperties>()
    private val cachingService = mockk<CachingService>()
    private val gitHubWebhookService = GitHubWebhookService(
        webhookEventService,
        gitHubEnterpriseProperties,
        cachingService,
    )

    @BeforeEach
    fun setUp() {
        every { cachingService.get("runId") } returns null
    }

    @Test
    fun `should not process event if unknown host`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"

        gitHubWebhookService.handleWebhookEvent("PUSH", "unknown.host", null, "{}")

        verify(exactly = 0) { webhookEventService.consumeWebhookEvent(any(), any()) }
    }

    @Test
    fun `should throw WebhookSecretNotSetException when signature is present but secret is not set`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""

        assertThrows<WebhookSecretNotSetException> {
            gitHubWebhookService.handleWebhookEvent("PUSH", "known.host", "sha256=signature", "{}")
        }
    }

    @Test
    fun `should throw InvalidEventSignatureException for invalid signature`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns "secret"

        assertThrows<InvalidEventSignatureException> {
            gitHubWebhookService.handleWebhookEvent("PUSH", "known.host", "sha256=signature", "{}")
        }
    }

    @Test
    fun `should not process unsupported event type`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""

        gitHubWebhookService.handleWebhookEvent("UNSUPPORTED_EVENT", "known.host", null, "{}")

        verify(exactly = 0) { webhookEventService.consumeWebhookEvent(any(), any()) }
    }

    @Test
    fun `should process supported event type successfully`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""
        every { webhookEventService.consumeWebhookEvent(any(), any()) } returns Unit

        gitHubWebhookService.handleWebhookEvent("PUSH", "host", null, "{}")

        verify { webhookEventService.consumeWebhookEvent("PUSH", "{}") }
    }

    @Test
    fun `should ignore event when runId is present and event type is not INSTALLATION`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""
        every { webhookEventService.consumeWebhookEvent(any(), any()) } returns Unit
        every { cachingService.get("runId") } returns "someRunId"

        gitHubWebhookService.handleWebhookEvent("PUSH", "host", null, "{}")

        verify(exactly = 0) { webhookEventService.consumeWebhookEvent(any(), any()) }
    }
}
