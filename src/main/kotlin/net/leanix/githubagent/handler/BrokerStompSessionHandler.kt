package net.leanix.githubagent.handler

import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.stereotype.Component

@Component
class BrokerStompSessionHandler : StompSessionHandlerAdapter() {

    private val logger = LoggerFactory.getLogger(BrokerStompSessionHandler::class.java)

    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        logger.info("connected to the server: ${session.sessionId}")
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
        logger.error(exception.message)
    }
}
