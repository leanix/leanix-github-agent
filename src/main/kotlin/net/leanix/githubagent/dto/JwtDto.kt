package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class JwtDto(
    val scope: String,
    val expired: Boolean,
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expired_in")
    val expiredIn: Int,
)
