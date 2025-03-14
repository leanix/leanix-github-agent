package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowRunEventDto(
    val action: String,
    @JsonProperty("workflow_run") val workflowRun: WorkflowRun,
    val repository: WorkflowRepository,
    val installation: InstallationDTO
) {
    fun isCompleted() = (
        action == "completed" && workflowRun.conclusion == "success"
        )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowRun(
    val id: Long,
    @JsonProperty("head_branch") val headBranch: String?,
    @JsonProperty("url") val runUrl: String?,
    @JsonProperty("run_attempt") val runAttempt: Int?,
    @JsonProperty("node_id") val nodeId: String,
    @JsonProperty("head_sha") val headSha: String?,
    val status: String,
    val conclusion: String?,
    @JsonProperty("created_at") val createdAt: String?,
    @JsonProperty("run_started_at") val startedAt: String?,
    val name: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowRepository(
    val id: Long,
    @JsonProperty("node_id") val nodeId: String,
    val name: String,
    @JsonProperty("full_name") val fullName: String,
    val private: Boolean,
    @JsonProperty("html_url") val htmlUrl: String?,
    @JsonProperty("default_branch") val defaultBranch: String?,
    val owner: RepositoryOwner
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RepositoryOwner(
    val login: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationDTO(
    val id: Int
)
