package net.leanix.githubagent.scheduler

import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.services.WebSocketService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AgentConnectionScheduler(
    private val brokerStompSessionHandler: BrokerStompSessionHandler,
    private val webSocketService: WebSocketService
) {

    private val logger = LoggerFactory.getLogger(AgentConnectionScheduler::class.java)

    @Scheduled(fixedDelay = 600000, initialDelay = 600000)
    fun checkConnection() {
        logger.info("Checking agent connection")
        if (!brokerStompSessionHandler.isConnected()) {
            logger.info("Trying to connect to ws server")
            webSocketService.initSession()
        }
    }
}
