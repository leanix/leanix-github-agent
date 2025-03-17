package net.leanix.githubagent.dto

data class SbomConfig(
    val source: GitHubSbomSource = GitHubSbomSource.GITHUB_ARTIFACT,
    val namingConventions: String = "serviceName-sbom",
    val branches: String = "default",
) {
    fun isFileNameValid(fileName: String, branchName: String): Boolean {
        return fileName == generateExpectedFileName(fileName) && branchName == branches
    }
    fun extractFactSheetName(fileName: String): String {
        val parts = fileName.split("-")
        return parts.drop(1).dropLast(1).joinToString("-")
    }

    private fun generateExpectedFileName(fileName: String): String {
        val parts = fileName.split("-")
        if (parts.size < 3) return ""

        val serviceName = parts.drop(1).dropLast(1).joinToString("-")

        val conventionParts = namingConventions.split("-")
        if (conventionParts.size < 3) return ""

        val orgPrefix = conventionParts.first()
        val sbomSuffix = conventionParts.last()

        return "$orgPrefix-$serviceName-$sbomSuffix"
    }
}

enum class GitHubSbomSource {
    GITHUB_ARTIFACT,
}
