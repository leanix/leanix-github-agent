package net.leanix.githubagent.listener

import net.leanix.githubagent.services.WebSocketService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class ApplicationReadyListener(
    private val webSocketService: WebSocketService,
) : ApplicationListener<ApplicationReadyEvent> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        logger.info("Agent started")
        webSocketService.initSession()
    }
}
