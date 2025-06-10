package net.leanix.githubagent.handler

import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.GitHubStartService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type

@Component
class FullScanHandler(
    @Lazy @Autowired
    private val cachingService: CachingService,
    @Lazy @Autowired
    private val gitHubStartService: GitHubStartService,
    @Value("\${webhookEventService.waitingTime}") private val waitingTime: Long,
) : StompFrameHandler {

    private val logger = LoggerFactory.getLogger(FullScanHandler::class.java)

    override fun getPayloadType(headers: StompHeaders): Type {
        return String::class.java
    }

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        logger.info("Received full scan request")
        runCatching {
            while (cachingService.get("runId") != null) {
                logger.info("A full scan is already in progress, waiting for it to finish.")
                Thread.sleep(waitingTime)
            }
            GitHubStartService.requireScan = true
            gitHubStartService.verifyAndStartScan()
        }
    }
}
