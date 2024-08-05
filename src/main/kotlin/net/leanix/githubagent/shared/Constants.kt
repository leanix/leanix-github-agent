package net.leanix.githubagent.shared

const val TOPIC_PREFIX = "/app/ghe/"

val SUPPORTED_EVENT_TYPES = listOf(
    "REPOSITORY",
    "PUSH",
    "ORGANIZATION",
    "INSTALLATION",
)
