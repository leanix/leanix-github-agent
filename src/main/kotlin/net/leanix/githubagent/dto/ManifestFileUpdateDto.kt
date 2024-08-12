package net.leanix.githubagent.dto

data class ManifestFileUpdateDto(
    val repositoryFullName: String,
    val action: ManifestFileAction,
    val manifestFile: ManifestFileDto
)

enum class ManifestFileAction {
    ADDED,
    MODIFIED,
    REMOVED
}
