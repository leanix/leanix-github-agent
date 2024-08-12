package net.leanix.githubagent.dto

import net.leanix.githubagent.graphql.data.enums.RepositoryVisibility

data class RepositoryDto(
    val id: String,
    val name: String,
    val organizationName: String,
    val description: String?,
    val url: String,
    val isArchived: Boolean,
    val visibility: RepositoryVisibility,
    val updatedAt: String,
    val languages: List<String>,
    val topics: List<String>,
    val manifestFile: ManifestFileDto?
)

data class ManifestFileDto(
    val path: String?,
    val content: String?
)
