package net.leanix.githubagent.shared

const val TOPIC_PREFIX = "/app/ghe/"
const val APP_NAME_TOPIC = "appName"

enum class ManifestFileName(val fileName: String) {
    YAML("leanix.yaml"),
    YML("leanix.yml")
}

val SUPPORTED_EVENT_TYPES = listOf(
    "REPOSITORY",
    "PUSH",
    "ORGANIZATION",
    "INSTALLATION",
)
