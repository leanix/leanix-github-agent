package net.leanix.githubagent.scheduler

import net.leanix.githubagent.dto.FullScanStatus
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.WebSocketService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FullScanStatusScheduler(
    private val webSocketService: WebSocketService,
    private val cachingService: CachingService,
) {

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    fun sendFullScanStatus() {
        val runId = cachingService.get("runId")
        val status = if (runId != null) FullScanStatus.IN_PROGRESS else FullScanStatus.FINISHED
        webSocketService.sendMessage("/fullScanStatus", status.name)
    }
}
