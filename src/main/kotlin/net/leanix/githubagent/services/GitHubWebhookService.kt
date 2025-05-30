package net.leanix.githubagent.services

import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.exceptions.InvalidEventSignatureException
import net.leanix.githubagent.exceptions.WebhookSecretNotSetException
import net.leanix.githubagent.shared.SUPPORTED_EVENT_TYPES
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

    private val logger = LoggerFactory.getLogger(GitHubWebhookService::class.java)

    @Async
    fun handleWebhookEvent(eventType: String, host: String, signature256: String?, payload: String) {
        val runId = cachingService.get("runId")
        if (runId != null && eventType.uppercase() != "INSTALLATION") {
            logger.debug("Received a webhook event while a full sync is in progress, ignoring the event.")
            return
        }
        if (SUPPORTED_EVENT_TYPES.contains(eventType.uppercase())) {
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
                val hashedSecret = hmacSHA256(gitHubEnterpriseProperties.webhookSecret, payload)
                val isEqual = timingSafeEqual(signature256.removePrefix("sha256="), hashedSecret)
                if (!isEqual) throw InvalidEventSignatureException()
            } else {
                logger.warn("Webhook secret is not set, Skipping signature verification")
            }
            logger.info("Received a webhook event of type: $eventType")
            webhookEventService.consumeWebhookEvent(eventType, payload)
        } else {
            logger.warn("Received an unsupported event of type: $eventType")
        }
    }
}
