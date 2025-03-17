package net.leanix.githubagent.dto

data class SbomConfigDTO(
    val rule: String,
    val source: String = GitHubSbomSource.GITHUB_ARTIFACT.name,
    val namingConventions: String = "serviceName-sbom",
    val branches: String = "default",
)


enum class SbomFileNameRule {
    STARTS,
    ENDS,
    CONTAINS
}