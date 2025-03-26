package net.leanix.githubagent.dto

data class ArtifactDTO(
    val repositoryFullName: String,
    val artifactFileName: String,
    val artifactFileContent: String,
)
