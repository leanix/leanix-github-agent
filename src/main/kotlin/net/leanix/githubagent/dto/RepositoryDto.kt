package net.leanix.githubagent.dto

import net.leanix.githubbroker.connector.adapter.graphql.data.enums.RepositoryVisibility

data class RepositoryDto(
    val id: String,
    val name: String,
    val fullName: String,
    val description: String?,
    val url: String,
    val isArchived: Boolean,
    val visibility: RepositoryVisibility,
    val updatedAt: String,
    val languages: List<String>,
    val topics: List<String>,
    val manifestFileContent: String?,
)
