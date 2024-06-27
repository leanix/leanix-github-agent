package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubAppResponse(
    @JsonProperty("name") val name: String,
    @JsonProperty("permissions") val permissions: Map<String, String>,
    @JsonProperty("events") val events: List<String>
)
