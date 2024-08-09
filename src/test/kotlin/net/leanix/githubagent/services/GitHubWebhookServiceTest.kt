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

class GitHubWebhookServiceTest {

    private val webhookService = mockk<WebhookService>()
    private val gitHubEnterpriseProperties = mockk<GitHubEnterpriseProperties>()
    private val gitHubWebhookService = GitHubWebhookService(webhookService, gitHubEnterpriseProperties)

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `should throw WebhookSecretNotSetException when webhook processing is disabled`() {
        setPrivateField(gitHubWebhookService, "isWebhookProcessingEnabled", false)

        assertThrows<WebhookSecretNotSetException> {
            gitHubWebhookService.processWebhookEvent("PUSH", "host", null, "{}")
        }
    }

    @Test
    fun `should not process event if unknown host`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"

        gitHubWebhookService.processWebhookEvent("PUSH", "unknown.host", null, "{}")

        verify(exactly = 0) { webhookService.consumeWebhookEvent(any(), any()) }
    }

    @Test
    fun `should throw WebhookSecretNotSetException when signature is present but secret is not set`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""

        assertThrows<WebhookSecretNotSetException> {
            gitHubWebhookService.processWebhookEvent("PUSH", "known.host", "sha256=signature", "{}")
        }
    }

    @Test
    fun `should throw InvalidEventSignatureException for invalid signature`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns "secret"

        assertThrows<InvalidEventSignatureException> {
            gitHubWebhookService.processWebhookEvent("PUSH", "known.host", "sha256=signature", "{}")
        }
    }

    @Test
    fun `should not process unsupported event type`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "known.host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""

        gitHubWebhookService.processWebhookEvent("UNSUPPORTED_EVENT", "known.host", null, "{}")

        verify(exactly = 0) { webhookService.consumeWebhookEvent(any(), any()) }
    }

    @Test
    fun `should process supported event type successfully`() {
        every { gitHubEnterpriseProperties.baseUrl } returns "host"
        every { gitHubEnterpriseProperties.webhookSecret } returns ""
        every { webhookService.consumeWebhookEvent(any(), any()) } returns Unit

        gitHubWebhookService.processWebhookEvent("PUSH", "host", null, "{}")

        verify { webhookService.consumeWebhookEvent("PUSH", "{}") }
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any) {
        val field: Field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
