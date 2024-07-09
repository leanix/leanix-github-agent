package net.leanix.githubagent.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.retry.annotation.Retry
import net.leanix.githubagent.handler.BrokerStompSessionHandler
import net.leanix.githubagent.services.AuthService
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

@Configuration
class WebSocketClientConfig(
    private val brokerStompSessionHandler: BrokerStompSessionHandler,
    private val objectMapper: ObjectMapper,
    private val authService: AuthService,
    private val leanIXProperties: LeanIXProperties,
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties
) {

    @Retry(name = "ws-init-session")
    fun initSession(): StompSession {
        val headers = WebSocketHttpHeaders()
        val stompHeaders = StompHeaders()
        stompHeaders["Authorization"] = "Bearer ${authService.getBearerToken()}"
        stompHeaders["GitHub-Enterprise-URL"] = gitHubEnterpriseProperties.baseUrl
        stompHeaders["GiHub-Enterprise-App-Id"] = gitHubEnterpriseProperties.gitHubAppId
        return stompClient().connectAsync(
            leanIXProperties.wsBaseUrl,
            headers,
            stompHeaders,
            brokerStompSessionHandler,
        ).get()
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
