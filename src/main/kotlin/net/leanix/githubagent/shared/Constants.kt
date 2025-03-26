package net.leanix.githubagent.shared

const val TOPIC_PREFIX = "/app/ghe/"
const val APP_NAME_TOPIC = "appName"
const val LOGS_TOPIC = "logs"
const val MANIFEST_FILE_NAME = "leanix.yaml"
const val WORKFLOW_RUN_EVENT = "WORKFLOW_RUN"
const val INSTALLATION_REPOSITORIES = "INSTALLATION_REPOSITORIES"

val SUPPORTED_EVENT_TYPES = listOf(
    "REPOSITORY",
    "PUSH",
    "ORGANIZATION",
    "INSTALLATION",
    WORKFLOW_RUN_EVENT,
    INSTALLATION_REPOSITORIES
)

val fileNameMatchRegex = Regex("/?$MANIFEST_FILE_NAME\$", RegexOption.IGNORE_CASE)

const val GITHUB_APP_LABEL = "GitHub App"
const val INSTALLATION_LABEL = "Installation"
