package net.leanix.githubagent.shared

const val TOPIC_PREFIX = "/app/ghe/"
const val APP_NAME_TOPIC = "appName"
const val LOGS_TOPIC = "logs"
const val MANIFEST_FILE_NAME = "leanix.yaml"

val SUPPORTED_EVENT_TYPES = listOf(
    "REPOSITORY",
    "PUSH",
    "ORGANIZATION",
    "INSTALLATION",
)

val fileNameMatchRegex = Regex("/?$MANIFEST_FILE_NAME\$")

const val GITHUB_APP_LABEL = "GitHub App"
const val INSTALLATION_LABEL = "Installation"
