package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.GenericWebhookEvent
import net.leanix.githubagent.exceptions.InvalidEventSignatureException
import net.leanix.githubagent.exceptions.WebhookSecretNotSetException
import net.leanix.githubagent.handler.SupportedWebhookEvent
import net.leanix.githubagent.shared.hmacSHA256
import net.leanix.githubagent.shared.timingSafeEqual
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class GitHubWebhookService(
    private val webhookEventService: WebhookEventService,
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties,
    private val cachingService: CachingService,
) {

    companion object {
        private var supportedWebhookEvents = listOf<SupportedWebhookEvent>()

        fun updateSupportedWebhookEvents(events: List<SupportedWebhookEvent>) {
            supportedWebhookEvents = events
        }

        fun getSupportedWebhookEvents(): List<SupportedWebhookEvent> = supportedWebhookEvents
    }

    private val objectMapper = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(GitHubWebhookService::class.java)

    @Async
    fun handleWebhookEvent(eventType: String, host: String, signature256: String?, payload: String) {
        val runId = cachingService.get("runId")
        if (runId != null && eventType.uppercase() != "INSTALLATION") {
            logger.debug("Received a webhook event while a full sync is in progress, ignoring the event.")
            return
        }

        val eventAction = extractEventAction(payload)
        val supportedWebhookEvent = findSupportedWebhookEvent(eventType)
        if (isEventSupported(supportedWebhookEvent, eventAction)) {
            if (!gitHubEnterpriseProperties.baseUrl.contains(host)) {
                logger.error("Received a webhook event from an unknown host: $host")
                return
            }
            if (gitHubEnterpriseProperties.webhookSecret.isBlank() && signature256 != null) {
                logger.error(
                    "Event signature is present but webhook secret is not set, " +
                        "please restart the agent with a valid secret, " +
                        "or remove the secret from the GitHub App settings."
                )
                throw WebhookSecretNotSetException()
            }
            if (gitHubEnterpriseProperties.webhookSecret.isNotBlank() && signature256 != null) {
                if (!signature256.startsWith("sha256=")) {
                    logger.error("Invalid signature format, expected 'sha256=' prefix")
                    throw InvalidEventSignatureException()
                }
                val hashedSecret = hmacSHA256(gitHubEnterpriseProperties.webhookSecret, payload)
                val isEqual = timingSafeEqual(signature256.removePrefix("sha256="), hashedSecret)
                if (!isEqual) throw InvalidEventSignatureException()
            } else {
                logger.warn("Webhook secret is not set, Skipping signature verification")
            }
            logger.info("Received a webhook event of type: $eventType")
            webhookEventService.consumeWebhookEvent(eventType, payload)
        } else {
            logger.warn("Received an unsupported event of type: $eventType with action: $eventAction")
        }
    }

    private fun findSupportedWebhookEvent(eventType: String): SupportedWebhookEvent? {
        return getSupportedWebhookEvents().find { it.eventType.equals(eventType, ignoreCase = true) }
    }

    private fun extractEventAction(payload: String): String? {
        return objectMapper.readValue(payload, GenericWebhookEvent::class.java).action
    }

    private fun isEventSupported(event: SupportedWebhookEvent?, action: String?): Boolean {
        return event != null && (event.actions.isEmpty() || (action != null && action in event.actions))
    }
}
