package net.leanix.githubagent.dto

data class ManifestFileUpdateDto(
    val repositoryFullName: String,
    val action: ManifestFileAction,
    val manifestContent: String?
)

enum class ManifestFileAction {
    ADDED,
    MODIFIED,
    REMOVED
}
