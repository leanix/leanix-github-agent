package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationTokenResponse(
    @JsonProperty("token") val token: String,
    @JsonProperty("expires_at") val expiresAt: String,
    @JsonProperty("permissions") val permissions: Map<String, String>,
    @JsonProperty("repository_selection") val repositorySelection: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Installation(
    @JsonProperty("id") val id: Long,
    @JsonProperty("account") val account: Account,
    @JsonProperty("permissions") val permissions: Map<String, String>,
    @JsonProperty("events") val events: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
    @JsonProperty("login") val login: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Organization(
    @JsonProperty("login") val login: String,
    @JsonProperty("id") val id: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Repository(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("full_name") val fullName: Boolean,
    @JsonProperty("owner") val owner: Organization,
    @JsonProperty("topics") val topics: List<String>
)
