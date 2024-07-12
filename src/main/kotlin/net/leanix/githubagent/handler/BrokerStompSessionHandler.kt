package net.leanix.githubagent.handler

import net.leanix.githubagent.services.WebSocketService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.stereotype.Component

@Component
class BrokerStompSessionHandler : StompSessionHandlerAdapter() {
    @Lazy
    @Autowired
    private lateinit var webSocketService: WebSocketService

    private val logger = LoggerFactory.getLogger(BrokerStompSessionHandler::class.java)

    private var isConnected = false

    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        logger.info("connected to the server: ${session.sessionId}")
        isConnected = true
        session.subscribe("/user/queue/repositories-string", this)
    }

    override fun handleException(
        session: StompSession,
        command: StompCommand?,
        headers: StompHeaders,
        payload: ByteArray,
        exception: Throwable,
    ) {
        logger.error("handleException", exception)
        logger.error(exception.message)
    }

    override fun handleTransportError(session: StompSession, exception: Throwable) {
        logger.error("handleTransportError", exception)
        if (isConnected) {
            logger.info("session closed")
            isConnected = false
            webSocketService.initSession()
        }
    }
}
