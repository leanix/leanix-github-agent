package net.leanix.githubagent.handler

import net.leanix.githubagent.services.GitHubWebhookService
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type

@Component
class ConfigurationSetHandler : StompFrameHandler {

    private val logger = LoggerFactory.getLogger(ConfigurationSetHandler::class.java)

    override fun getPayloadType(headers: StompHeaders): Type {
        return AgentConfigurationRequest::class.java
    }

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        payload?.let {
            val config = payload as AgentConfigurationRequest
            logger.info("Received configuration set message from server")
            runCatching {
                config.supportedWebhookEvents?.let { supportedEvents ->
                    GitHubWebhookService.updateSupportedWebhookEvents(supportedEvents)
                    logger.info(
                        "Supported webhook events updated: ${GitHubWebhookService.getSupportedWebhookEvents()}"
                    )
                }
            }
        }
    }
}

data class SupportedWebhookEvent(val eventType: String, val actions: List<String>)
data class AgentConfigurationRequest(val supportedWebhookEvents: List<SupportedWebhookEvent>?)
