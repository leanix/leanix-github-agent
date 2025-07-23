package net.leanix.githubagent.handler

import net.leanix.githubagent.dto.ConnectionEstablishedEvent
import net.leanix.githubagent.services.WebSocketService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.stereotype.Component

@Component
class BrokerStompSessionHandler(
    private val artifactDownloadHandler: ArtifactDownloadHandler,
    private val repositoryGetHandler: RepositoryGetHandler,
    private val installationGetHandler: InstallationGetHandler,
    private val fullScanHandler: FullScanHandler,
    private val configurationSetHandler: ConfigurationSetHandler,
    private val eventPublisher: ApplicationEventPublisher
) : StompSessionHandlerAdapter() {
    @Lazy
    @Autowired
    private lateinit var webSocketService: WebSocketService

    private val logger = LoggerFactory.getLogger(BrokerStompSessionHandler::class.java)

    private var isConnected = false

    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        logger.info("connected to the server: ${session.sessionId}")
        isConnected = true
        session.subscribe("/user/queue/message/artifact", artifactDownloadHandler)
        session.subscribe("/user/queue/message/repository", repositoryGetHandler)
        session.subscribe("/user/queue/message/installation", installationGetHandler)
        session.subscribe("/user/queue/message/fullScan", fullScanHandler)
        session.subscribe("/user/queue/message/configurationSet", configurationSetHandler)
        eventPublisher.publishEvent(ConnectionEstablishedEvent())
    }

    override fun handleException(
        session: StompSession,
        command: StompCommand?,
        headers: StompHeaders,
        payload: ByteArray,
        exception: Throwable,
    ) {
        logger.error("handleException", exception)
    }

    override fun handleTransportError(session: StompSession, exception: Throwable) {
        logger.error("Connection error: ${exception.message}")
        if (isConnected && session.sessionId == webSocketService.stompSession?.sessionId) {
            isConnected = false
            logger.error("Session closed. This could be due to a network error or the server closing the connection.")
            logger.info("Reconnecting...")
            webSocketService.initSession()
        }
    }

    fun isConnected(): Boolean {
        return isConnected
    }
}
