package net.leanix.githubagent.dto

data class RepositoryDto(
    val id: String,
    val name: String,
    val description: String?,
    val url: String,
    val organization: RepositoryOrganizationDto,
    val languages: List<String>,
    val topics: List<String>,
    val manifest: String?,
)

class RepositoryOrganizationDto(
    val id: String,
    val name: String
)
