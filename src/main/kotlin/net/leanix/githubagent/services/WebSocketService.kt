package net.leanix.githubagent.services

import net.leanix.githubagent.config.WebSocketClientConfig
import net.leanix.githubagent.shared.TOPIC_PREFIX
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.stereotype.Service

@Service
class WebSocketService(
    private val webSocketClientConfig: WebSocketClientConfig
) {

    private val logger = LoggerFactory.getLogger(WebSocketService::class.java)
    var stompSession: StompSession? = null

    fun initSession() {
        logger.info("init session")
        stompSession = webSocketClientConfig.initSession()
    }

    fun sendMessage(topic: String, data: Any) {
        stompSession!!.send("$TOPIC_PREFIX$topic", data)
    }
}
