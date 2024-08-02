package net.leanix.githubagent.shared

const val TOPIC_PREFIX = "/app/ghe/"

val SUPPORTED_EVENT_TYPES = listOf(
    "repository",
    "push",
    "organization",
    "installation",
)
