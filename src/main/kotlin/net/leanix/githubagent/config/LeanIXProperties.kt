package net.leanix.githubagent.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "leanix")
data class LeanIXProperties(
    val wsBaseUrl: String,
    val auth: Auth
) {
    data class Auth(
        val accessTokenUri: String,
        val technicalUserToken: String
    )
}
