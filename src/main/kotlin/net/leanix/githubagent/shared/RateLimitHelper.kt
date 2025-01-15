package net.leanix.githubagent.shared

import net.leanix.githubagent.dto.RateLimitType

fun determineRateLimitType(requestUrl: String): RateLimitType {
    return if (requestUrl.contains("/graphql")) {
        RateLimitType.GRAPHQL
    } else if (requestUrl.contains("/search")) {
        RateLimitType.SEARCH
    } else {
        RateLimitType.REST
    }
}
