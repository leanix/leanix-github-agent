package net.leanix.githubagent.dto

data class ArtifactDownloadDTO(
    val repositoryName: String,
    val repositoryOwner: String,
    val runId: Long,
    val installationId: Int,
    val artifactName: String? = null,
)
