package net.leanix.githubagent.dto

data class ManifestFilesDTO(
    val repositoryId: String,
    val repositoryFullName: String,
    val manifestFiles: List<ManifestFileDTO>
)

data class ManifestFileDTO(
    val path: String,
    val content: String?,
)
