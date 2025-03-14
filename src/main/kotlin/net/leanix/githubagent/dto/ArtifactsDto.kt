package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtifactsListResponse(
    @JsonProperty("total_count")
    val totalCount: Int,
    val artifacts: List<Artifact>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Artifact(
    val id: Long,
    val name: String,
    val url: String,
    @JsonProperty("archive_download_url")
    val archiveDownloadUrl: String,
)
