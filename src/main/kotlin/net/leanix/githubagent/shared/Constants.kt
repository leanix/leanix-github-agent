package net.leanix.githubagent.shared

const val TOPIC_PREFIX = "/app/ghe/"
const val AGENT_METADATA_TOPIC = "agent"
const val LOGS_TOPIC = "logs"

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
