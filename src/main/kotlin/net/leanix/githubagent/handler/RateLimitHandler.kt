package net.leanix.githubagent.handler

import net.leanix.githubagent.services.SyncLogService
import net.leanix.githubagent.shared.RateLimitMonitor
import org.springframework.stereotype.Component

@Component
class RateLimitHandler(
    private val syncLogService: SyncLogService,
) {

    fun <T> executeWithRateLimitHandler(block: () -> T): T {
        while (true) {
            val waitTimeSeconds = RateLimitMonitor.shouldThrottle()
            if (waitTimeSeconds > 0) {
                syncLogService.sendInfoLog("Approaching rate limit. Waiting for $waitTimeSeconds seconds.")
                Thread.sleep(waitTimeSeconds * 1000)
            }
            return block()
        }
    }
}
