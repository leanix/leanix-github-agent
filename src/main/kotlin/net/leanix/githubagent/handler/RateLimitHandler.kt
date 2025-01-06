package net.leanix.githubagent.handler

import net.leanix.githubagent.dto.RateLimitType
import net.leanix.githubagent.services.SyncLogService
import net.leanix.githubagent.shared.RateLimitMonitor
import org.springframework.stereotype.Component

@Component
class RateLimitHandler(
    private val syncLogService: SyncLogService,
) {

    fun <T> executeWithRateLimitHandler(rateLimitType: RateLimitType, block: () -> T): T {
        while (true) {
            val waitTimeSeconds = RateLimitMonitor.shouldThrottle(rateLimitType)
            if (waitTimeSeconds > 0) {
                syncLogService.sendInfoLog(
                    "Approaching rate limit for $rateLimitType calls. " +
                        "Waiting for $waitTimeSeconds seconds."
                )
                Thread.sleep(waitTimeSeconds * 1000)
            }
            return block()
        }
    }
}
