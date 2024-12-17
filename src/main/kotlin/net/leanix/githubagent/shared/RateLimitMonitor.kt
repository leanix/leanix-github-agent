package net.leanix.githubagent.shared

import org.slf4j.LoggerFactory

object RateLimitMonitor {

    private val logger = LoggerFactory.getLogger(RateLimitMonitor::class.java)

    @Volatile
    private var rateLimitRemaining: Int = Int.MAX_VALUE

    @Volatile
    private var rateLimitResetTime: Long = 0

    private val lock = Any()

    fun updateRateLimitInfo(remaining: Int, resetTimeEpochSeconds: Long) {
        synchronized(lock) {
            rateLimitRemaining = remaining
            rateLimitResetTime = resetTimeEpochSeconds
        }
    }

    fun shouldThrottle(): Long {
        synchronized(lock) {
            if (rateLimitRemaining <= THRESHOLD) {
                val currentTimeSeconds = System.currentTimeMillis() / 1000
                val waitTimeSeconds = rateLimitResetTime - currentTimeSeconds + 5

                val adjustedWaitTime = if (waitTimeSeconds > 0) waitTimeSeconds else 0
                logger.warn(
                    "Rate limit remaining ($rateLimitRemaining) is at or below threshold ($THRESHOLD)." +
                        " Throttling for $adjustedWaitTime seconds."
                )
                return adjustedWaitTime
            } else {
                logger.debug(
                    "Rate limit remaining ($rateLimitRemaining) is above threshold ($THRESHOLD). No need to throttle."
                )
            }
            return 0
        }
    }

    private const val THRESHOLD = 2
}
