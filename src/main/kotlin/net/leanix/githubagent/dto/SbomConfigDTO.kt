package net.leanix.githubagent.dto

data class SbomConfigDTO(
    val fileNameRule: SbomFileNameRule,
    val source: GitHubSbomSource = GitHubSbomSource.GITHUB_ARTIFACT,
    val namingConventions: String = "serviceName-sbom",
    val branches: String = "default",
)

fun SbomConfigDTO.toSbomConfig(): SbomConfig {
    return SbomConfig(
        fileNameRule = this.fileNameRule,
        source = this.source,
        namingConventions = this.namingConventions,
        branches = this.branches,
    )
}
enum class SbomFileNameRule {
    STARTS,
    ENDS,
    CONTAINS
}
