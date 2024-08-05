package net.leanix.githubagent.shared

const val TOPIC_PREFIX = "/app/ghe/"

const val MANIFEST_FILE_NAME = "leanix.yaml"

val SUPPORTED_EVENT_TYPES = listOf(
    "REPOSITORY",
    "PUSH",
    "ORGANIZATION",
    "INSTALLATION",
)
