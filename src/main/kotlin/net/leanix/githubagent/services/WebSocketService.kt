package net.leanix.githubagent.services

import io.github.resilience4j.retry.annotation.Retry
import net.leanix.githubagent.config.WebSocketClientConfig
import net.leanix.githubagent.exceptions.UnableToSendMessageException
import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.shared.TOPIC_PREFIX
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.stereotype.Service

@Service
class WebSocketService(
    private val webSocketClientConfig: WebSocketClientConfig,
    private val brokerStompSessionHandler: BrokerStompSessionHandler,
    private val cachingService: CachingService,
) {

    private val logger = LoggerFactory.getLogger(WebSocketService::class.java)
    var stompSession: StompSession? = null

    fun initSession() {
        logger.info("Initializing websocket session")
        kotlin.runCatching {
            stompSession = webSocketClientConfig.initSession()

            logger.info("Websocket session initialized with success")
        }.onFailure {
            logger.error("Failed to initialize WebSocket session")
        }
    }

    @Retry(name = "sendMessageRetry")
    fun sendMessage(topic: String, data: Any) {
        if (!brokerStompSessionHandler.isConnected() && cachingService.get("runId") == null) {
            return
        }
        runCatching {
            stompSession!!.send("$TOPIC_PREFIX$topic", data)
        }.onFailure {
            throw UnableToSendMessageException()
        }
    }
}
