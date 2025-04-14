package net.leanix.githubagent.dto

data class RepositoryRequestDTO(
    val installation: Installation,
    val repositoryName: String,
    val repositoryFullName: String,
)
