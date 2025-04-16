package net.leanix.githubagent.dto

data class RepositoryRequestDTO(
    val installation: RepositoryRequestInstallationDTO,
    val repositoryName: String,
    val repositoryFullName: String,
)

data class RepositoryRequestInstallationDTO(
    val id: Long,
    val account: Account
)
