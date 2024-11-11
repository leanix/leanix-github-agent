package net.leanix.githubagent.shared

fun generateFullPath(defaultBranch: String?, filePath: String?): String {
    if (filePath.isNullOrEmpty()) {
        return ""
    }
    val branch = defaultBranch?.takeIf { it.isNotEmpty() } ?: "-"

    return "tree/$branch/$filePath"
}
