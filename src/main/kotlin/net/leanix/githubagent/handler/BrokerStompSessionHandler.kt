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
import java.util.concurrent.CountDownLatch

@Component
class BrokerStompSessionHandler : StompSessionHandlerAdapter() {
    @Lazy
    @Autowired
    private lateinit var webSocketService: WebSocketService

    private val logger = LoggerFactory.getLogger(BrokerStompSessionHandler::class.java)

    private var isConnected = false

    private val latch = CountDownLatch(1)

    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        logger.info("connected to the server: ${session.sessionId}")
        isConnected = true
        latch.countDown()
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
        logger.error("Connection error: ${exception.message}")
        if (isConnected) {
            logger.error("Session closed. This could be due to a network error or the server closing the connection.")
            isConnected = false
            logger.info("Reconnecting...")
            webSocketService.initSession()
        } else {
            if (latch.count != 0L) latch.countDown()
        }
    }

    fun isConnected(): Boolean {
        awaitConnection()
        return isConnected
    }

    private fun awaitConnection() {
        latch.await()
    }
}
