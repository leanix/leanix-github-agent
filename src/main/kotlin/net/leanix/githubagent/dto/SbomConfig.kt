package net.leanix.githubagent.dto

data class SbomConfig(
    val source: GitHubSbomSource = GitHubSbomSource.GITHUB_ARTIFACT,
    val namingConventions: String = "leanix-serviceName-sbom",
    val defaultBranch: String = "main",
) {
    fun isFileNameValid(fileName: String, branchName: String): Boolean {
        return fileName == generateExpectedFileName(fileName) && branchName == defaultBranch
    }

    private fun generateExpectedFileName(fileName: String): String {
        val split = fileName.split("-")
        if (split.size != 3) return ""

        val namingConventionSplit = namingConventions.split("-")
        return "${namingConventionSplit[0]}-${split[1]}-${namingConventionSplit[2]}"
    }
}

enum class GitHubSbomSource {
    GITHUB_ARTIFACT,
}
