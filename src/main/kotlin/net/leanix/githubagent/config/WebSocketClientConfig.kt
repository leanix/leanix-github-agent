package net.leanix.githubagent.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.retry.annotation.Retry
import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.services.LeanIXAuthService
import net.leanix.githubagent.shared.GitHubAgentProperties.GITHUB_AGENT_VERSION
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.util.concurrent.ScheduledFuture

@Configuration
class WebSocketClientConfig(
    private val brokerStompSessionHandler: BrokerStompSessionHandler,
    private val objectMapper: ObjectMapper,
    private val leanIXAuthService: LeanIXAuthService,
    private val leanIXProperties: LeanIXProperties,
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties,
) {

    private var heartbeatTask: ScheduledFuture<*>? = null
    private val logger = LoggerFactory.getLogger(WebSocketClientConfig::class.java)

    @Retry(name = "ws_init_session")
    fun initSession(): StompSession {
        val headers = WebSocketHttpHeaders()
        val stompHeaders = StompHeaders()
        stompHeaders["Authorization"] = "Bearer ${leanIXAuthService.getBearerToken()}"
        stompHeaders["GitHub-Enterprise-URL"] = gitHubEnterpriseProperties.baseUrl
        stompHeaders["GitHub-Agent-Version"] = GITHUB_AGENT_VERSION
        val session = stompClient().connectAsync(
            leanIXProperties.wsBaseUrl,
            headers,
            stompHeaders,
            brokerStompSessionHandler,
        ).get()

        sendHeartbeat(session)
        return session
    }

    fun sendHeartbeat(session: StompSession) {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.initialize()
        heartbeatTask = scheduler.scheduleAtFixedRate({
            kotlin.runCatching {
                if (session.isConnected) {
                    session.send("/app/ghe/heartbeat", "")
                    logger.debug("Heartbeat sent to /app/heartbeat")
                } else {
                    logger.warn("Session is not connected, stopping heartbeat")
                    stopHeartbeat()
                }
            }.onFailure {
                logger.error("Failed to send heartbeat: ${it.message}")
            }
        }, java.time.Duration.ofSeconds(30))
    }

    fun stopHeartbeat() {
        heartbeatTask?.cancel(true)
    }

    @Bean
    fun stompClient(): WebSocketStompClient {
        val jsonConverter = MappingJackson2MessageConverter()
        jsonConverter.objectMapper = objectMapper

        val simpleWebSocketClient = StandardWebSocketClient()
        val transports = listOf(WebSocketTransport(simpleWebSocketClient))
        val sockJsClient = SockJsClient(transports)
        val stompClient = WebSocketStompClient(sockJsClient)
        stompClient.messageConverter = jsonConverter
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.initialize()
        stompClient.taskScheduler = scheduler
        return stompClient
    }
}
