package net.leanix.githubagent.shared

import net.leanix.githubagent.dto.RateLimitType
import org.slf4j.LoggerFactory

object RateLimitMonitor {

    private val logger = LoggerFactory.getLogger(RateLimitMonitor::class.java)

    @Volatile
    private var graphqlRateLimitRemaining: Int = Int.MAX_VALUE

    @Volatile
    private var graphqlRateLimitResetTime: Long = 0

    @Volatile
    private var restRateLimitRemaining: Int = Int.MAX_VALUE

    @Volatile
    private var restRateLimitResetTime: Long = 0

    @Volatile
    private var searchRateLimitRemaining: Int = Int.MAX_VALUE

    @Volatile
    private var searchRateLimitResetTime: Long = 0

    private val lock = Any()

    fun updateRateLimitInfo(rateLimitType: RateLimitType, remaining: Int, resetTimeEpochSeconds: Long) {
        synchronized(lock) {
            when (rateLimitType) {
                RateLimitType.GRAPHQL -> {
                    graphqlRateLimitRemaining = remaining
                    graphqlRateLimitResetTime = resetTimeEpochSeconds
                }
                RateLimitType.REST -> {
                    restRateLimitRemaining = remaining
                    restRateLimitResetTime = resetTimeEpochSeconds
                }
                RateLimitType.SEARCH -> {
                    searchRateLimitRemaining = remaining
                    searchRateLimitResetTime = resetTimeEpochSeconds
                }
            }
        }
    }

    fun shouldThrottle(rateLimitType: RateLimitType): Long {
        synchronized(lock) {
            val (rateLimitRemaining, rateLimitResetTime) = when (rateLimitType) {
                RateLimitType.GRAPHQL -> graphqlRateLimitRemaining to graphqlRateLimitResetTime
                RateLimitType.REST -> restRateLimitRemaining to restRateLimitResetTime
                RateLimitType.SEARCH -> searchRateLimitRemaining to searchRateLimitResetTime
            }

            if (rateLimitRemaining <= THRESHOLD) {
                val currentTimeSeconds = System.currentTimeMillis() / 1000
                val waitTimeSeconds = rateLimitResetTime - currentTimeSeconds + 5

                val adjustedWaitTime = if (waitTimeSeconds > 0) waitTimeSeconds else 0
                logger.warn(
                    "Rate limit remaining ($rateLimitRemaining) for $rateLimitType calls, is at or below " +
                        "threshold ($THRESHOLD). Throttling for $adjustedWaitTime seconds."
                )
                return adjustedWaitTime
            } else {
                logger.debug(
                    "Rate limit remaining ($rateLimitRemaining) for $rateLimitType calls, is above threshold" +
                        " ($THRESHOLD). No need to throttle."
                )
            }
            return 0
        }
    }

    private const val THRESHOLD = 2
}
