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
import java.lang.reflect.Field

class GitHubWebhookHandlerTest {

    private val webhookEventService = mockk<WebhookEventService>()
    private val gitHubEnterpriseProperties = mockk<GitHubEnterpriseProperties>()
    private val gitHubWebhookHandler = GitHubWebhookHandler(webhookEventService, gitHubEnterpriseProperties)

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `should throw WebhookSecretNotSetException when webhook processing is disabled`() {
        setPrivateField(gitHubWebhookHandler, "isWebhookProcessingEnabled", false)

        assertThrows<WebhookSecretNotSetException> {
            gitHubWebhookHandler.handleWebhookEvent("PUSH", "host", null, "{}")
        }
    }

    @Test
    fun `should not process event if unknown host`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"

        gitHubWebhookHandler.handleWebhookEvent("PUSH", "unknown.host", null, "{}")

        verify(exactly = 0) { webhookEventService.consumeWebhookEvent(any(), any()) }
    }

    @Test
    fun `should throw WebhookSecretNotSetException when signature is present but secret is not set`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""

        assertThrows<WebhookSecretNotSetException> {
            gitHubWebhookHandler.handleWebhookEvent("PUSH", "known.host", "sha256=signature", "{}")
        }
    }

    @Test
    fun `should throw InvalidEventSignatureException for invalid signature`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns "secret"

        assertThrows<InvalidEventSignatureException> {
            gitHubWebhookHandler.handleWebhookEvent("PUSH", "known.host", "sha256=signature", "{}")
        }
    }

    @Test
    fun `should not process unsupported event type`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""

        gitHubWebhookHandler.handleWebhookEvent("UNSUPPORTED_EVENT", "known.host", null, "{}")

        verify(exactly = 0) { webhookEventService.consumeWebhookEvent(any(), any()) }
    }

    @Test
    fun `should process supported event type successfully`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""
        every { webhookEventService.consumeWebhookEvent(any(), any()) } returns Unit

        gitHubWebhookHandler.handleWebhookEvent("PUSH", "host", null, "{}")

        verify { webhookEventService.consumeWebhookEvent("PUSH", "{}") }
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any) {
        val field: Field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
