package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubSearchResponse(
    @JsonProperty("total_count")
    val totalCount: Int,
    val items: List<ItemResponse>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ItemResponse(
    val name: String,
    val path: String,
    val repository: RepositoryItemResponse,
    val url: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RepositoryItemResponse(
    val name: String,
    @JsonProperty("full_name")
    val fullName: String,
)
