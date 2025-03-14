package net.leanix.githubagent.dto

data class SbomEventDTO(
    val repositoryName: String,
    val factSheetName: String,
    val sbomFileName: String,
    val sbomFileContent: String,
)
