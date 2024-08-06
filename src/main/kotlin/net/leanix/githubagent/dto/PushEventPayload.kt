package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PushEventPayload(
    val ref: String,
    val repository: PushEventRepository,
    val installation: PushEventInstallation,
    @JsonProperty("head_commit")
    val headCommit: PushEventCommit
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PushEventRepository(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("full_name")
    val fullName: String,
    @JsonProperty("default_branch")
    val defaultBranch: String,
    val owner: PushEventOwner
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PushEventInstallation(
    val id: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PushEventCommit(
    val added: List<String>,
    val removed: List<String>,
    val modified: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PushEventOwner(
    val name: String
)
