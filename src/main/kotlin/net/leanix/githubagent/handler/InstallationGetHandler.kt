package net.leanix.githubagent.handler

import net.leanix.githubagent.dto.InstallationRequestDTO
import net.leanix.githubagent.services.WebhookEventService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type

@Component
class InstallationGetHandler(
    @Lazy @Autowired
    private val webhookEventService: WebhookEventService
) : StompFrameHandler {

    private val logger = LoggerFactory.getLogger(InstallationGetHandler::class.java)

    override fun getPayloadType(headers: StompHeaders): Type {
        return InstallationRequestDTO::class.java
    }

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        payload?.let {
            val dto = payload as InstallationRequestDTO
            logger.info("Received installation get message from server for organisation: ${dto.account.login}")
            runCatching {
                webhookEventService.handleInstallationCreated(dto)
            }
        }
    }
}
