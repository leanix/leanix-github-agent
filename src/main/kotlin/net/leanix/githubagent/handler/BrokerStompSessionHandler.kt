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
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CountDownLatch

@Component
class BrokerStompSessionHandler : StompSessionHandlerAdapter() {

    companion object{
        private const val RETRY_INTERVAL = 10000L
        private const val MAX_RETRY_TIME = 600000L
    }

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
            retryConnection()
        } else {
            if (latch.count != 0L) latch.countDown()
        }
    }

    private fun retryConnection() {
        val timer = Timer()
        val startTime = System.currentTimeMillis()
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (System.currentTimeMillis() - startTime > MAX_RETRY_TIME) {
                    logger.error("Failed to reconnect after ${MAX_RETRY_TIME / 60000} minutes. Stopping retries.")
                    timer.cancel()
                    return
                }
                logger.info("Attempting to reconnect...")
                webSocketService.initSession()
                if (isConnected) {
                    logger.info("Reconnected successfully.")
                    timer.cancel()
                }
            }
        }, 0, RETRY_INTERVAL) // Retry every 10 seconds
    }

    fun isConnected(): Boolean {
        awaitConnection()
        return isConnected
    }

    private fun awaitConnection() {
        latch.await()
    }
}
