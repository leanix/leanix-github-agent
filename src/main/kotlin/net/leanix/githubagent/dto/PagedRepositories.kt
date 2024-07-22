package net.leanix.githubagent.dto

data class PagedRepositories(
    val repositories: List<RepositoryDto>,
    val hasNextPage: Boolean,
    val cursor: String? = null
)
