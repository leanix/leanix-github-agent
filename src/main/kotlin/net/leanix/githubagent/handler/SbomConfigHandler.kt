package net.leanix.githubagent.handler

import net.leanix.githubagent.dto.SbomConfigDTO
import net.leanix.githubagent.services.CachingService
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type

@Component
class SbomConfigHandler(
    private val cachingService: CachingService,
) : StompFrameHandler {
    private val logger = LoggerFactory.getLogger(SbomConfigHandler::class.java)

    override fun getPayloadType(headers: StompHeaders): Type {
        return SbomConfigDTO::class.java
    }

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        logger.info("Received sbom config payload: {}", payload)
        payload?.let {
            val configDTO = payload as SbomConfigDTO
            println(configDTO)
            cachingService.remove("sbomConfig")
        }
    }
}